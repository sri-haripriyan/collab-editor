package com.collab.codeeditor.dto;

public class ExecutionResponse {
    private String stdout;
    private String stderr;
    private int exitCode;
    private long timeMs;

    public ExecutionResponse() {
    }

    public ExecutionResponse(String stdout, String stderr, int exitCode, long timeMs) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
        this.timeMs = timeMs;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(long timeMs) {
        this.timeMs = timeMs;
    }
}
