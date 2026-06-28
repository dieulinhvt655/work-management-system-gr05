package com.workmanagement.backend.project.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddProjectMemberRequest {

    @NotNull(message = "teamMemberId không được để trống")
    private Long teamMemberId;

    @NotNull(message = "roleId không được để trống")
    private Long roleId;

}
