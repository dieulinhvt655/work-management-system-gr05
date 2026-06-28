package com.workmanagement.backend.project.controller;

import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.project.dto.response.ProjectResponse;
import com.workmanagement.backend.project.service.ProjectService;
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
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnProject() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(30L).name("Alpha").build();
        when(projectService.create(eq(10L), eq(20L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"PRJ-001",
                                  "name":"Alpha",
                                  "projectManagerMemberId":7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alpha"));
    }

    @Test
    void findAll_shouldReturnPagedList() throws Exception {
        PageResponse<ProjectResponse> page = PageResponse.<ProjectResponse>builder()
                .items(List.of(ProjectResponse.builder().id(30L).name("Alpha").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(projectService.findAll(10L, 20L, 0, 20, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Alpha"));
    }

    @Test
    void findById_shouldReturnProject() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(30L).name("Alpha").build();
        when(projectService.findById(10L, 20L, 30L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alpha"));
    }

    @Test
    void update_shouldReturnUpdatedProject() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(30L).name("Beta").build();
        when(projectService.update(eq(10L), eq(20L), eq(30L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/workspaces/10/teams/20/projects/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Beta"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Beta"));
    }

    @Test
    void activate_shouldReturnActiveProject() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(30L).status(ProjectStatus.ACTIVE).build();
        when(projectService.activate(10L, 20L, 30L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void complete_shouldReturnCompletedProject() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(30L).status(ProjectStatus.COMPLETED).build();
        when(projectService.complete(10L, 20L, 30L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void archive_shouldReturnArchivedProject() throws Exception {
        ProjectResponse response = ProjectResponse.builder().id(30L).status(ProjectStatus.ARCHIVED).build();
        when(projectService.archive(10L, 20L, 30L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        verify(projectService).archive(10L, 20L, 30L);
    }

}
