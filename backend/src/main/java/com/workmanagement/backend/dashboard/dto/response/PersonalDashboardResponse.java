package com.workmanagement.backend.dashboard.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PersonalDashboardResponse {

    private Long projectId;
    private String projectName;
    private long totalAssignedTasks;
    private TaskStatusBreakdownResponse taskBreakdown;
    private List<PersonalTaskItemResponse> upcomingTasks;
    private List<PersonalTaskItemResponse> inProgressTasks;

}
