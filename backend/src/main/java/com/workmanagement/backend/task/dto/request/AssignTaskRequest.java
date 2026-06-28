package com.workmanagement.backend.task.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTaskRequest {

    @NotNull(message = "Thành viên được gán không được để trống")
    private Long assigneeMemberId;

    private Long reviewerMemberId;

}
