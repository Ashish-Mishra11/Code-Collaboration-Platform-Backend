package com.codesync.auth.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka event published when admin approves a developer application.
 * Topic: codesync.developer.approved
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeveloperApprovedEvent {

    private String name;
    private String email;
    private String companyName;
    private String registrationLink;

    @Builder.Default
    private LocalDateTime approvedAt = LocalDateTime.now();
}
