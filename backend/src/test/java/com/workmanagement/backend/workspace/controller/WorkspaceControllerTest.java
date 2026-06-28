package com.workmanagement.backend.workspace.controller;

import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.workspace.dto.response.WorkspaceResponse;
import com.workmanagement.backend.workspace.service.WorkspaceService;
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
class WorkspaceControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private WorkspaceController workspaceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(workspaceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturn201Payload() throws Exception {
        WorkspaceResponse response = WorkspaceResponse.builder().id(1L).name("New WS").build();
        when(workspaceService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New WS","description":"Test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New WS"));
    }

    @Test
    void findAll_shouldReturnPagedList() throws Exception {
        PageResponse<WorkspaceResponse> page = PageResponse.<WorkspaceResponse>builder()
                .items(List.of(WorkspaceResponse.builder().id(1L).name("WS").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(workspaceService.findAll(0, 20, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("WS"));
    }

    @Test
    void findById_shouldReturnWorkspace() throws Exception {
        WorkspaceResponse response = WorkspaceResponse.builder().id(1L).name("WS").build();
        when(workspaceService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/workspaces/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void update_shouldReturnUpdatedWorkspace() throws Exception {
        WorkspaceResponse response = WorkspaceResponse.builder().id(1L).name("Updated").build();
        when(workspaceService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/workspaces/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    void close_shouldReturnClosedWorkspace() throws Exception {
        WorkspaceResponse response = WorkspaceResponse.builder().id(1L).status(CommonStatus.INACTIVE).build();
        when(workspaceService.close(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(workspaceService).close(1L);
    }

}
