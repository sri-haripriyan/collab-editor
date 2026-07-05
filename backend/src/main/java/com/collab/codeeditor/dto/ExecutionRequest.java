package com.collab.codeeditor.dto;

public class ExecutionRequest {
    private String sourceCode;
    private String language;
    private String input;

    public ExecutionRequest() {
    }

    public ExecutionRequest(String sourceCode, String language, String input) {
        this.sourceCode = sourceCode;
        this.language = language;
        this.input = input;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
