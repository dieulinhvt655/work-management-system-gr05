package com.workmanagement.backend.sprint.controller;

import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.sprint.dto.response.SprintProgressResponse;
import com.workmanagement.backend.sprint.dto.response.SprintResponse;
import com.workmanagement.backend.sprint.service.SprintService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SprintControllerTest {

    @Mock
    private SprintService sprintService;

    @InjectMocks
    private SprintController sprintController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sprintController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnSprint() throws Exception {
        SprintResponse response = SprintResponse.builder()
                .id(80L)
                .name("Sprint 1")
                .status(SprintStatus.PLANNING)
                .build();
        when(sprintService.create(eq(10L), eq(20L), eq(30L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Sprint 1",
                                  "startDate":"2026-06-01",
                                  "endDate":"2026-06-14"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sprint 1"));
    }

    @Test
    void findAll_shouldReturnPagedSprints() throws Exception {
        PageResponse<SprintResponse> page = PageResponse.<SprintResponse>builder()
                .items(List.of(SprintResponse.builder().id(80L).name("Sprint 1").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(sprintService.findAll(10L, 20L, 30L, 0, 20, null)).thenReturn(page);

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Sprint 1"));
    }

    @Test
    void start_shouldActivateSprint() throws Exception {
        SprintResponse response = SprintResponse.builder().id(80L).status(SprintStatus.ACTIVE).build();
        when(sprintService.start(10L, 20L, 30L, 80L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void complete_shouldReturnCompletedSprint() throws Exception {
        SprintResponse response = SprintResponse.builder()
                .id(80L)
                .status(SprintStatus.COMPLETED)
                .summary("Done")
                .build();
        when(sprintService.complete(eq(10L), eq(20L), eq(30L), eq(80L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"summary":"Done"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void getProgress_shouldReturnProgress() throws Exception {
        SprintProgressResponse response = SprintProgressResponse.builder()
                .sprintId(80L)
                .completionPercent(50)
                .doneTasks(1)
                .totalTasks(2)
                .build();
        when(sprintService.getProgress(10L, 20L, 30L, 80L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completionPercent").value(50));
    }

    @Test
    void addPbi_shouldReturnUpdatedPbi() throws Exception {
        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder()
                .id(60L)
                .sprintId(80L)
                .build();
        when(sprintService.addPbi(10L, 20L, 30L, 80L, 60L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/sprints/80/pbis/60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sprintId").value(80));
    }

    @Test
    void findHistory_shouldReturnHistory() throws Exception {
        when(sprintService.findHistory(10L, 20L, 30L)).thenReturn(List.of(
                SprintResponse.builder().id(80L).status(SprintStatus.COMPLETED).build()
        ));

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/sprints/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));

        verify(sprintService).findHistory(10L, 20L, 30L);
    }

}
