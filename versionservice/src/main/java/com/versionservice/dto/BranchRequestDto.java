package com.versionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BranchRequestDto {
    @NotNull(message = "Project Id is required")
    private Integer projectId;

    @NotBlank(message = "Branch name is required")
    private String branchName;
}
