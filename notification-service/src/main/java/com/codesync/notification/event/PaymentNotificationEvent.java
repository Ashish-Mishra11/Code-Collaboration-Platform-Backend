package com.codesync.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published by Payment-Service after a token purchase attempt.
 * Topic: codesync.payment.notification
 *
 * status values: "SUCCESS" | "FAILED"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentNotificationEvent {

    /** Developer's user ID */
    private Integer userId;

    /** Developer's email */
    private String email;

    /** Developer's display name */
    private String userName;

    /** Number of tokens purchased */
    private Integer tokensPurchased;

    /** Amount charged */
    private BigDecimal amount;

    /** Gateway reference (Razorpay order ID) */
    private String gatewayReference;

    /** SUCCESS or FAILED */
    private String status;

    /** Descriptive remark (e.g., error message) */
    private String remarks;

    /** When the transaction was processed */
    @Builder.Default
    private LocalDateTime transactionTime = LocalDateTime.now();
}
