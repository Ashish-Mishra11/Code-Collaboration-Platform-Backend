package com.codesync.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateFolderRequest {

    @NotNull
    private Integer projectId;

    @NotBlank
    private String folderName;

    @NotBlank
    private String path;

    @NotNull
    private Integer createdById;
}