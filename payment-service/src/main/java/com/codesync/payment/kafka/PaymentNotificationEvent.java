package com.codesync.payment.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event published by Payment-Service after every token purchase attempt.
 * Topic: codesync.payment.notification
 *
 * status: "SUCCESS" | "FAILED"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentNotificationEvent {

    private Integer userId;
    private String email;
    private String userName;
    private Integer tokensPurchased;
    private BigDecimal amount;
    private String gatewayReference;

    /** SUCCESS or FAILED */
    private String status;

    private String remarks;

    @Builder.Default
    private LocalDateTime transactionTime = LocalDateTime.now();
}
