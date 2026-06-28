package com.workmanagement.backend.user.dto.request;

import com.workmanagement.backend.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Request body khóa/mở khóa tài khoản (UC-1.8). */
@Getter
@Setter
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private UserStatus status;

}
