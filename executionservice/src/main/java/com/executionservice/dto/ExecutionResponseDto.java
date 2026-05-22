package com.executionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Getter
public class ExecutionResponseDto {
    private final String jobId;
    private final Integer projectId;
    private final Integer fileId;
    private final Integer userId;
    private final String language;
    private final String sourceCode;
    private final String stdin;
    private final String status;
    private final String stdout;
    private final String stderr;
    private final Integer exitCode;
    private final Long executionTimeMs;
    private final Long memoryUsedKb;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;
}
