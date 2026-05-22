package com.executionservice.dto;

import lombok.Data;

@Data
public class ExecutionRequest {
    private Integer projectId;
    private Integer fileId;
    private Integer userId;
    private String language;
    private String sourceCode;
    private String stdin;
}