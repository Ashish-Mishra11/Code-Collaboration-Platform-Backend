package com.codesync.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterUserDto {

	@NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^[a-zA-Z]{2,20}$",
            message = "Invalid username format"
        )
    private String userName;
    private String email;
    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
            message = "Password must have at least 8 characters, 1 uppercase, 1 lowercase, 1 number, and 1 special character"
        )
    private String password;
    private String fullName;
}
