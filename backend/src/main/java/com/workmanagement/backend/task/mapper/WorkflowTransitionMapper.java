package com.workmanagement.backend.task.mapper;

import com.workmanagement.backend.task.dto.response.WorkflowTransitionResponse;
import com.workmanagement.backend.task.entity.WorkflowTransition;
import org.springframework.stereotype.Component;

@Component
public class WorkflowTransitionMapper {

    public WorkflowTransitionResponse toResponse(WorkflowTransition transition) {
        return WorkflowTransitionResponse.builder()
                .id(transition.getId())
                .projectId(transition.getProject().getId())
                .fromStateId(transition.getFromState().getId())
                .fromStateName(transition.getFromState().getName())
                .toStateId(transition.getToState().getId())
                .toStateName(transition.getToState().getName())
                .name(transition.getName())
                .build();
    }

}
