package com.workmanagement.backend.project.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddProjectMemberRequest {

    @NotNull(message = "teamMemberId không được để trống")
    @Positive(message = "teamMemberId không hợp lệ")
    private Long teamMemberId;

    @NotNull(message = "roleId không được để trống")
    @Positive(message = "roleId không hợp lệ")
    private Long roleId;

}
