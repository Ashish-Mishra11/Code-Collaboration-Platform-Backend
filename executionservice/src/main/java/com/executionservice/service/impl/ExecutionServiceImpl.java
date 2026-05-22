package com.executionservice.service.impl;

import com.executionservice.dto.ExecutionResult;
import com.executionservice.dto.ExecutionStatsDto;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.exception.ResourceNotFoundException;
import com.executionservice.repository.ExecutionRepository;
import com.executionservice.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExecutionServiceImpl implements ExecutionService {

    private final ExecutionRepository executionRepository;
    private final DockerService dockerService;
    private final TaskExecutor taskExecutor;

    /**
     * FIXED: Synchronous execution.
     * Runs code immediately, waits for result, saves and returns complete job.
     * This is what the UI uses so it gets output in one request.
     */
    @Override
    @Transactional
    public ExecutionJob runSynchronously(ExecutionJob executionJob) {
        executionJob.setStatus("RUNNING");
        executionJob.setCreatedAt(LocalDateTime.now());
        executionJob.setJobId(UUID.randomUUID().toString());
        executionJob.setCompletedAt(null);
        executionJob.setStderr("");
        executionJob.setStdout("");
        if (executionJob.getStdin() == null) executionJob.setStdin("");

        ExecutionJob saved = executionRepository.saveAndFlush(executionJob);
        log.info("Sync execution start: jobId={} lang={} user={}", saved.getJobId(), saved.getLanguage(), saved.getUserId());

        try {
            ExecutionResult result = dockerService.runCode(saved);

            saved.setStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
            saved.setStdout(result.getStdout() != null ? result.getStdout() : "");
            saved.setStderr(result.getStderr() != null ? result.getStderr() : "");
            saved.setExitCode(result.getExitCode());
            saved.setExecutionTimeMs(result.getExecutionTimeMs());
            saved.setErrorMessage(result.getErrorMessage());
            saved.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            saved.setStatus("FAILED");
            saved.setStderr(e.getMessage() != null ? e.getMessage() : "Unknown error");
            saved.setErrorMessage(e.getMessage());
            saved.setCompletedAt(LocalDateTime.now());
            log.error("Sync execution failed for job {}", saved.getJobId(), e);
        }

        ExecutionJob completed = executionRepository.saveAndFlush(saved);
        log.info("Sync execution done: jobId={} status={} timeMs={}", completed.getJobId(), completed.getStatus(), completed.getExecutionTimeMs());
        return completed;
    }

    @Override
    @Transactional
    public ExecutionJob submitExecution(ExecutionJob executionJob) {
        executionJob.setStatus("PENDING");
        executionJob.setCreatedAt(LocalDateTime.now());
        executionJob.setJobId(UUID.randomUUID().toString());
        executionJob.setCompletedAt(null);
        executionJob.setStderr("");
        executionJob.setStdout("");
        if (executionJob.getStdin() == null) executionJob.setStdin("");

        ExecutionJob saved = executionRepository.saveAndFlush(executionJob);
        log.info("Submitted async execution job ID: {} for user: {} language: {}", saved.getJobId(), saved.getUserId(), saved.getLanguage());

        String savedJobId = saved.getJobId();
        taskExecutor.execute(() -> executeJobAsync(savedJobId));
        return saved;
    }

    public void executeJobAsync(String jobId) {
        ExecutionJob job = executionRepository.findByJobId(jobId).orElse(null);
        if (job == null) {
            log.error("Async execution: job {} not found in DB", jobId);
            return;
        }
        try {
            job.setStatus("RUNNING");
            executionRepository.saveAndFlush(job);

            ExecutionResult result = dockerService.runCode(job);

            job.setStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
            job.setStdout(result.getStdout() != null ? result.getStdout() : "");
            job.setStderr(result.getStderr() != null ? result.getStderr() : "");
            job.setExitCode(result.getExitCode());
            job.setExecutionTimeMs(result.getExecutionTimeMs());
            job.setErrorMessage(result.getErrorMessage());
            job.setCompletedAt(LocalDateTime.now());
            executionRepository.saveAndFlush(job);
            log.info("Completed async execution job: {} status: {}", jobId, job.getStatus());
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            executionRepository.saveAndFlush(job);
            log.error("Async execution failed for job {}", jobId, e);
        }
    }

    @Override
    @Transactional
    public void cancelExecution(String jobId) {
        ExecutionJob job = executionRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution job not found: " + jobId));
        if ("CANCELLED".equals(job.getStatus())) throw new IllegalArgumentException("Job already cancelled");
        if ("RUNNING".equals(job.getStatus()) && job.getContainerId() != null) {
            dockerService.killContainer(job.getContainerId());
        }
        job.setStatus("CANCELLED");
        job.setCompletedAt(LocalDateTime.now());
        executionRepository.save(job);
        log.info("Cancelled execution job: {}", jobId);
    }

    @Override @Transactional(readOnly = true)
    public Optional<ExecutionJob> getJobById(String jobId) { return executionRepository.findByJobId(jobId); }

    @Override @Transactional(readOnly = true)
    public List<ExecutionJob> getExecutionsByUser(int userId) { return executionRepository.findByUserId(userId); }

    @Override @Transactional(readOnly = true)
    public List<ExecutionJob> getExecutionsByProject(int projectId) { return executionRepository.findByProjectId(projectId); }

    @Override @Transactional(readOnly = true)
    public ExecutionJob getExecutionResult(String jobId) {
        return executionRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution job not found: " + jobId));
    }

    @Override
    public List<String> getSupportedLanguages() {
        return List.of("Java", "Python", "JavaScript", "C++", "Go", "Rust");
    }

    @Override
    public String getLanguageVersion(String language) {
        Map<String, String> versions = new HashMap<>();
        versions.put("Java", "21"); versions.put("Python", "3.12");
        versions.put("JavaScript", "Node 20"); versions.put("C++", "GCC 13");
        versions.put("Go", "1.22"); versions.put("Rust", "1.77");
        return versions.getOrDefault(language, "Unknown");
    }

    @Override @Transactional(readOnly = true)
    public ExecutionStatsDto getExecutionStats(int userId) {
        long totalJobs  = executionRepository.countByUserId(userId);
        long successful = executionRepository.countByUserIdAndStatus(userId, "SUCCESS");
        long failed     = executionRepository.countByUserIdAndStatus(userId, "FAILED");
        long pending    = executionRepository.countByUserIdAndStatus(userId, "PENDING");
        long cancelled  = executionRepository.countByUserIdAndStatus(userId, "CANCELLED");
        log.info("Fetched execution stats for user: {}", userId);
        return new ExecutionStatsDto(totalJobs, successful, failed, pending, cancelled);
    }
}
