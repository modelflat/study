package com.github.modelflat.nit3;

import com.github.modelflat.nit3.jpeg.Block;
import com.github.modelflat.nit3.jpeg.huffman.Huffman;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.*;
import java.util.Locale;

public class Main {

    private static GnuplotInterface gnuplot = new GnuplotInterface()
            .addWorkspaceCommand("unset key");

    private static int[] qSet = {0, 2, 4, 6};

    private static final String BASE_OUTPUT_DIR = "./output/";
    private static final String GNUPLOT_OUTPUT_DIR = BASE_OUTPUT_DIR + "3D/";
    private static final String MATRIX_OUTPUT_DIR = BASE_OUTPUT_DIR + "gnu/";
    private static final String MPEG_TEST_IMAGES_OUTPUT_DIR = BASE_OUTPUT_DIR + "mpeg/";
    private static final String JPEG_TEST_IMAGES_OUTPUT_DIR = BASE_OUTPUT_DIR + "jpeg/";

    public static void main(String[] args) {

        mkdirs();

        Settings settings = new Settings();
        CmdLineParser parser = new CmdLineParser(settings);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            parser.printUsage(System.out);
            return;
        }

        if (settings.getFilename() != null) {
            try {
                System.setOut(new PrintStream(settings.getFilename()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }

        if (settings.isInteractive()) {
            gnuplot.setInteractive(true);
        } else {
            gnuplot.addWorkspaceCommand("set term png");
        }

        Block block1 = readData(settings.getInputFile1(), settings.getQ());
        Block block2 = readData(settings.getInputFile2(), settings.getQ());

        block1.saveImage(JPEG_TEST_IMAGES_OUTPUT_DIR + "source1");
        block2.saveImage(MPEG_TEST_IMAGES_OUTPUT_DIR + "source2");

        System.out.println("====== JPEG ======");
        try (PrintStream s = open("source.3d")) {
            block1.print("исходная матрица")
                    .print(s);
            splot("source.3d");
        }

        if (settings.hasAutoJPEGQ()) {
            for (int q : qSet) {
                jpegTest(block1.copy().setQ(q));
            }
        } else {
            jpegTest(block1.copy());
        }
        System.out.println("====== MPEG ======");
        mpegTest(block1.copy(), block2.copy());
    }

    private static void jpegTest(Block block) {
        Block initial = block.copy();

        try (PrintStream fdct = open("fdct" + block.getQ());
             PrintStream encoded = open("encoded" + block.getQ())) {

            block   .normalize()
                    .print("Нормализованная матрица")
                    .fdct()
                    .print("После косинусного преобразования")
                    .print(fdct)
                    .printQuantizationMatrix()
                    .quantize()
                    .print("После квантования с коэф. q = " + block.getQ())
                    .print(encoded);

            System.out.printf("Сжатие (RLE): %.3f%%\n",
                    compressionPercentage(block.computeRLESize(), 64));
        }

        splot("fdct" + block.getQ());
        splot("encoded" + block.getQ());

        try (PrintStream spec = open("spec" + block.getQ());
             PrintStream luminance = open("luminance" + block.getQ())) {
            block.dequantize()
                    .print("Матрица спектральных амплитуд")
                    .print(spec)
                    .idct()
                    .print("После обратного косинусного преобразования")
                    .print(luminance)
                    .restore()
                    .print("Восстановленная");
        }

        block.saveImage(JPEG_TEST_IMAGES_OUTPUT_DIR + "restored" + block.getQ());

        splot("spec" + block.getQ());
        splot("luminance" + block.getQ());

        block.diff(initial)
                .print("Разница между начальной и конечной матрицей");

        System.out.println("Размеры символов в битах после построения дерева Хаффмана");
        Huffman h = block.encodeHuffman();
        h.printSizes();
        System.out.printf("Сжатие (Huffman): %.3f%%\n",
                compressionPercentage(block.computeHuffmanSize(h), 64));
    }

    private static void mpegTest(Block block1, Block block2) {

        block2.saveImage(MPEG_TEST_IMAGES_OUTPUT_DIR + "mpeg_P");

        Block diff = block1.diff(block2);
        diff.print("Матрица яркости P-кадра")
                .saveImage(MPEG_TEST_IMAGES_OUTPUT_DIR + "mpeg_diff");

        for (int q : qSet) {
            Block copyDiff = diff.copy().setQ(q);
            Huffman h = copyDiff.compress().print("Сжатый кадр").encodeHuffman();
            System.out.printf("Сжатие P-кадра при q = %d: %.3f%% (RLE), %.3f%% (Huffman)\n", q,
                    compressionPercentage(copyDiff.computeRLESize(), 64),
                    compressionPercentage(copyDiff.computeHuffmanSize(h), 64));
            Block restored = copyDiff.decompress().sum(block2);
            try (PrintStream e = open("mpeg_decoded" + q)) {
                restored.print(e);
                splot("mpeg_decoded" + q);
            }
            restored.saveImage(MPEG_TEST_IMAGES_OUTPUT_DIR + "restored" + q);
        }
    }

    private static float compressionPercentage(float compressedSize, float sourceSize) {
        return (1 - compressedSize / sourceSize) * 100;
    }

    private static Block readData(String fileName, int q) {
        float[][] m = new float[8][8];
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for (int i = 0; i < 8; i++) {
                int j = 0;
                for (String value : br.readLine().split("\\s+")) {
                    if (!value.isEmpty()) {
                        m[i][j++] = Integer.parseInt(value);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new Block(m, q);
    }

    private static void splot(String source) {
        try {
            gnuplot.addCommand("set style data pm3d")
                    .addCommand(String.format("set output '%s.png'", GNUPLOT_OUTPUT_DIR + source))
                    .addCommand("set pm3d")
                    .addCommand(String.format("splot '%s' matrix", MATRIX_OUTPUT_DIR + source))
                    .invoke();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static PrintStream open(String filename) {
        try {
            return new PrintStream(MATRIX_OUTPUT_DIR + filename);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void mkdirs() {
        for (String dir : new String[] {
                BASE_OUTPUT_DIR,
                GNUPLOT_OUTPUT_DIR,
                MATRIX_OUTPUT_DIR,
                MPEG_TEST_IMAGES_OUTPUT_DIR,
                JPEG_TEST_IMAGES_OUTPUT_DIR
        }) {
            new File(dir).mkdir();
        }
    }

}
