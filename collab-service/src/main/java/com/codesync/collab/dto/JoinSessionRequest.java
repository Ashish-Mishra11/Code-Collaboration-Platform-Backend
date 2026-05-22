package com.codesync.collab.dto;

import lombok.Data;

@Data
public class JoinSessionRequest {
    private String role;           // e.g. "HOST", "EDITOR", "VIEWER"
    private String password;       // optional, only if session is password protected
}