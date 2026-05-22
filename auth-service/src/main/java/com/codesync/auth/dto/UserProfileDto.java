package com.codesync.auth.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserProfileDto {

    private Integer userId;
    private String userName;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createAt;
}
