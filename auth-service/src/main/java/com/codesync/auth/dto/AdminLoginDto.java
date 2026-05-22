package com.codesync.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
