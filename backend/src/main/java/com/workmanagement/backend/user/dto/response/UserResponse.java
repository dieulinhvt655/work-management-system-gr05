package com.workmanagement.backend.user.dto.response;

import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String username;
    private String phone;
    private String avatarUrl;
    private String employeeCode;
    private String description;
    private UserStatus status;
    private RoleResponse role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
