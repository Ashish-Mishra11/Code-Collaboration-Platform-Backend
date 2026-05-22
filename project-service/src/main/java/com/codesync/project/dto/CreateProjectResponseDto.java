package com.codesync.project.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CreateProjectResponseDto {

    private Long  projectId;
    private String name;
    private String description;
    private Long ownerId;
    private String visibility;
    private String language;
    private boolean archived;
}