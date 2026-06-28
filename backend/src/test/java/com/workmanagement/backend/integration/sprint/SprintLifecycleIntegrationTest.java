package com.workmanagement.backend.integration.sprint;

import com.workmanagement.backend.integration.support.AbstractIntegrationTest;
import com.workmanagement.backend.integration.support.ProjectTestContext;
import com.workmanagement.backend.productbacklog.dto.request.CreateProductBacklogItemRequest;
import com.workmanagement.backend.sprint.dto.request.CompleteSprintRequest;
import com.workmanagement.backend.sprint.dto.request.CreateSprintRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SprintLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Test
    void sprintFullLifecycle_shouldCompleteSuccessfully() throws Exception {
        ProjectTestContext context = setupPmProject(uniqueId());
        String token = context.tokens().accessToken();

        CreateProductBacklogItemRequest pbiRequest = new CreateProductBacklogItemRequest();
        pbiRequest.setTitle("Integration PBI");

        var pbiResult = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/backlog/items",
                        context.workspaceId(), context.teamId(), context.projectId())
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pbiRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andReturn();
        Long pbiId = readData(pbiResult).get("id").asLong();

        mockMvc.perform(patch(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/backlog/items/{itemId}/ready",
                        context.workspaceId(), context.teamId(), context.projectId(), pbiId)
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        CreateSprintRequest sprintRequest = new CreateSprintRequest();
        sprintRequest.setName("Sprint 1");
        sprintRequest.setGoal("Integration sprint lifecycle");
        sprintRequest.setStartDate(LocalDate.now());
        sprintRequest.setEndDate(LocalDate.now().plusWeeks(2));

        var sprintResult = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints",
                        context.workspaceId(), context.teamId(), context.projectId())
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sprintRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PLANNING"))
                .andReturn();
        Long sprintId = readData(sprintResult).get("id").asLong();

        mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints/{sprintId}/pbis/{itemId}",
                        context.workspaceId(), context.teamId(), context.projectId(), sprintId, pbiId)
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_SPRINT"));

        mockMvc.perform(patch(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints/{sprintId}/start",
                        context.workspaceId(), context.teamId(), context.projectId(), sprintId)
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints/{sprintId}/progress",
                        context.workspaceId(), context.teamId(), context.projectId(), sprintId)
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sprintId").value(sprintId));

        CompleteSprintRequest completeRequest = new CompleteSprintRequest();
        completeRequest.setSummary("Sprint completed in integration test");

        mockMvc.perform(patch(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints/{sprintId}/complete",
                        context.workspaceId(), context.teamId(), context.projectId(), sprintId)
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.summary").value("Sprint completed in integration test"));

        mockMvc.perform(get(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints/history",
                        context.workspaceId(), context.teamId(), context.projectId())
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(sprintId))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }
}
