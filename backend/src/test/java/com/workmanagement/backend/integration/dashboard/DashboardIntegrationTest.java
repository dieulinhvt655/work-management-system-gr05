package com.workmanagement.backend.integration.dashboard;

import com.workmanagement.backend.integration.support.AbstractIntegrationTest;
import com.workmanagement.backend.integration.support.ProjectTestContext;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Test
    void allDashboardScopes_shouldReturnMetrics() throws Exception {
        ProjectTestContext context = setupPmProject(uniqueId());
        String token = context.tokens().accessToken();

        createPbi(context, "Dashboard PBI");

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/dashboard", context.workspaceId())
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scopeType").value("workspace"))
                .andExpect(jsonPath("$.data.totalTeams").value(1))
                .andExpect(jsonPath("$.data.totalProjects").value(1))
                .andExpect(jsonPath("$.data.activeProjects").value(1));

        mockMvc.perform(get(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/dashboard",
                        context.workspaceId(), context.teamId())
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("team"))
                .andExpect(jsonPath("$.data.totalProjects").value(1))
                .andExpect(jsonPath("$.data.activeProjects").value(1));

        mockMvc.perform(get(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/dashboard",
                        context.workspaceId(), context.teamId(), context.projectId())
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(context.projectId()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalPbis").value(1));

        mockMvc.perform(get(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/dashboard/personal",
                        context.workspaceId(), context.teamId(), context.projectId())
                        .param("upcomingLimit", "3")
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(context.projectId()))
                .andExpect(jsonPath("$.data.totalAssignedTasks").value(0));
    }

    @Test
    void workspaceDashboard_withoutPermission_shouldReturn403() throws Exception {
        ProjectTestContext context = setupPmProject(uniqueId());
        var outsider = registerAndLogin(uniqueId());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/dashboard", context.workspaceId())
                        .with(bearer(outsider.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
