package com.workmanagement.backend.task.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkflowStateRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String code;

    private Integer position;

    private Boolean isDefault;

    private Boolean isFinal;

}
