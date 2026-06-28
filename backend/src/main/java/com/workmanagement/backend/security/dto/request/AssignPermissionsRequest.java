package com.workmanagement.backend.security.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Request body gán permission cho vai trò (UC-1.9). */
@Getter
@Setter
public class AssignPermissionsRequest {

    @NotEmpty(message = "Danh sách permission không được rỗng")
    private List<Long> permissionIds;

}
