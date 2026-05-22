package com.codesync.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaboratorDto {
    private Integer id;
    private Integer projectId;
    private Integer userId;
    private String role;
    private LocalDateTime addedAt;
}
