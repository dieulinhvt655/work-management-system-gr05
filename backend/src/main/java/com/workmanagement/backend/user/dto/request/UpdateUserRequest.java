package com.workmanagement.backend.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request body cập nhật thông tin tài khoản bởi admin (UC-1.7). */
@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(min = 3, max = 100, message = "Username từ 3–100 ký tự")
    private String username;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Size(max = 500, message = "Avatar URL tối đa 500 ký tự")
    private String avatarUrl;

}
