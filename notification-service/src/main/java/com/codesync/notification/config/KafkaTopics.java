package com.codesync.notification.config;

/**
 * Central registry of all Kafka topic names used across the CodeSync platform.
 * Import this class in both producers (Auth/Payment) and the consumer (Notification).
 */
public final class KafkaTopics {

    private KafkaTopics() { /* utility class */ }

    /** Published by Auth-Service when a user logs in successfully */
    public static final String USER_LOGIN = "codesync.user.login";

    /** Published by Auth-Service when admin approves a developer application */
    public static final String DEVELOPER_APPROVED = "codesync.developer.approved";

    /** Published by Payment-Service after a token purchase (success or failure) */
    public static final String PAYMENT_NOTIFICATION = "codesync.payment.notification";
}
