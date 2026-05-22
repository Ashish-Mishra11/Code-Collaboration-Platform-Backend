package com.versionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RestoreResponseDto {

    private final Integer restoredFromSnapshotId;
    private final String content;
    private final String message;

}
