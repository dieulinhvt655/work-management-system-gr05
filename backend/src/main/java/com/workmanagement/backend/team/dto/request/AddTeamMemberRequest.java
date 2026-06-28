package com.workmanagement.backend.team.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTeamMemberRequest {

    @NotNull(message = "workspaceMemberId không được để trống")
    private Long workspaceMemberId;

    @NotNull(message = "roleId không được để trống")
    private Long roleId;

}
