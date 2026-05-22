package com.executionservice;

import com.executionservice.dto.ExecutionResult;
import com.executionservice.dto.ExecutionStatsDto;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.exception.ResourceNotFoundException;
import com.executionservice.repository.ExecutionRepository;
import com.executionservice.service.impl.DockerService;
import com.executionservice.service.impl.ExecutionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionService Unit Tests")
class ExecutionserviceApplicationTests {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private DockerService dockerService;

    @InjectMocks
    private ExecutionServiceImpl executionService;

    private ExecutionJob sampleJob;
    private ExecutionResult successResult;

    @BeforeEach
    void setUp() {
        // Replace async executor with synchronous one so tests run predictably
        executionService = new ExecutionServiceImpl(executionRepository, dockerService, new SyncTaskExecutor());

        sampleJob = ExecutionJob.builder()
                .language("Python")
                .sourceCode("print('hello')")
                .userId(1)
                .projectId(10)
                .fileId(100)
                .stdin("")
                .build();

        successResult = ExecutionResult.builder()
                .success(true)
                .stdout("hello")
                .stderr("")
                .exitCode(0)
                .executionTimeMs(200L)
                .build();
    }

    // ── runSynchronously ───────────────────────────────────────────────────────

    @Test
    @DisplayName("runSynchronously: returns SUCCESS with stdout when Docker succeeds")
    void runSynchronously_success() {
        when(executionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            ExecutionJob j = inv.getArgument(0);
            if (j.getJobId() == null) j.setJobId(UUID.randomUUID().toString());
            return j;
        });
        when(dockerService.runCode(any())).thenReturn(successResult);

        ExecutionJob result = executionService.runSynchronously(sampleJob);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getStdout()).isEqualTo("hello");
        assertThat(result.getExitCode()).isEqualTo(0);
        verify(dockerService, times(1)).runCode(any());
    }

    @Test
    @DisplayName("runSynchronously: returns FAILED with stderr when Docker reports failure")
    void runSynchronously_failure() {
        ExecutionResult failResult = ExecutionResult.builder()
                .success(false).stdout("").stderr("SyntaxError").exitCode(1).executionTimeMs(50L).build();

        when(executionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            ExecutionJob j = inv.getArgument(0);
            if (j.getJobId() == null) j.setJobId(UUID.randomUUID().toString());
            return j;
        });
        when(dockerService.runCode(any())).thenReturn(failResult);

        ExecutionJob result = executionService.runSynchronously(sampleJob);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getStderr()).isEqualTo("SyntaxError");
    }

    @Test
    @DisplayName("runSynchronously: marks FAILED when DockerService throws exception")
    void runSynchronously_dockerThrows() {
        when(executionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            ExecutionJob j = inv.getArgument(0);
            if (j.getJobId() == null) j.setJobId(UUID.randomUUID().toString());
            return j;
        });
        when(dockerService.runCode(any())).thenThrow(new RuntimeException("Docker unavailable"));

        ExecutionJob result = executionService.runSynchronously(sampleJob);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getStderr()).contains("Docker unavailable");
    }

    // ── submitExecution (async) ────────────────────────────────────────────────

    @Test
    @DisplayName("submitExecution: persists job with PENDING status initially")
    void submitExecution_pendingStatus() {
        ExecutionJob pending = ExecutionJob.builder()
                .jobId(UUID.randomUUID().toString()).status("PENDING")
                .language("Python").sourceCode("x=1").userId(1).projectId(10).fileId(100).stdin("")
                .createdAt(LocalDateTime.now()).build();

        org.springframework.core.task.TaskExecutor mockExecutor = mock(org.springframework.core.task.TaskExecutor.class);
        ExecutionServiceImpl asyncExecutionService = new ExecutionServiceImpl(executionRepository, dockerService, mockExecutor);

        when(executionRepository.saveAndFlush(any())).thenReturn(pending);

        ExecutionJob result = asyncExecutionService.submitExecution(sampleJob);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(executionRepository, atLeastOnce()).saveAndFlush(any());
        verify(mockExecutor).execute(any(Runnable.class));
    }

    // ── getJobById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getJobById: returns job when found")
    void getJobById_found() {
        ExecutionJob job = ExecutionJob.builder().jobId("abc-123").status("SUCCESS").build();
        when(executionRepository.findByJobId("abc-123")).thenReturn(Optional.of(job));

        Optional<ExecutionJob> result = executionService.getJobById("abc-123");

        assertThat(result).isPresent();
        assertThat(result.get().getJobId()).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("getJobById: returns empty when not found")
    void getJobById_notFound() {
        when(executionRepository.findByJobId("missing")).thenReturn(Optional.empty());
        assertThat(executionService.getJobById("missing")).isEmpty();
    }

    // ── cancelExecution ────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelExecution: sets status to CANCELLED")
    void cancelExecution_success() {
        ExecutionJob job = ExecutionJob.builder().jobId("job-1").status("PENDING").build();
        when(executionRepository.findByJobId("job-1")).thenReturn(Optional.of(job));
        when(executionRepository.save(any())).thenReturn(job);

        executionService.cancelExecution("job-1");

        assertThat(job.getStatus()).isEqualTo("CANCELLED");
        verify(executionRepository).save(job);
    }

    @Test
    @DisplayName("cancelExecution: throws when job not found")
    void cancelExecution_notFound() {
        when(executionRepository.findByJobId("none")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> executionService.cancelExecution("none"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSupportedLanguages ──────────────────────────────────────────────────

    @Test
    @DisplayName("getSupportedLanguages: returns all 6 supported languages")
    void getSupportedLanguages() {
        List<String> langs = executionService.getSupportedLanguages();
        assertThat(langs).hasSize(6).contains("Java", "Python", "JavaScript", "C++", "Go", "Rust");
    }

    // ── getExecutionStats ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getExecutionStats: aggregates counts correctly")
    void getExecutionStats() {
        when(executionRepository.countByUserId(1)).thenReturn(10L);
        when(executionRepository.countByUserIdAndStatus(1, "SUCCESS")).thenReturn(6L);
        when(executionRepository.countByUserIdAndStatus(1, "FAILED")).thenReturn(2L);
        when(executionRepository.countByUserIdAndStatus(1, "PENDING")).thenReturn(1L);
        when(executionRepository.countByUserIdAndStatus(1, "CANCELLED")).thenReturn(1L);

        ExecutionStatsDto stats = executionService.getExecutionStats(1);

        assertThat(stats.getTotalJobs()).isEqualTo(10L);
        assertThat(stats.getSuccessful()).isEqualTo(6L);
        assertThat(stats.getFailed()).isEqualTo(2L);
    }
}
