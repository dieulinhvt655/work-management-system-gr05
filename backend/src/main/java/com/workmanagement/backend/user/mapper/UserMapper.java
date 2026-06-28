package com.workmanagement.backend.user.mapper;

import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.mapper.RoleMapper;
import com.workmanagement.backend.user.dto.response.UserResponse;
import com.workmanagement.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final RoleMapper roleMapper;

    public UserResponse toResponse(User user) {
        return toResponse(user, List.of());
    }

    public UserResponse toResponse(User user, List<Permission> permissions) {
        Role role = user.getRole();
        RoleResponse roleResponse = role != null
                ? roleMapper.toResponse(role, permissions)
                : null;

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(roleResponse)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

}
