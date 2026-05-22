package com.executionservice.mapper;

import com.executionservice.dto.ExecutionRequestDto;
import com.executionservice.dto.ExecutionResponseDto;
import com.executionservice.dto.ExecutionStatsDto;
import com.executionservice.entity.ExecutionJob;

import java.util.List;

public class ExecutionMapper {
    //request DTO -> Entity
    public static ExecutionJob toEntity(ExecutionRequestDto dto ){
        return ExecutionJob.builder()
                .projectId(dto.getProjectId())
                .fileId(dto.getFileId())
                .userId(dto.getUserId())
                .language(dto.getLanguage())
                .sourceCode(dto.getSourceCode())
                .stdin(dto.getStdin())
                .build();
    }

    // entity->  response dto
    public static ExecutionResponseDto toResponse(ExecutionJob job){
        return ExecutionResponseDto
                .builder()
                .jobId(job.getJobId())
                .projectId(job.getProjectId())
                .fileId(job.getFileId())
                .userId(job.getUserId())
                .language(job.getLanguage())
                .sourceCode(job.getSourceCode())
                .stdin(job.getStdin())
                .status(job.getStatus())
                .stdout(job.getStdout())
                .stderr(job.getStderr())
                .exitCode(job.getExitCode())
                .executionTimeMs(job.getExecutionTimeMs())
                .memoryUsedKb(job.getMemoryUsedKb())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    //stats
    public static ExecutionStatsDto toStatsDto(List<ExecutionJob> jobs, int totalCount){
        long success = jobs.stream().filter(j -> "SUCCESS".equals(j.getStatus())).count();
        long failed = jobs.stream().filter(j -> "FAILED".equals(j.getStatus())).count();
        long pending = jobs.stream().filter(j -> "PENDING".equals(j.getStatus())).count();
        long cancelled = jobs.stream().filter(j -> "CANCELLED".equals(j.getStatus())).count();

        return ExecutionStatsDto
                .builder()
                .totalJobs(jobs.size())
                .successful(success)
                .failed(failed)
                .pending(pending)
                .cancelled(cancelled)
                .build();
    }

}
