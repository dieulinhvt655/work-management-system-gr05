package com.workmanagement.backend.auth.mapper;

import com.workmanagement.backend.auth.dto.response.AuthUserResponse;
import com.workmanagement.backend.auth.dto.response.LoginResponse;
import com.workmanagement.backend.auth.dto.response.RegisterResponse;
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

    public LoginResponse toLoginResponse(User user, String accessToken, long expiresIn, List<Permission> permissions) {
        RoleResponse roleResponse = user.getRole() != null
                ? roleMapper.toResponse(user.getRole(), permissions)
                : null;

        return LoginResponse.builder()
                .accessToken(accessToken)
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

    public RegisterResponse toRegisterResponse(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .build();
    }

}
