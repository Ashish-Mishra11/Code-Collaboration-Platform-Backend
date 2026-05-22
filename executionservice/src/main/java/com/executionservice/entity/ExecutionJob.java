package com.executionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "execution_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionJob {

    @Id
    @Column(name = "job_id", length = 36)
    private String jobId;

    @Column(nullable = false)
    private Integer projectId;

    @Column(nullable = false)
    private Integer fileId;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String stdin = "";

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String stdout = "";

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String stderr = "";

    private Integer exitCode;
    private Long executionTimeMs;
    private Long memoryUsedKb;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
    private String containerId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    public void prePersist() {
        if (this.jobId == null) {
            this.jobId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
