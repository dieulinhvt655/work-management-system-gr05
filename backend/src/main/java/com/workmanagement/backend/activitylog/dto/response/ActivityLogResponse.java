package com.workmanagement.backend.activitylog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActivityLogResponse {

    private Long id;
    private Long actorUserId;
    private String actorName;
    private Long projectId;
    private String action;
    private String targetType;
    private Long targetId;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

}
