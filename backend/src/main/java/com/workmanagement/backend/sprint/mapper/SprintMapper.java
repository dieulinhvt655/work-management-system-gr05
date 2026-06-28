package com.workmanagement.backend.sprint.mapper;

import com.workmanagement.backend.sprint.dto.response.SprintResponse;
import com.workmanagement.backend.sprint.entity.Sprint;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {

    public SprintResponse toResponse(Sprint sprint, long pbiCount, long taskCount) {
        return SprintResponse.builder()
                .id(sprint.getId())
                .projectId(sprint.getProject().getId())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(sprint.getStatus())
                .summary(sprint.getSummary())
                .pbiCount(pbiCount)
                .taskCount(taskCount)
                .createdAt(sprint.getCreatedAt())
                .updatedAt(sprint.getUpdatedAt())
                .completedAt(sprint.getCompletedAt())
                .build();
    }

}
