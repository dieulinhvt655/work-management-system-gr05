package com.workmanagement.backend.dashboard.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.dashboard.dto.request.DashboardFilterRequest;
import com.workmanagement.backend.dashboard.dto.response.DashboardSummaryResponse;
import com.workmanagement.backend.dashboard.dto.response.PersonalDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.ProjectDashboardResponse;
import com.workmanagement.backend.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** UC-7.1 — Dashboard tổng quan workspace */
    @GetMapping("/api/v1/workspaces/{workspaceId}/dashboard")
    public ApiResponse<DashboardSummaryResponse> getWorkspaceDashboard(@PathVariable Long workspaceId) {
        return ApiResponse.success(
                dashboardService.getWorkspaceDashboard(workspaceId),
                "Lấy dashboard workspace thành công"
        );
    }

    /** UC-7.2 — Dashboard nhóm làm việc */
    @GetMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/dashboard")
    public ApiResponse<DashboardSummaryResponse> getTeamDashboard(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId
    ) {
        return ApiResponse.success(
                dashboardService.getTeamDashboard(workspaceId, teamId),
                "Lấy dashboard nhóm thành công"
        );
    }

    /** UC-7.3 — Dashboard dự án */
    @GetMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/dashboard")
    public ApiResponse<ProjectDashboardResponse> getProjectDashboard(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                dashboardService.getProjectDashboard(workspaceId, teamId, projectId),
                "Lấy dashboard dự án thành công"
        );
    }

    /** UC-7.4 — Dashboard cá nhân trong dự án */
    @GetMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/dashboard/personal")
    public ApiResponse<PersonalDashboardResponse> getPersonalDashboard(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer upcomingLimit
    ) {
        DashboardFilterRequest filter = new DashboardFilterRequest();
        if (upcomingLimit != null) {
            filter.setUpcomingLimit(upcomingLimit);
        }

        return ApiResponse.success(
                dashboardService.getPersonalDashboard(workspaceId, teamId, projectId, filter),
                "Lấy dashboard cá nhân thành công"
        );
    }

}
