package com.codesync.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health and info endpoint for the Notification Service.
 * Useful for API Gateway routing and monitoring.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Service", description = "Health and status endpoints")
public class NotificationController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service status and timestamp")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "notification-service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "topics", new String[]{
                        "codesync.user.login",
                        "codesync.developer.approved",
                        "codesync.payment.notification"
                }
        ));
    }
}
