package com.versionservice.dto;

import lombok.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SnapshotResponseDto {

    private  Integer snapshotId;
    private  Integer projectId;
    private  Integer fileId;
    private  Integer authorId;
    private  String message;
    private Integer parentSnapshotId;
    private  String hash;
    private  String branch;
    private  String tag;
    private  LocalDateTime createdAt;
    private  String content;
}