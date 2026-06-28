package com.workmanagement.backend.task.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkflowStateResponse {

    private Long id;
    private Long projectId;
    private String name;
    private String code;
    private Integer position;
    private Boolean isDefault;
    private Boolean isFinal;

}
