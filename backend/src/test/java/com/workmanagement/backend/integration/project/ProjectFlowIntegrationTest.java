package com.workmanagement.backend.integration.project;

import com.workmanagement.backend.integration.support.AbstractIntegrationTest;
import com.workmanagement.backend.integration.support.LoginTokens;
import com.workmanagement.backend.project.dto.request.CreateProjectRequest;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
import com.workmanagement.backend.team.dto.request.CreateTeamRequest;
import com.workmanagement.backend.workspace.dto.request.CreateWorkspaceRequest;
import com.workmanagement.backend.common.enums.RoleScope;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void workspaceTeamProjectHierarchy_shouldWorkEndToEnd() throws Exception {
        LoginTokens tokens = loginAsAdmin();
        String suffix = uniqueId();

        CreateWorkspaceRequest workspaceRequest = new CreateWorkspaceRequest();
        workspaceRequest.setName("Flow Workspace " + suffix);

        var workspaceResult = mockMvc.perform(post("/api/v1/workspaces")
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workspaceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Flow Workspace " + suffix))
                .andReturn();
        Long workspaceId = readData(workspaceResult).get("id").asLong();

        CreateTeamRequest teamRequest = new CreateTeamRequest();
        teamRequest.setName("Flow Team " + suffix);

        var teamResult = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/teams", workspaceId)
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teamRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        Long teamId = readData(teamResult).get("id").asLong();

        var membersResult = mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .with(bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andReturn();
        Long workspaceMemberId = readData(membersResult).get(0).get("id").asLong();

        Long teamLeaderRoleId = findRoleId(tokens.accessToken(), "Team Leader", RoleScope.TEAM);

        AddTeamMemberRequest addMemberRequest = new AddTeamMemberRequest();
        addMemberRequest.setWorkspaceMemberId(workspaceMemberId);
        addMemberRequest.setRoleId(teamLeaderRoleId);

        var teamMemberResult = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/members",
                        workspaceId, teamId)
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        Long teamMemberId = readData(teamMemberResult).get("id").asLong();

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setCode("FLOW-" + suffix);
        projectRequest.setName("Flow Project " + suffix);
        projectRequest.setStartDate(LocalDate.now());
        projectRequest.setEndDate(LocalDate.now().plusMonths(2));
        projectRequest.setProjectManagerMemberId(teamMemberId);

        var projectResult = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects",
                        workspaceId, teamId)
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        Long projectId = readData(projectResult).get("id").asLong();

        mockMvc.perform(patch(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/activate",
                        workspaceId, teamId, projectId)
                        .with(bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}",
                        workspaceId, teamId, projectId)
                        .with(bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(projectId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.code").value("FLOW-" + suffix));
    }

    @Test
    void userWithoutPermission_shouldNotCreateWorkspace() throws Exception {
        LoginTokens tokens = registerAndLogin(uniqueId());

        CreateWorkspaceRequest workspaceRequest = new CreateWorkspaceRequest();
        workspaceRequest.setName("Forbidden Workspace");

        mockMvc.perform(post("/api/v1/workspaces")
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workspaceRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
