package com.github.modelflat.nit3.jpeg;

import com.github.modelflat.nit3.jpeg.huffman.Huffman;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

public class Block {

    private float[][] core = new float[8][8];
    private float[][] quantizationMatrix;
    private int q;

    private static final double cInner = Math.PI / 16;

    public Block(float[][] raw) {
        this(raw, 0);
    }

    public Block(float[][] raw, int q) {
        if (raw.length != 8 || raw[0].length != 8) {
            throw new RuntimeException("Invalid block size!");
        }
        core = raw;
        setQ(q);
    }

    public Block copy() {
        float[][] newArray = new float[8][];
        for (int i = 0; i < 8; i++) {
            newArray[i] = Arrays.copyOf(core[i], core[i].length);
        }
        return new Block(newArray, q);
    }

    public Block diff(Block other) {
        float[][] result = new float[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                result[i][j] = core[i][j] - other.core[i][j];
            }
        }
        return new Block(result);
    }

    public Block sum(Block other) {
        float[][] result = new float[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                result[i][j] = core[i][j] + other.core[i][j];
            }
        }
        return new Block(result);
    }

    public Block normalize() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                core[i][j] -= 128;
            }
        }
        return this;
    }

    public Block restore() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                core[i][j] += 128;
                int value = round(core[i][j]);
                if (value < 0) {
                    core[i][j] = 0;
                }
                if (value > 255) {
                    core[i][j] = 255;
                }
            }
        }
        return this;
    }

    /*
    Forward DCT
     */
    public Block fdct() {
        float[][] result = new float[8][8];
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                float sum = 0;
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        sum +=  core[x][y] *
                                Math.cos((2 * x + 1) * u * cInner) *
                                Math.cos((2 * y + 1) * v * cInner);
                    }
                }
                result[u][v] = (float) (sum * c(u, v) / 4);
            }
        }
        core = result;
        return this;
    }

    public Block quantize() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                core[i][j] = round(core[i][j] / quantizationMatrix[i][j]);
            }
        }
        return this;
    }

    public int computeRLESize() {
        Iterable<Float> i = new SerpentIterator(core);
        @SuppressWarnings("unchecked") int prev = ((Iterator<Float>) i).next().intValue();
        int total = 2;
        for (Float v : i) {
            int current = v.intValue();
            if (prev != current) {
                total += 2;
                prev = current;
            }
        }
        return total;
    }

    public Block dequantize() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                core[i][j] *= quantizationMatrix[i][j];
            }
        }
        return this;
    }

    public Block compress() {
        this.normalize();
        this.fdct();
        this.quantize();
        return this;
    }

    public Block decompress() {
        this.dequantize();
        this.idct();
        this.restore();
        return this;
    }


    /*
    Inverse DCT
     */
    public Block idct() {
        float[][] result = new float[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                float sum = 0;
                for (int u = 0; u < 8; u++) {
                    for (int v = 0; v < 8; v++) {
                        sum +=  c(u, v) *
                                core[u][v]*
                                Math.cos((2*x + 1) * u * cInner) *
                                Math.cos((2*y + 1) * v * cInner);
                    }
                }
                result[x][y] = (int) (sum / 4);
            }
        }
        core = result;
        return this;
    }

    public Huffman encodeHuffman() {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int symbol = round(core[i][j]);
                freq.compute(symbol, (integer, integer2) -> (integer2 == null) ? 1 : integer2 + 1);
            }
        }
        return new Huffman(freq);
    }

    public int computeHuffmanSize(Huffman encoder) {
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                sum += encoder.getSizeFor(round(core[i][j]));
            }
        }
        if (sum % 8 != 0) {
            sum += 8 - sum % 8;
        }
        return sum / 8;
    }

    public Block saveImage(String name) {
        BufferedImage result = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int value = 0xFF & (int) core[y][x];
                result.setRGB(x, y, (value << 16) | (value << 8) | value);
            }
        }
        try {
            ImageIO.write(result, "bmp", new File(name + ".bmp"));
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
        return this;
    }

    public Block print(PrintStream stream) {
        return print(null, stream);
    }

    public Block print(String header) {
        return print(header, System.out);
    }

    public Block print(String header, PrintStream stream) {
        printMatrix(header, stream, core);
        return this;
    }

    public Block printQuantizationMatrix() {
        return printQuantizationMatrix("Матрица квантования", System.out);
    }

    public Block printQuantizationMatrix(String header, PrintStream stream) {
        printMatrix(header, stream, quantizationMatrix);
        return this;
    }

    private void printMatrix(String header, PrintStream stream, float[][] matrix) {
        StringBuilder sb = new StringBuilder();
        if (header != null) {
            sb.append("=== ").append(header).append(":\n");
        }
        for (float[] row : matrix) {
            for (int i = 0; i < row.length; i++) {
                sb.append(Math.rint(row[i]) == row[i] ?
                        String.format("%4d", (int)(row[i])) : String.format("%6.1f", row[i]))
                        .append(i == row.length - 1 ? "" : "\t");
            }
            sb.append("\n");
        }
        stream.print(sb.toString());
    }

    private float[][] computeQuantizationMatrix(int q) {
        float[][] quantizationMatrix = new float[8][8];

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                quantizationMatrix[i][j] = 1 + (i + j + 1) * q;
            }
        }

        return quantizationMatrix;
    }

    private int round(float x) {
        if (x < 0) {
            return -Math.round(-x);
        }
        return Math.round(x);
    }

    private static double c(int u, int v) {
        if (u == 0 && v == 0) {
            return .5;
        }
        if (u == 0 || v == 0) {
            return 1d / Math.sqrt(2);
        }
        return 1;
    }

    public int getQ() {
        return q;
    }

    public Block setQ(int q) {
        this.q = q;
        quantizationMatrix = computeQuantizationMatrix(q);
        return this;
    }
}
