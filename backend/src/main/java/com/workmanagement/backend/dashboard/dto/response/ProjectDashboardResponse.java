package com.workmanagement.backend.dashboard.dto.response;

import com.workmanagement.backend.common.enums.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProjectDashboardResponse {

    private Long projectId;
    private String projectName;
    private ProjectStatus status;

    private long totalPbis;
    private long readyPbis;
    private long inSprintPbis;
    private long completedPbis;

    private SprintDashboardResponse activeSprint;
    private TaskStatusBreakdownResponse taskBreakdown;
    private List<MemberWorkloadResponse> memberWorkload;

}
