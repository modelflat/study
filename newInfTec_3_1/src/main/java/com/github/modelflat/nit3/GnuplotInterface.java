package com.github.modelflat.nit3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GnuplotInterface {

    private static final String BASE = "gnuplot";

    private List<String> commands;
    private List<String> workspaceCommands;
    private int prevHash;
    private String compiledWorkspace;
    private boolean interactive;

    public GnuplotInterface() {
        this(false);
    }

    public GnuplotInterface(boolean interactive) {
        commands = new LinkedList<>();
        workspaceCommands = new LinkedList<>();
        compiledWorkspace = "";
        prevHash = workspaceCommands.hashCode();
        this.interactive = interactive;
    }

    public GnuplotInterface addWorkspaceCommand(String command) {
        workspaceCommands.add(command);
        return this;
    }

    public GnuplotInterface addCommand(String command) {
        commands.add(command);
        return this;
    }

    public void invoke() throws IOException {

        if (prevHash != workspaceCommands.hashCode()) {
            compiledWorkspace = String.join("; ", workspaceCommands);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                BASE,
                interactive ? "-p" : "",
                "-e",
                String.format("\"%s; %s\"", compiledWorkspace, String.join("; ", commands)))
                .inheritIO();
        commands.clear();

        Process gnuplotProcess = processBuilder.start();
        int ret;
        try {
            ret = gnuplotProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        if (ret != 0) {
            throw new RuntimeException(String.format("Gnuplot error code: %d", ret));
        }
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }
}
