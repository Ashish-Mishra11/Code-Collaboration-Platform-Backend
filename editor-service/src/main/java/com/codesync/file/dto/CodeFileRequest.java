package com.codesync.file.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodeFileRequest {

    @NotNull
    private Integer projectId;

    @NotBlank
    private String name;

    @NotBlank
    private String path;

    private String language;

    private String content = "";

    @NotNull
    private Integer createdById;
}