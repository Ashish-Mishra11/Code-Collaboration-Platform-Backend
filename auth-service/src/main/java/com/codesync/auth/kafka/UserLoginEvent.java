package com.codesync.auth.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka event published when a user successfully logs in.
 * Topic: codesync.user.login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLoginEvent {

    private Integer userId;
    private String userName;
    private String email;
    private String fullName;

    @Builder.Default
    private LocalDateTime loginTime = LocalDateTime.now();

    private String ipAddress;
}
