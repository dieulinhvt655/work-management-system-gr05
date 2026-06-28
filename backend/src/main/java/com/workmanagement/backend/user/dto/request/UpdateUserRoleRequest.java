package com.workmanagement.backend.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Request body cập nhật vai trò hệ thống của người dùng (UC-1.9). */
@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull(message = "Vai trò không được để trống")
    private Long roleId;

}
