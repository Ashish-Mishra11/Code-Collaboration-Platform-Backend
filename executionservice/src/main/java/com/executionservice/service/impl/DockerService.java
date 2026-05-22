package com.executionservice.service.impl;

import com.executionservice.dto.ExecutionResult;
import com.executionservice.entity.ExecutionJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DockerService {

    private static final long DEFAULT_TIMEOUT_SECONDS = 15;
    private static final String WORKSPACE = "/workspace";

    public ExecutionResult runCode(ExecutionJob job) {
        Path tempDir = null;
        String containerName = "exec_" + job.getJobId().replace("-", "").substring(0, 12);

        try {
            tempDir = Files.createTempDirectory("sandbox_" + job.getJobId());

            // Write source code file
            String filename = getSourceFileName(job.getLanguage());
            Path sourcePath = tempDir.resolve(filename);
            Files.writeString(sourcePath, job.getSourceCode());

            // Write stdin file (always create it so the script can redirect from it)
            String stdinContent = (job.getStdin() != null && !job.getStdin().isBlank())
                    ? job.getStdin() : "";
            Files.writeString(tempDir.resolve("input.txt"), stdinContent);

            String image = getDockerImage(job.getLanguage());
            String runScript = buildRunScript(job.getLanguage(),job.getSourceCode());

            List<String> command = Arrays.asList(
                    "docker", "run", "--rm",
                    "--name", containerName,
                    "--network", "none",
                    "--cpus", "0.5",
                    "--memory", "256m",
                    "--memory-swap", "256m",
                    "-v", tempDir.toAbsolutePath() + ":" + WORKSPACE + ":ro",
                    "--workdir", WORKSPACE,
                    image,
                    "sh", "-c", "timeout " + DEFAULT_TIMEOUT_SECONDS + "s sh -c '" + runScript + "' < " + WORKSPACE + "/input.txt"
            );

            log.info("Running docker command for job {}: language={}", job.getJobId(), job.getLanguage());

            long start = System.currentTimeMillis();
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Read streams concurrently to avoid blocking
            StringBuilder stdoutBuf = new StringBuilder();
            StringBuilder stderrBuf = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) stdoutBuf.append(line).append("\n");
                } catch (IOException ignored) {}
            });
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) stderrBuf.append(line).append("\n");
                } catch (IOException ignored) {}
            });
            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS + 5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                killContainer(containerName);
            }

            stdoutThread.join(2000);
            stderrThread.join(2000);
            long end = System.currentTimeMillis();

            String stdout = stdoutBuf.toString().trim();
            String stderr = stderrBuf.toString().trim();
            int exitCode = finished ? process.exitValue() : -1;

            log.info("Job {} finished: exitCode={} timeMs={}", job.getJobId(), exitCode, end - start);

            return ExecutionResult.builder()
                    .success(exitCode == 0)
                    .stdout(stdout)
                    .stderr(stderr)
                    .exitCode(exitCode)
                    .executionTimeMs(end - start)
                    .errorMessage(!finished ? "Execution timed out after " + DEFAULT_TIMEOUT_SECONDS + "s" : null)
                    .build();

        } catch (Exception e) {
            log.error("Execution failed for job {}", job.getJobId(), e);
            return ExecutionResult.builder()
                    .success(false)
                    .stdout("")
                    .stderr(e.getMessage())
                    .exitCode(-1)
                    .errorMessage(e.getMessage())
                    .build();
        } finally {
            if (tempDir != null) deleteDirectory(tempDir);
        }
    }

    /**
     * Builds the inline shell script to compile + run code for each language.
     * This removes the need for an external /run_code.sh baked into the image.
     */
 // Updated buildRunScript - Now takes sourceCode as parameter (No 'job' dependency)
    private String buildRunScript(String language, String sourceCode) {
        return switch (language.toLowerCase()) {
            case "python" -> WORKSPACE + "/main.py";

            case "javascript" -> WORKSPACE + "/main.js";

            case "java" -> buildJavaCommand(sourceCode);   // Pass sourceCode here

            case "c++" -> "cd /tmp && cp " + WORKSPACE + "/main.cpp . && g++ -o main main.cpp && ./main";

            case "go" -> "cd /tmp && cp " + WORKSPACE + "/main.go . && go run main.go";

            case "rust" -> "cd /tmp && cp " + WORKSPACE + "/main.rs . && rustc -o main main.rs && ./main";

            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }
    
    //extract the public class name of the class which contains main method
 // Add this method in your DockerService class
    private String extractClassName(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return "Main";
        }

        // Improved regex to find class name (handles public class, class, extra spaces)
        Pattern pattern = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?(?:final\\s+|abstract\\s+)?class\\s+(\\w+)", 
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "Main"; // fallback
    }
 // New helper method for Java
    private String buildJavaCommand(String sourceCode) {
        String className = extractClassName(sourceCode);

        return "cd /tmp && " +
               "cp " + WORKSPACE + "/Main.java " + className + ".java && " +
               "javac " + className + ".java && " +
               "java " + className;
    }
    
  

    private String getDockerImage(String language) {
        return switch (language.toLowerCase()) {
            case "java"       -> "eclipse-temurin:21-jdk-alpine";
            case "python"     -> "python:3.12-slim";
            case "javascript" -> "node:20-slim";
            case "c++"        -> "gcc:13";
            case "go"         -> "golang:1.22-alpine";
            case "rust"       -> "rust:1.77-slim";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private String getSourceFileName(String language) {
        return switch (language.toLowerCase()) {
            case "java"       -> "Main.java";
            case "python"     -> "main.py";
            case "javascript" -> "main.js";
            case "c++"        -> "main.cpp";
            case "go"         -> "main.go";
            case "rust"       -> "main.rs";
            default -> "code.txt";
        };
    }

    public void killContainer(String containerIdOrName) {
        try {
            new ProcessBuilder("docker", "kill", containerIdOrName).start().waitFor(3, TimeUnit.SECONDS);
            new ProcessBuilder("docker", "rm", "-f", containerIdOrName).start().waitFor(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to kill container {}: {}", containerIdOrName, e.getMessage());
        }
    }

    private void deleteDirectory(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}
