package com.workmanagement.backend.task.dto.response;

import com.workmanagement.backend.common.enums.PriorityLevel;
import com.workmanagement.backend.common.enums.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {

    private Long id;
    private Long pbiId;
    private String pbiTitle;
    private Long sprintId;
    private Long parentTaskId;
    private String title;
    private String description;
    private PriorityLevel priority;
    private TaskStatus status;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate deadline;
    private LocalDateTime completedAt;
    private Long assigneeMemberId;
    private String assigneeName;
    private Long reporterMemberId;
    private String reporterName;
    private Long reviewerMemberId;
    private String reviewerName;
    private Long workflowStateId;
    private String workflowStateName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
