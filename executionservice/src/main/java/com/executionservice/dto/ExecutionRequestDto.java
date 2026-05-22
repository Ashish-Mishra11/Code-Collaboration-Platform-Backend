package com.executionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class ExecutionRequestDto {

    @NotNull(message = "Project ID is required")
    private Integer projectId;

    @NotNull(message = "File Id is required")
    private Integer fileId;

    @NotNull(message = "User id required")
    private Integer userId;

    @NotNull(message = "Language is required")
    private String language;

    @NotNull(message = "scource code is required")
    private String sourceCode;

    private String stdin;

}
