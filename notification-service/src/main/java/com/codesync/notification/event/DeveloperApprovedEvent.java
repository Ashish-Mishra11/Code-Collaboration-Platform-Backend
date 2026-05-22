package com.codesync.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published by Auth-Service when admin approves a developer application.
 * Topic: codesync.developer.approved
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeveloperApprovedEvent {

    /** Applicant's full name */
    private String name;

    /** Applicant's email (used as recipient) */
    private String email;

    /** Company the developer applied from */
    private String companyName;

    /** Pre-built registration link for the developer */
    private String registrationLink;

    /** Timestamp of approval */
    @Builder.Default
    private LocalDateTime approvedAt = LocalDateTime.now();
}
