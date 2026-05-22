package com.codesync.collab.dto;
// dto/CreateCollabSessionRequest.java

import lombok.Data;

@Data

public class CreateCollabSessionRequest {
    private Integer projectId;
    private Integer fileId;
    private String language;           
    private Integer maxParticipants;
    private Boolean isPasswordProtected;
    private String sessionPassword;   
}