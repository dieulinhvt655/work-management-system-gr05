package com.workmanagement.backend.user.dto.response;

import com.workmanagement.backend.security.dto.response.RoleResponse;
import lombok.Builder;
import lombok.Getter;

/** Vai trò gắn với người dùng (UC-1.9). */
@Getter
@Builder
public class UserRoleResponse {

    private Long userId;
    private Long workspaceId;
    private RoleResponse role;

}
