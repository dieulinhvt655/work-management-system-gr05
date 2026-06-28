package com.workmanagement.backend.security.mapper;

import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final PermissionMapper permissionMapper;

    public RoleResponse toResponse(Role role, List<Permission> permissions) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .scope(role.getScope())
                .permissions(permissions.stream().map(permissionMapper::toResponse).toList())
                .build();
    }

}
