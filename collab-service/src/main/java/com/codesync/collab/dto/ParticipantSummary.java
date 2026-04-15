package com.codesync.collab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantSummary {
    private Integer userId;
    private String role;
    private String color;
    private Integer cursorLine;
    private Integer cursorCol;
    private LocalDateTime joinedAt;
}