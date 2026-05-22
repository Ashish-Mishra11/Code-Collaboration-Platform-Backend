package com.versionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotRequestDto {

    @NotNull(message = "Project ID is required")
    private Integer projectId;
    @NotNull(message = "File ID is required")
    private Integer fileId;
    @NotNull(message = "Author ID is required")
    private Integer authorId;
    @NotNull(message = "Commit message is required")
    private String message;
    @NotNull(message = "Content cannot be blank")
    private String content;

    private Integer parentSnapshotId;
    @NotBlank(message = "Branch name is required")
    private String branch;

    private String tag;
}
