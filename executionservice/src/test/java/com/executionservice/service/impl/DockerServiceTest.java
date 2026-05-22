package com.executionservice.service.impl;

import com.executionservice.dto.ExecutionResult;
import com.executionservice.entity.ExecutionJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerServiceTest {

    private DockerService dockerService;

    @BeforeEach
    void setUp() {
        dockerService = new DockerService();
    }

    @Test
    void testRunCode_Success() throws Exception {
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-12345678-abcd")
                .language("python")
                .sourceCode("print('Hello World')")
                .stdin("input_data")
                .build();

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process mockProcess = mock(Process.class);
                    InputStream stdout = new ByteArrayInputStream("Hello World\n".getBytes(StandardCharsets.UTF_8));
                    InputStream stderr = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                    
                    when(mock.start()).thenReturn(mockProcess);
                    when(mockProcess.getInputStream()).thenReturn(stdout);
                    when(mockProcess.getErrorStream()).thenReturn(stderr);
                    when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
                    when(mockProcess.exitValue()).thenReturn(0);
                })) {

            ExecutionResult result = dockerService.runCode(job);

            assertTrue(result.isSuccess());
            assertEquals("Hello World", result.getStdout());
            assertEquals("", result.getStderr());
            assertEquals(0, result.getExitCode());
            assertNull(result.getErrorMessage());
        }
    }

    @Test
    void testRunCode_Timeout() throws Exception {
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-timeout-1234567890")
                .language("javascript")
                .sourceCode("while(true) {}")
                .build();

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process mockProcess = mock(Process.class);
                    InputStream stdout = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                    InputStream stderr = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                    
                    when(mock.start()).thenReturn(mockProcess);
                    lenient().when(mockProcess.getInputStream()).thenReturn(stdout);
                    lenient().when(mockProcess.getErrorStream()).thenReturn(stderr);
                    when(mockProcess.waitFor(anyLong(), any())).thenReturn(false); // Timeout
                })) {

            ExecutionResult result = dockerService.runCode(job);

            assertFalse(result.isSuccess());
            assertEquals(-1, result.getExitCode());
            assertTrue(result.getErrorMessage().contains("timed out"));
        }
    }

    @Test
    void testRunCode_UnsupportedLanguage() {
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-unsupported-12345678")
                .language("ruby")
                .sourceCode("puts 'Hello'")
                .build();

        ExecutionResult result = dockerService.runCode(job);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Unsupported language: ruby"));
    }

    @Test
    void testRunCode_JavaExecution() throws Exception {
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-java-1234567890")
                .language("java")
                .sourceCode("public class TestClass { public static void main(String[] args) {} }")
                .build();

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process mockProcess = mock(Process.class);
                    InputStream stdout = new ByteArrayInputStream("Java Success".getBytes(StandardCharsets.UTF_8));
                    InputStream stderr = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                    
                    when(mock.start()).thenReturn(mockProcess);
                    when(mockProcess.getInputStream()).thenReturn(stdout);
                    when(mockProcess.getErrorStream()).thenReturn(stderr);
                    when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
                    when(mockProcess.exitValue()).thenReturn(0);
                })) {

            ExecutionResult result = dockerService.runCode(job);

            assertTrue(result.isSuccess());
            assertEquals("Java Success", result.getStdout());
        }
    }
    
    @Test
    void testRunCode_CppExecution() throws Exception {
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-cpp-1234567890")
                .language("c++")
                .sourceCode("int main() {}")
                .build();

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process mockProcess = mock(Process.class);
                    when(mock.start()).thenReturn(mockProcess);
                    when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
                    when(mockProcess.exitValue()).thenReturn(0);
                })) {
            ExecutionResult result = dockerService.runCode(job);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void testRunCode_GoExecution() throws Exception {
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-go-1234567890")
                .language("go")
                .sourceCode("package main")
                .build();

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process mockProcess = mock(Process.class);
                    when(mock.start()).thenReturn(mockProcess);
                    when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
                    when(mockProcess.exitValue()).thenReturn(0);
                })) {
            ExecutionResult result = dockerService.runCode(job);
            assertTrue(result.isSuccess());
        }
    }
    
    @Test
    void testRunCode_RustExecution() throws Exception {
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-rust-1234567890")
                .language("rust")
                .sourceCode("fn main() {}")
                .build();

        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process mockProcess = mock(Process.class);
                    when(mock.start()).thenReturn(mockProcess);
                    when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
                    when(mockProcess.exitValue()).thenReturn(0);
                })) {
            ExecutionResult result = dockerService.runCode(job);
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void testExtractClassName() throws Exception {
        Method method = DockerService.class.getDeclaredMethod("extractClassName", String.class);
        method.setAccessible(true);

        assertEquals("MyProgram", method.invoke(dockerService, "public class MyProgram {"));
        assertEquals("Worker", method.invoke(dockerService, "class Worker implements Runnable {"));
        assertEquals("Main", method.invoke(dockerService, "")); // fallback
        assertEquals("Main", method.invoke(dockerService, "def some_python()")); // fallback
    }

    @Test
    void testKillContainer() throws Exception {
        try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process mockProcess = mock(Process.class);
                    when(mock.start()).thenReturn(mockProcess);
                    when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
                })) {

            dockerService.killContainer("exec_container123");

            // Verify ProcessBuilder was created twice (docker kill and docker rm)
            assertEquals(2, mocked.constructed().size());
        }
    }
}
