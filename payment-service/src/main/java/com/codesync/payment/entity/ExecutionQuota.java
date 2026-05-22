package com.codesync.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks execution quota per (userId, projectId, fileId).
 * Free tier: 50 executions. Additional tokens purchased via payment.
 */
@Entity
@Table(name = "execution_quota",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id", "file_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExecutionQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "file_id", nullable = false)
    private Integer fileId;

    /** Total executions consumed so far. */
    @Column(nullable = false)
    @Builder.Default
    private Integer executionsUsed = 0;

    /** Free executions per file (default 50). */
    @Column(nullable = false)
    @Builder.Default
    private Integer freeLimit = 50;

    /** Additional paid tokens. Each token = 1 execution. */
    @Column(nullable = false)
    @Builder.Default
    private Integer purchasedTokens = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Returns total allowed executions (free + purchased). */
    public int totalAllowed() {
        return freeLimit + purchasedTokens;
    }

    /** Returns remaining executions. */
    public int remaining() {
        return Math.max(0, totalAllowed() - executionsUsed);
    }

    /** Returns true if the user can still run code. */
    public boolean canExecute() {
        return remaining() > 0;
    }
}
