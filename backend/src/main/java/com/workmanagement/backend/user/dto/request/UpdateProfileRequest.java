package com.workmanagement.backend.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request body cập nhật hồ sơ cá nhân (UC-1.4). */
@Getter
@Setter
public class UpdateProfileRequest {

    /** Chỉ giữ để tương thích request cũ; hồ sơ cá nhân không được phép tự đổi họ tên. */
    private String fullName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Size(max = 500, message = "Avatar URL tối đa 500 ký tự")
    private String avatarUrl;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    private String currentPassword;

    private String newPassword;

    private String confirmNewPassword;

}
