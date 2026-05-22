package com.codesync.file.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateFolderResponse {
    private Integer fileId;
    private Integer projectId;
    private String folderName;
    private String path;
    private String language;
    private Integer createdById;
}
