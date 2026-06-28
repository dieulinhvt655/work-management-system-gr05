package com.workmanagement.backend.security.mapper;

import com.workmanagement.backend.security.dto.response.PermissionResponse;
import com.workmanagement.backend.security.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .module(permission.getModule())
                .description(permission.getDescription())
                .build();
    }

}
