package com.workmanagement.backend.security.dto.request;

import com.workmanagement.backend.common.enums.RoleScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateRoleRequest {

    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Scope không được để trống")
    private RoleScope scope;

    private List<Long> permissionIds;

}
