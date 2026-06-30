package com.workmanagement.backend.workspace.dto.request;

import com.workmanagement.backend.common.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkspaceMemberRequest {

    @NotNull(message = "roleId không được để trống")
    @Positive(message = "roleId không hợp lệ")
    private Long roleId;

    private MemberStatus status;

}
