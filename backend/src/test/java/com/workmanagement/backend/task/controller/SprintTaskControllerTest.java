package com.workmanagement.backend.task.controller;

import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.task.dto.response.TaskResponse;
import com.workmanagement.backend.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SprintTaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private SprintTaskController sprintTaskController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sprintTaskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnSprintTask() throws Exception {
        when(taskService.createSprintTask(eq(10L), eq(20L), eq(30L), eq(80L), eq(60L), any()))
                .thenReturn(TaskResponse.builder().id(90L).title("Implement login").sprintId(80L).build());

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/tasks/pbi/60")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Implement login"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sprintId").value(80));
    }

    @Test
    void activate_shouldReturnActivatedTask() throws Exception {
        when(taskService.activatePreparationTaskInSprint(10L, 20L, 30L, 80L, 60L, 70L))
                .thenReturn(TaskResponse.builder().id(70L).sprintId(80L).build());

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/tasks/pbi/60/activate/70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sprintId").value(80));
    }

    @Test
    void confirmAssignment_shouldReturnTask() throws Exception {
        when(taskService.confirmAssignment(eq(10L), eq(20L), eq(30L), eq(80L), eq(90L), any()))
                .thenReturn(TaskResponse.builder().id(90L).assigneeMemberId(12L).build());

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/tasks/90/confirm-assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeMemberId":12}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeMemberId").value(12));
    }

    @Test
    void start_shouldReturnInProgressTask() throws Exception {
        when(taskService.startWork(10L, 20L, 30L, 80L, 90L))
                .thenReturn(TaskResponse.builder().id(90L).status(TaskStatus.IN_PROGRESS).build());

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/tasks/90/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void reject_shouldReturnReopenedTask() throws Exception {
        when(taskService.rejectTask(eq(10L), eq(20L), eq(30L), eq(80L), eq(90L), any()))
                .thenReturn(TaskResponse.builder().id(90L).status(TaskStatus.REOPENED).build());

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/tasks/90/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Need more tests"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REOPENED"));
    }

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/tasks/90"))
                .andExpect(status().isOk());
    }

    @Test
    void approve_shouldReturnDoneTask() throws Exception {
        when(taskService.approveTask(10L, 20L, 30L, 80L, 90L))
                .thenReturn(TaskResponse.builder().id(90L).status(TaskStatus.DONE).build());

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/tasks/90/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

}
