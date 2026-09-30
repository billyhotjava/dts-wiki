package com.yuzhi.dts.wiki.service.wiki.sync;

/** A failed git CLI invocation (command, exit code, stderr tail). */
public class GitCommandException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String command;
    private final int exitCode;

    public GitCommandException(String command, int exitCode, String stderr) {
        super("git " + command + " failed with exit " + exitCode + ": " + stderr);
        this.command = command;
        this.exitCode = exitCode;
    }

    public String getCommand() {
        return command;
    }

    public int getExitCode() {
        return exitCode;
    }
}
