package com.workmanagement.backend.dashboard.dto.response;

import com.workmanagement.backend.common.enums.SprintStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SprintDashboardResponse {

    private Long sprintId;
    private String sprintName;
    private SprintStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalTasks;
    private long doneTasks;
    private int completionPercent;
    private TaskStatusBreakdownResponse taskBreakdown;

}
