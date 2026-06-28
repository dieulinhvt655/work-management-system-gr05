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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnTask() throws Exception {
        when(taskService.createPreparationTask(eq(10L), eq(20L), eq(30L), eq(60L), any()))
                .thenReturn(TaskResponse.builder().id(70L).title("Design login").build());

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Design login"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Design login"));
    }

    @Test
    void findAll_shouldReturnTasks() throws Exception {
        when(taskService.findPreparationTasks(10L, 20L, 30L, 60L))
                .thenReturn(List.of(TaskResponse.builder().id(70L).title("Design login").build()));

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Design login"));
    }

    @Test
    void assign_shouldReturnAssignedTask() throws Exception {
        when(taskService.assignPreparationTask(eq(10L), eq(20L), eq(30L), eq(60L), eq(70L), any()))
                .thenReturn(TaskResponse.builder().id(70L).assigneeMemberId(12L).build());

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60/tasks/70/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeMemberId":12}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeMemberId").value(12));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60/tasks/70"))
                .andExpect(status().isOk());

        verify(taskService).deletePreparationTask(10L, 20L, 30L, 60L, 70L);
    }

    @Test
    void update_shouldReturnUpdatedTask() throws Exception {
        when(taskService.updatePreparationTask(eq(10L), eq(20L), eq(30L), eq(60L), eq(70L), any()))
                .thenReturn(TaskResponse.builder().id(70L).title("Updated").build());

        mockMvc.perform(put("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60/tasks/70")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

}
