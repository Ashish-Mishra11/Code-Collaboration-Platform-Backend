package com.codesync.auth.dto;

import lombok.Data;

@Data
public class RegisterUserDto {

    private String userName;
    private String email;
    private String password;
    private String fullName;
}
