package com.workmanagement.backend.workspace.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddWorkspaceMemberRequest {

    @NotNull(message = "userId không được để trống")
    private Long userId;

    @NotNull(message = "roleId không được để trống")
    private Long roleId;

}
