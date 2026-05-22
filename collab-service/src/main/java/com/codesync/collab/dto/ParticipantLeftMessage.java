package com.codesync.collab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantLeftMessage {
    private String sessionId;
    private Integer userId;
    private String eventType = "participant_left";
    private int currentParticipantCount;
}