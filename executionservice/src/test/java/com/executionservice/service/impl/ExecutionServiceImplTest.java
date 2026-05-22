package com.executionservice.service.impl;

import com.executionservice.dto.ExecutionResult;
import com.executionservice.dto.ExecutionStatsDto;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.exception.ResourceNotFoundException;
import com.executionservice.repository.ExecutionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionService Unit Tests")
class ExecutionServiceImplTest {

    @Mock private ExecutionRepository executionRepository;
    @Mock private DockerService dockerService;
    @Mock private TaskExecutor taskExecutor;

    @InjectMocks private ExecutionServiceImpl executionService;

    private ExecutionJob buildJob() {
        ExecutionJob job = new ExecutionJob();
        job.setUserId(1);
        job.setProjectId(10);
        job.setFileId(5);
        job.setLanguage("Python");
        job.setSourceCode("print('hello')");
        return job;
    }

    // ── runSynchronously ──────────────────────────────────────────────────────

    @Test
    @DisplayName("runSynchronously – returns SUCCESS job when docker succeeds")
    void runSynchronously_dockerSuccess_returnsSuccessJob() throws Exception {
        ExecutionJob job = buildJob();

        ExecutionResult result = new ExecutionResult();
        result.setSuccess(true);
        result.setStdout("hello\n");
        result.setStderr("");
        result.setExitCode(0);
        result.setExecutionTimeMs(100L);

        when(executionRepository.saveAndFlush(any(ExecutionJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dockerService.runCode(any(ExecutionJob.class))).thenReturn(result);

        ExecutionJob completed = executionService.runSynchronously(job);

        assertThat(completed.getStatus()).isEqualTo("SUCCESS");
        assertThat(completed.getStdout()).isEqualTo("hello\n");
        assertThat(completed.getExitCode()).isZero();
        verify(executionRepository, times(2)).saveAndFlush(any());
    }

    @Test
    @DisplayName("runSynchronously – returns FAILED job when docker returns failure result")
    void runSynchronously_dockerFails_returnsFailedJob() throws Exception {
        ExecutionJob job = buildJob();

        ExecutionResult result = new ExecutionResult();
        result.setSuccess(false);
        result.setStdout("");
        result.setStderr("SyntaxError");
        result.setExitCode(1);
        result.setExecutionTimeMs(50L);

        when(executionRepository.saveAndFlush(any(ExecutionJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dockerService.runCode(any(ExecutionJob.class))).thenReturn(result);

        ExecutionJob completed = executionService.runSynchronously(job);

        assertThat(completed.getStatus()).isEqualTo("FAILED");
        assertThat(completed.getStderr()).isEqualTo("SyntaxError");
    }

    @Test
    @DisplayName("runSynchronously – returns FAILED job when docker throws exception")
    void runSynchronously_dockerThrows_returnsFailedJob() throws Exception {
        ExecutionJob job = buildJob();
        when(executionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dockerService.runCode(any())).thenThrow(new RuntimeException("Container timeout"));

        ExecutionJob completed = executionService.runSynchronously(job);

        assertThat(completed.getStatus()).isEqualTo("FAILED");
        assertThat(completed.getStderr()).contains("Container timeout");
    }

    @Test
    @DisplayName("runSynchronously – sets empty stdin when null")
    void runSynchronously_nullStdin_setsEmpty() throws Exception {
        ExecutionJob job = buildJob();
        job.setStdin(null);

        ExecutionResult result = new ExecutionResult();
        result.setSuccess(true);
        result.setStdout("ok");
        result.setStderr("");
        result.setExitCode(0);

        when(executionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dockerService.runCode(any())).thenReturn(result);

        ExecutionJob completed = executionService.runSynchronously(job);
        assertThat(completed.getStatus()).isEqualTo("SUCCESS");
    }

    // ── submitExecution ───────────────────────────────────────────────────────

    @Test
    @DisplayName("submitExecution – saves job as PENDING and submits to taskExecutor")
    void submitExecution_schedulesAsyncTask() {
        ExecutionJob job = buildJob();
        when(executionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            ExecutionJob j = inv.getArgument(0);
            j.setJobId("job-123");
            return j;
        });
        doNothing().when(taskExecutor).execute(any(Runnable.class));

        ExecutionJob saved = executionService.submitExecution(job);

        assertThat(saved.getStatus()).isEqualTo("PENDING");
        verify(taskExecutor).execute(any(Runnable.class));
    }

    // ── executeJobAsync ───────────────────────────────────────────────────────

    @Test
    @DisplayName("executeJobAsync – sets SUCCESS when docker succeeds")
    void executeJobAsync_dockerSuccess_setsSuccess() throws Exception {
        ExecutionJob job = buildJob();
        job.setJobId("async-job-1");

        ExecutionResult result = new ExecutionResult();
        result.setSuccess(true);
        result.setStdout("output");
        result.setStderr("");
        result.setExitCode(0);
        result.setExecutionTimeMs(200L);

        when(executionRepository.findByJobId("async-job-1")).thenReturn(Optional.of(job));
        when(executionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dockerService.runCode(any())).thenReturn(result);

        executionService.executeJobAsync("async-job-1");

        verify(executionRepository, atLeast(2)).saveAndFlush(any());
    }

    @Test
    @DisplayName("executeJobAsync – returns early when job not found")
    void executeJobAsync_jobNotFound_returnsEarly() {
        when(executionRepository.findByJobId("missing-id")).thenReturn(Optional.empty());

        assertThatCode(() -> executionService.executeJobAsync("missing-id")).doesNotThrowAnyException();
        verify(dockerService, never()).runCode(any());
    }

    // ── cancelExecution ───────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelExecution – sets CANCELLED status")
    void cancelExecution_pendingJob_setsCancelled() {
        ExecutionJob job = buildJob();
        job.setJobId("job-cancel");
        job.setStatus("PENDING");

        when(executionRepository.findByJobId("job-cancel")).thenReturn(Optional.of(job));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.cancelExecution("job-cancel");

        verify(executionRepository).save(argThat(j -> "CANCELLED".equals(j.getStatus())));
    }

    @Test
    @DisplayName("cancelExecution – throws ResourceNotFoundException when job not found")
    void cancelExecution_jobNotFound_throwsException() {
        when(executionRepository.findByJobId("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> executionService.cancelExecution("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancelExecution – throws IllegalArgumentException when already cancelled")
    void cancelExecution_alreadyCancelled_throwsException() {
        ExecutionJob job = buildJob();
        job.setJobId("already-cancelled");
        job.setStatus("CANCELLED");

        when(executionRepository.findByJobId("already-cancelled")).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> executionService.cancelExecution("already-cancelled"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job already cancelled");
    }

    // ── getJobById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getJobById – returns Optional with job when found")
    void getJobById_found_returnsOptional() {
        ExecutionJob job = buildJob();
        when(executionRepository.findByJobId("job-1")).thenReturn(Optional.of(job));
        assertThat(executionService.getJobById("job-1")).isPresent();
    }

    @Test
    @DisplayName("getJobById – returns empty Optional when not found")
    void getJobById_notFound_returnsEmpty() {
        when(executionRepository.findByJobId("ghost")).thenReturn(Optional.empty());
        assertThat(executionService.getJobById("ghost")).isEmpty();
    }

    // ── getExecutionsByUser ───────────────────────────────────────────────────

    @Test
    @DisplayName("getExecutionsByUser – returns list from repository")
    void getExecutionsByUser_returnsList() {
        List<ExecutionJob> jobs = List.of(buildJob(), buildJob());
        when(executionRepository.findByUserId(1)).thenReturn(jobs);
        assertThat(executionService.getExecutionsByUser(1)).hasSize(2);
    }

    // ── getExecutionsByProject ────────────────────────────────────────────────

    @Test
    @DisplayName("getExecutionsByProject – returns list from repository")
    void getExecutionsByProject_returnsList() {
        when(executionRepository.findByProjectId(10)).thenReturn(List.of(buildJob()));
        assertThat(executionService.getExecutionsByProject(10)).hasSize(1);
    }

    // ── getExecutionResult ────────────────────────────────────────────────────

    @Test
    @DisplayName("getExecutionResult – returns job when found")
    void getExecutionResult_found_returnsJob() {
        ExecutionJob job = buildJob();
        when(executionRepository.findByJobId("result-job")).thenReturn(Optional.of(job));
        assertThat(executionService.getExecutionResult("result-job")).isEqualTo(job);
    }

    @Test
    @DisplayName("getExecutionResult – throws ResourceNotFoundException when not found")
    void getExecutionResult_notFound_throws() {
        when(executionRepository.findByJobId("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> executionService.getExecutionResult("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSupportedLanguages ─────────────────────────────────────────────────

    @Test
    @DisplayName("getSupportedLanguages – returns non-empty list")
    void getSupportedLanguages_returnsNonEmpty() {
        List<String> langs = executionService.getSupportedLanguages();
        assertThat(langs).isNotEmpty().contains("Java", "Python");
    }

    // ── getLanguageVersion ────────────────────────────────────────────────────

    @Test
    @DisplayName("getLanguageVersion – returns correct version for Java")
    void getLanguageVersion_java_returns21() {
        assertThat(executionService.getLanguageVersion("Java")).isEqualTo("21");
    }

    @Test
    @DisplayName("getLanguageVersion – returns 'Unknown' for unsupported language")
    void getLanguageVersion_unknown_returnsUnknown() {
        assertThat(executionService.getLanguageVersion("COBOL")).isEqualTo("Unknown");
    }

    // ── getExecutionStats ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getExecutionStats – aggregates counts from repository")
    void getExecutionStats_returnsAggregatedStats() {
        when(executionRepository.countByUserId(1)).thenReturn(10L);
        when(executionRepository.countByUserIdAndStatus(1, "SUCCESS")).thenReturn(7L);
        when(executionRepository.countByUserIdAndStatus(1, "FAILED")).thenReturn(2L);
        when(executionRepository.countByUserIdAndStatus(1, "PENDING")).thenReturn(1L);
        when(executionRepository.countByUserIdAndStatus(1, "CANCELLED")).thenReturn(0L);

        ExecutionStatsDto stats = executionService.getExecutionStats(1);

        assertThat(stats.getTotalJobs()).isEqualTo(10L);
        assertThat(stats.getSuccessful()).isEqualTo(7L);
        assertThat(stats.getFailed()).isEqualTo(2L);
        assertThat(stats.getPending()).isEqualTo(1L);
        assertThat(stats.getCancelled()).isZero();
    }
}
