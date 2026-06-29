package com.workmanagement.backend.auth.mapper;

import com.workmanagement.backend.auth.dto.response.AuthUserResponse;
import com.workmanagement.backend.auth.dto.response.LoginResponse;
import com.workmanagement.backend.auth.dto.response.TokenResponse;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.mapper.RoleMapper;
import com.workmanagement.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final RoleMapper roleMapper;

    public LoginResponse toLoginResponse(
            User user,
            String accessToken,
            String refreshToken,
            long expiresIn,
            List<Permission> permissions
    ) {
        RoleResponse roleResponse = user.getRole() != null
                ? roleMapper.toResponse(user.getRole(), permissions)
                : null;

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(AuthUserResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(roleResponse)
                        .build())
                .build();
    }

    public TokenResponse toTokenResponse(String accessToken, String refreshToken, long expiresIn) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
