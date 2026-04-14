package com.codesync.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateContentRequest {

    @NotBlank
    private String content;

    @NotNull
    private Integer userId;
}