package com.neuon.tools;

public class ProcessResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public ProcessResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }
}
