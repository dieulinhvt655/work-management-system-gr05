package com.workmanagement.backend.project.dto.request;

import com.workmanagement.backend.common.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectMemberRequest {

    @NotNull(message = "roleId không được để trống")
    private Long roleId;

    private MemberStatus status;

}
