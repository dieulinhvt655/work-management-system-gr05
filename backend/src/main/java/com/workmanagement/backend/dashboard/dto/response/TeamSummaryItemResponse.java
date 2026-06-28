package com.workmanagement.backend.dashboard.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamSummaryItemResponse {

    private Long teamId;
    private String teamName;
    private long projectCount;
    private long activeProjects;

}
