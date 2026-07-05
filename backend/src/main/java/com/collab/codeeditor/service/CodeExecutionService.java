package com.collab.codeeditor.service;

import com.collab.codeeditor.dto.ExecutionRequest;
import com.collab.codeeditor.dto.ExecutionResponse;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    private static final long TIMEOUT_MS = 5000; // 5 seconds safety timeout
    private static final Path TEMP_DIR = Paths.get("./temp_runs");

    public ExecutionResponse executeCode(ExecutionRequest request) {
        String lang = request.getLanguage().toLowerCase();
        String code = request.getSourceCode();
        String input = request.getInput() != null ? request.getInput() : "";

        // Create run directory
        String runId = UUID.randomUUID().toString();
        Path runPath = TEMP_DIR.resolve(runId);
        
        try {
            Files.createDirectories(runPath);
        } catch (IOException e) {
            return new ExecutionResponse("", "Failed to create runtime sandbox: " + e.getMessage(), 1, 0);
        }

        try {
            if ("python".equals(lang)) {
                return runPython(runPath, code, input);
            } else if ("java".equals(lang)) {
                return runJava(runPath, code, input);
            } else if ("cpp".equals(lang) || "c++".equals(lang)) {
                return runCpp(runPath, code, input);
            } else {
                return new ExecutionResponse("", "Unsupported language: " + lang, 1, 0);
            }
        } finally {
            // Clean up files in runtime directory
            deleteDirectory(runPath.toFile());
        }
    }

    private ExecutionResponse runPython(Path runPath, String code, String input) {
        Path filePath = runPath.resolve("script.py");
        try {
            Files.writeString(filePath, code);
        } catch (IOException e) {
            return new ExecutionResponse("", "Failed to write source file: " + e.getMessage(), 1, 0);
        }

        return executeProcess(new String[]{"python", filePath.toString()}, runPath, input);
    }

    private ExecutionResponse runJava(Path runPath, String code, String input) {
        // Enforce class name to be Main
        Path filePath = runPath.resolve("Main.java");
        try {
            Files.writeString(filePath, code);
        } catch (IOException e) {
            return new ExecutionResponse("", "Failed to write source file: " + e.getMessage(), 1, 0);
        }

        // Compile
        long startTime = System.currentTimeMillis();
        ExecutionResponse compileResult = executeProcess(new String[]{"javac", "Main.java"}, runPath, "");
        if (compileResult.getExitCode() != 0) {
            return new ExecutionResponse("", "Compilation Error:\n" + compileResult.getStderr(), compileResult.getExitCode(), System.currentTimeMillis() - startTime);
        }

        // Run
        return executeProcess(new String[]{"java", "Main"}, runPath, input);
    }

    private ExecutionResponse runCpp(Path runPath, String code, String input) {
        Path filePath = runPath.resolve("program.cpp");
        String binaryName = "program.exe";
        try {
            Files.writeString(filePath, code);
        } catch (IOException e) {
            return new ExecutionResponse("", "Failed to write source file: " + e.getMessage(), 1, 0);
        }

        // Compile
        long startTime = System.currentTimeMillis();
        ExecutionResponse compileResult = executeProcess(new String[]{"g++", "program.cpp", "-o", binaryName}, runPath, "");
        if (compileResult.getExitCode() != 0) {
            return new ExecutionResponse("", "Compilation Error:\n" + compileResult.getStderr(), compileResult.getExitCode(), System.currentTimeMillis() - startTime);
        }

        // Run
        return executeProcess(new String[]{runPath.resolve(binaryName).toString()}, runPath, input);
    }

    private ExecutionResponse executeProcess(String[] command, Path workingDir, String input) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());

        long startTime = System.currentTimeMillis();
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return new ExecutionResponse("", "Failed to start process: " + e.getMessage(), 1, 0);
        }

        // Write input to process standard input stream
        if (input != null && !input.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(input);
                writer.flush();
            } catch (IOException e) {
                // Ignore process closed pipeline errors
            }
        } else {
            try {
                process.getOutputStream().close();
            } catch (IOException e) {
                // Ignore
            }
        }

        // Capture stdout and stderr
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Thread outThread = new Thread(() -> readStream(process.getInputStream(), stdoutBuilder));
        Thread errThread = new Thread(() -> readStream(process.getErrorStream(), stderrBuilder));

        outThread.start();
        errThread.start();

        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            return new ExecutionResponse("", "Execution Interrupted", 1, System.currentTimeMillis() - startTime);
        }

        long timeMs = System.currentTimeMillis() - startTime;

        if (!finished) {
            process.destroyForcibly();
            try {
                outThread.join(500);
                errThread.join(500);
            } catch (InterruptedException e) {
                // Ignore
            }
            return new ExecutionResponse(stdoutBuilder.toString(), stderrBuilder.toString() + "\n[Time Limit Exceeded (5000ms)]", 124, timeMs);
        }

        try {
            outThread.join(1000);
            errThread.join(1000);
        } catch (InterruptedException e) {
            // Ignore
        }

        return new ExecutionResponse(stdoutBuilder.toString(), stderrBuilder.toString(), process.exitValue(), timeMs);
    }

    private void readStream(InputStream is, StringBuilder sb) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                // Limit buffer size to prevent memory attacks
                if (sb.length() > 50000) {
                    sb.append("\n[Output Truncated]");
                    break;
                }
            }
        } catch (IOException e) {
            // Stream closed
        }
    }

    private void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
