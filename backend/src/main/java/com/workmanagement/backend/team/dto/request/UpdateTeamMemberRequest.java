package com.workmanagement.backend.team.dto.request;

import com.workmanagement.backend.common.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeamMemberRequest {

    @NotNull(message = "roleId không được để trống")
    private Long roleId;

    private MemberStatus status;

}
