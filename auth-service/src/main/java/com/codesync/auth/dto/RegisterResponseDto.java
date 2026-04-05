package com.codesync.auth.dto;

import lombok.Data;

@Data
public class RegisterResponseDto {

    private Integer userId;
    private String userName;
    private String email;
    private String role;
}
