package com.workmanagement.backend.user.dto.request;

import com.workmanagement.backend.common.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request body tạo tài khoản người dùng (UC-1.5). */
@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    /** Username tùy chọn; nếu bỏ trống sẽ lấy từ mã nhân viên do hệ thống tự sinh. */
    @Size(min = 3, max = 100, message = "Username từ 3–100 ký tự")
    private String username;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @NotNull(message = "Workspace không được để trống")
    private Long workspaceId;

    /** Team trong workspace (tùy chọn). */
    private Long teamId;

    /** Khóa vai trò FE (SYSTEM_ADMIN, WORKSPACE_OWNER, …). */
    private String role;

    /** Id vai trò; ưu tiên hơn role nếu cả hai được gửi. */
    private Long roleId;

    private UserStatus status;

}
