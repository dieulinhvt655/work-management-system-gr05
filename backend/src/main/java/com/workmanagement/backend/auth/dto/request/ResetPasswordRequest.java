package com.workmanagement.backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request body đặt lại mật khẩu bằng reset token (UC-1.3). */
@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token không được để trống")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới tối thiểu 6 ký tự")
    private String newPassword;

}
