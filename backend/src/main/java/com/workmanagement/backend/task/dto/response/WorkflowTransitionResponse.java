package com.workmanagement.backend.task.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkflowTransitionResponse {

    private Long id;
    private Long projectId;
    private Long fromStateId;
    private String fromStateName;
    private Long toStateId;
    private String toStateName;
    private String name;

}
