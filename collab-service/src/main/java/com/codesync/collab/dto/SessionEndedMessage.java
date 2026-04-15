package com.codesync.collab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEndedMessage {

    private String sessionId;
    private Integer endedByUserId;
    private String eventType = "session_ended";
    private String message = "The collaboration session has been ended by the host.";
}