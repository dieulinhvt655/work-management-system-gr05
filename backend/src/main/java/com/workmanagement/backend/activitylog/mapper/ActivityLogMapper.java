package com.workmanagement.backend.activitylog.mapper;

import com.workmanagement.backend.activitylog.dto.response.ActivityLogResponse;
import com.workmanagement.backend.activitylog.entity.ActivityLog;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogMapper {

    public ActivityLogResponse toResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .actorUserId(log.getActor().getId())
                .actorName(log.getActor().getFullName())
                .projectId(log.getProject() != null ? log.getProject().getId() : null)
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }

}
