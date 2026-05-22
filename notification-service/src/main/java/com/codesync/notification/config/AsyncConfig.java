package com.codesync.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's @Async support so email sending does not block
 * the Kafka listener thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
