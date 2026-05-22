package com.executionservice.service;

import com.executionservice.dto.ExecutionStatsDto;
import com.executionservice.entity.ExecutionJob;

import java.util.List;
import java.util.Optional;

public interface ExecutionService {

    /** Async submission - returns immediately with PENDING status */
    ExecutionJob submitExecution(ExecutionJob executionJob);

    /** Synchronous execution - waits for result, returns complete job with output */
    ExecutionJob runSynchronously(ExecutionJob executionJob);

    void cancelExecution(String jobId);
    Optional<ExecutionJob> getJobById(String jobId);
    List<ExecutionJob> getExecutionsByUser(int userId);
    List<ExecutionJob> getExecutionsByProject(int projectId);
    ExecutionJob getExecutionResult(String jobId);
    List<String> getSupportedLanguages();
    String getLanguageVersion(String language);
    ExecutionStatsDto getExecutionStats(int userId);
}
