package com.codesync.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProjectDto {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;


    @NotBlank(message = "Visibility is required")
    private String visibility; // PUBLIC or PRIVATE

    @NotBlank(message = "Language is required")
    private String language;
}