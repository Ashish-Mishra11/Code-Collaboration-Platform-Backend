package com.codesync.collab.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CreateCollabSessionResponse {

    private String sessionId;
    private Integer projectId;
    private Integer fileId;

    private String language;
    private Integer maxParticipants;

    private Boolean isPasswordProtected;

    private Integer activeEditorUserId;

    private LocalDateTime createdAt;
}