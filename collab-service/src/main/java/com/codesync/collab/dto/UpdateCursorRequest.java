package com.codesync.collab.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCursorRequest {

    @NotNull
    private Integer userId;

    @NotNull
    private Integer cursorLine;

    @NotNull
    private Integer cursorCol;
}