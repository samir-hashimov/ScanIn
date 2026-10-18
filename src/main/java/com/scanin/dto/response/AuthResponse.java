package com.scanin.dto.response;

import com.scanin.util.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String username;
    private String fullName;
    private String position;
    private Role role;
}