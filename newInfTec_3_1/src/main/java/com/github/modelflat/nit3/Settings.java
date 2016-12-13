package com.github.modelflat.nit3;

import org.kohsuke.args4j.Option;

class Settings {

    @Option(name = "--interactive", aliases = {"-i"}, usage = "enables interactive mode")
    private boolean interactive;
    @Option(name = "-f1", required = true)
    private String inputFileName;
    @Option(name = "-f2", required = true)
    private String inputFileName2;
    @Option(name = "-q")
    private int q;
    @Option(name = "--autoQ")
    private  boolean autoJPEGQ;
    @Option(name = "--redirectOutput", aliases = {"-o"})
    private String filename;

    boolean isInteractive() {
        return interactive;
    }

    String getInputFile1() {
        return inputFileName;
    }

    int getQ() {
        return q;
    }

    String getInputFile2() {
        return inputFileName2;
    }

    boolean hasAutoJPEGQ() {
        return autoJPEGQ;
    }

    String getFilename() {
        return filename;
    }
}
