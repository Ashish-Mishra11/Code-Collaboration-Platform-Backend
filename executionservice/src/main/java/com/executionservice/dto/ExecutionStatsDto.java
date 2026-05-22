package com.executionservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecutionStatsDto {
    private long totalJobs;
    private long successful;
    private long failed;
    private long pending;
    private long cancelled;
}
