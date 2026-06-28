package com.workmanagement.backend.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePermissionRequest {

    @NotBlank(message = "Mã quyền không được để trống")
    private String code;

    @NotBlank(message = "Tên quyền không được để trống")
    private String name;

    @NotBlank(message = "Module không được để trống")
    private String module;

    private String description;

}
