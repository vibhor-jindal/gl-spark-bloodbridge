package com.globallogic.bloodbridge.auth.dto;

import com.globallogic.bloodbridge.auth.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private String token;
    private long expiresInSeconds;
}
