package com.collab.editor.service;

import com.collab.editor.model.ExecutionResult;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CodeExecutionService {
    
    private static final long TIMEOUT_SECONDS = 10;
    
    public ExecutionResult executeCode(String code, String language) {
        long startTime = System.currentTimeMillis();
        try {
            String output;
            
            switch (language.toLowerCase()) {
                case "python":
                    output = executePython(code);
                    break;
                case "javascript":
                    output = executeJavaScript(code);
                    break;
                case "java":
                    output = executeJava(code);
                    break;
                case "cpp":
                case "c++":
                    output = executeCpp(code);
                    break;
                default:
                    return ExecutionResult.error("Unsupported language: " + language);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return new ExecutionResult(true, output, null, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new ExecutionResult(false, null, e.getMessage(), executionTime);
        }
    }
    
    private String executePython(String code) throws IOException, InterruptedException {
        File tempFile = File.createTempFile("code", ".py");
        Files.write(tempFile.toPath(), code.getBytes());
        
        ProcessBuilder pb = new ProcessBuilder("python3", tempFile.getAbsolutePath());
        return executeProcess(pb, tempFile);
    }
    
    private String executeJavaScript(String code) throws IOException, InterruptedException {
        File tempFile = File.createTempFile("code", ".js");
        Files.write(tempFile.toPath(), code.getBytes());
        
        ProcessBuilder pb = new ProcessBuilder("node", tempFile.getAbsolutePath());
        return executeProcess(pb, tempFile);
    }
    
    private String executeJava(String code) throws IOException, InterruptedException {
        // Extract class name from code
        String className = extractJavaClassName(code);
        if (className == null) {
            throw new RuntimeException("Could not find public class in Java code");
        }
        
        // Create temp directory
        File tempDir = Files.createTempDirectory("java_exec").toFile();
        File javaFile = new File(tempDir, className + ".java");
        Files.write(javaFile.toPath(), code.getBytes());
        
        // Compile
        ProcessBuilder compileProcess = new ProcessBuilder("javac", javaFile.getAbsolutePath());
        compileProcess.directory(tempDir);
        Process compile = compileProcess.start();
        
        if (!compile.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            compile.destroy();
            throw new RuntimeException("Compilation timeout");
        }
        
        if (compile.exitValue() != 0) {
            String error = readStream(compile.getErrorStream());
            javaFile.delete();
            tempDir.delete();
            throw new RuntimeException("Compilation error: " + error);
        }
        
        // Execute
        ProcessBuilder runProcess = new ProcessBuilder("java", "-cp", tempDir.getAbsolutePath(), className);
        String output = executeProcess(runProcess, null);
        
        // Cleanup
        new File(tempDir, className + ".class").delete();
        javaFile.delete();
        tempDir.delete();
        
        return output;
    }
    
    private String executeCpp(String code) throws IOException, InterruptedException {
        File tempFile = File.createTempFile("code", ".cpp");
        Files.write(tempFile.toPath(), code.getBytes());
        
        File outputFile = File.createTempFile("output", ".out");
        
        // Compile
        ProcessBuilder compileProcess = new ProcessBuilder("g++", tempFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath());
        Process compile = compileProcess.start();
        
        if (!compile.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            compile.destroy();
            throw new RuntimeException("Compilation timeout");
        }
        
        if (compile.exitValue() != 0) {
            String error = readStream(compile.getErrorStream());
            tempFile.delete();
            outputFile.delete();
            throw new RuntimeException("Compilation error: " + error);
        }
        
        // Execute
        ProcessBuilder runProcess = new ProcessBuilder(outputFile.getAbsolutePath());
        String output = executeProcess(runProcess, null);
        
        // Cleanup
        tempFile.delete();
        outputFile.delete();
        
        return output;
    }
    
    private String executeProcess(ProcessBuilder pb, File tempFile) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroy();
            if (tempFile != null) tempFile.delete();
            throw new RuntimeException("Execution timeout");
        }
        
        String output = readStream(process.getInputStream());
        
        if (tempFile != null) {
            tempFile.delete();
        }
        
        return output;
    }
    
    private String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    private String extractJavaClassName(String code) {
        // Simple regex to find public class name
        String[] lines = code.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("public class ")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    return parts[2].replace("{", "").trim();
                }
            }
        }
        return null;
    }
}