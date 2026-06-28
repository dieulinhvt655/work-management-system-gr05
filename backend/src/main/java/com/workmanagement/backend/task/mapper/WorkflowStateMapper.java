package com.workmanagement.backend.task.mapper;

import com.workmanagement.backend.task.dto.response.WorkflowStateResponse;
import com.workmanagement.backend.task.entity.WorkflowState;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStateMapper {

    public WorkflowStateResponse toResponse(WorkflowState state) {
        return WorkflowStateResponse.builder()
                .id(state.getId())
                .projectId(state.getProject().getId())
                .name(state.getName())
                .code(state.getCode())
                .position(state.getPosition())
                .isDefault(state.getIsDefault())
                .isFinal(state.getIsFinal())
                .build();
    }

}
