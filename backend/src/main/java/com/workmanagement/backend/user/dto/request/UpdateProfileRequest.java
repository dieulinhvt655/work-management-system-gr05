package com.workmanagement.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request body cập nhật hồ sơ cá nhân (UC-1.4). */
@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Size(max = 500, message = "Avatar URL tối đa 500 ký tự")
    private String avatarUrl;

    private String currentPassword;

    @Size(min = 6, message = "Mật khẩu mới tối thiểu 6 ký tự")
    private String newPassword;

}
