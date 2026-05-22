package com.codesync.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records each payment transaction made by a developer.
 */
@Entity
@Table(name = "payment_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "file_id", nullable = false)
    private Integer fileId;

    /** Tokens purchased in this transaction. */
    @Column(nullable = false)
    private Integer tokensPurchased;

    /** Amount charged (in INR or configured currency). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Payment gateway reference ID (e.g., Razorpay order ID / Stripe charge ID). */
    @Column(name = "gateway_reference")
    private String gatewayReference;

    /** PENDING | SUCCESS | FAILED */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
