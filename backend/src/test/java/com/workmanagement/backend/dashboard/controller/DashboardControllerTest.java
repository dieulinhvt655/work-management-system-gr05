package com.workmanagement.backend.dashboard.controller;

import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.dashboard.dto.response.DashboardSummaryResponse;
import com.workmanagement.backend.dashboard.dto.response.PersonalDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.ProjectDashboardResponse;
import com.workmanagement.backend.dashboard.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getWorkspaceDashboard_shouldReturnSummary() throws Exception {
        when(dashboardService.getWorkspaceDashboard(10L)).thenReturn(
                DashboardSummaryResponse.builder()
                        .scopeId(10L)
                        .scopeName("Acme")
                        .scopeType("workspace")
                        .totalProjects(3)
                        .build()
        );

        mockMvc.perform(get("/api/v1/workspaces/10/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("workspace"))
                .andExpect(jsonPath("$.data.totalProjects").value(3));
    }

    @Test
    void getTeamDashboard_shouldReturnSummary() throws Exception {
        when(dashboardService.getTeamDashboard(10L, 20L)).thenReturn(
                DashboardSummaryResponse.builder()
                        .scopeId(20L)
                        .scopeType("team")
                        .activeProjects(2)
                        .build()
        );

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("team"))
                .andExpect(jsonPath("$.data.activeProjects").value(2));
    }

    @Test
    void getProjectDashboard_shouldReturnMetrics() throws Exception {
        when(dashboardService.getProjectDashboard(10L, 20L, 30L)).thenReturn(
                ProjectDashboardResponse.builder()
                        .projectId(30L)
                        .projectName("WMS")
                        .status(ProjectStatus.ACTIVE)
                        .totalPbis(5)
                        .build()
        );

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectName").value("WMS"))
                .andExpect(jsonPath("$.data.totalPbis").value(5));
    }

    @Test
    void getPersonalDashboard_shouldReturnAssignedTasks() throws Exception {
        when(dashboardService.getPersonalDashboard(eq(10L), eq(20L), eq(30L), any())).thenReturn(
                PersonalDashboardResponse.builder()
                        .projectId(30L)
                        .totalAssignedTasks(4)
                        .build()
        );

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/dashboard/personal")
                        .param("upcomingLimit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAssignedTasks").value(4));

        verify(dashboardService).getPersonalDashboard(eq(10L), eq(20L), eq(30L), any());
    }

}
