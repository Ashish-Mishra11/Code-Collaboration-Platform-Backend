package com.versionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DiffResponseDto {

    private final Integer snapshotId1;
    private final Integer snapshotId2;
    private final String diffResult;
}
