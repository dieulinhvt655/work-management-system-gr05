package com.workmanagement.backend.workspace.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddWorkspaceMemberRequest {

    @NotNull(message = "userId không được để trống")
    @Positive(message = "userId không hợp lệ")
    private Long userId;

    @NotNull(message = "roleId không được để trống")
    @Positive(message = "roleId không hợp lệ")
    private Long roleId;

}
