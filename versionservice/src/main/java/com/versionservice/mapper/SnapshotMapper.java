package com.versionservice.mapper;

import com.versionservice.dto.SnapshotRequestDto;
import com.versionservice.dto.SnapshotResponseDto;
import com.versionservice.entity.Snapshot;

public class SnapshotMapper {

    // Request DTO → Entity
    public static Snapshot toEntity(SnapshotRequestDto dto) {
        if(dto == null) return null;

        return Snapshot.builder()
                .projectId(dto.getProjectId())
                .fileId(dto.getFileId())
                .authorId(dto.getAuthorId())
                .message(dto.getMessage())
                .content(dto.getContent())
                .parentSnapshotId(dto.getParentSnapshotId())
                .branch(dto.getBranch() != null ? dto.getBranch() : "main")
                .tag(dto.getTag())
                .build();
    }

    // Entity → Response DTO
    public static SnapshotResponseDto toResponse(Snapshot s) {
        if(s == null ) return null;

        return SnapshotResponseDto.builder()
                .snapshotId(s.getSnapshotId())
                .projectId(s.getProjectId())
                .fileId(s.getFileId())
                .authorId(s.getAuthorId())
                .message(s.getMessage())
                .content(s.getContent())
                .hash(s.getHash())
                .parentSnapshotId(s.getParentSnapshotId())
                .branch(s.getBranch())
                .tag(s.getTag())
                .createdAt(s.getCreatedAt())
                .build();
    }
}