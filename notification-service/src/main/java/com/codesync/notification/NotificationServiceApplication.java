package com.codesync.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * CodeSync Notification Service
 *
 * Consumes Kafka events from Auth and Payment services and
 * dispatches transactional emails to developers/users.
 *
 * Topics consumed:
 *   - codesync.user.login          → user login confirmation mail
 *   - codesync.developer.approved  → developer selection mail
 *   - codesync.payment.notification → payment success/failure mail
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
