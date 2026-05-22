package com.codesync.collab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantJoinedMessage {
    private String sessionId;
    private ParticipantSummary participant;
    private int currentParticipantCount;
    private String eventType = "participant_joined";
}