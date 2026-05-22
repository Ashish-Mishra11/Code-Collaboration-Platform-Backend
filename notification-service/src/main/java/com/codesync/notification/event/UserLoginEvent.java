package com.codesync.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published by Auth-Service when a user successfully logs in.
 * Topic: codesync.user.login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLoginEvent {

    /** Database user ID */
    private Integer userId;

    /** Unique username */
    private String userName;

    /** Email address to send notification to */
    private String email;

    /** Full display name */
    private String fullName;

    /** Time of login (ISO-8601) */
    @Builder.Default
    private LocalDateTime loginTime = LocalDateTime.now();

    /** IP address or "UNKNOWN" */
    private String ipAddress;
}
