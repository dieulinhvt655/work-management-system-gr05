package com.workmanagement.backend.task.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkflowTransitionRequest {

    private Long fromStateId;

    private Long toStateId;

    private String name;

}
