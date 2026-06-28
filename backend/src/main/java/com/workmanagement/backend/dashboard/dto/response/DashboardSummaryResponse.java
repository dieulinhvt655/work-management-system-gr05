package com.workmanagement.backend.dashboard.dto.response;

import com.workmanagement.backend.common.enums.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardSummaryResponse {

    private Long scopeId;
    private String scopeName;
    private String scopeType;

    private long totalTeams;
    private long totalProjects;
    private long totalMembers;
    private long activeProjects;
    private long activeSprints;

    private Map<ProjectStatus, Long> projectsByStatus;
    private List<TeamSummaryItemResponse> teams;

}
