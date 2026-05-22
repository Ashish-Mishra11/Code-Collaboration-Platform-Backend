package com.codesync.file.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CodeFileResponse {
    private Integer fileId;
    private Integer projectId;
    private String name;
    private String path;
    private String language;
    private String content;
    private Long size;
    private Integer createdById;
    private Integer lastEditedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}
