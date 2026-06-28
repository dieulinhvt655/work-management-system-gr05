package com.workmanagement.backend.task.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkflowTransitionRequest {

    @NotNull(message = "Trạng thái nguồn không được để trống")
    private Long fromStateId;

    @NotNull(message = "Trạng thái đích không được để trống")
    private Long toStateId;

    private String name;

}
