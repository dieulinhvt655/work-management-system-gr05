package com.workmanagement.backend.team.controller;

import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.team.dto.response.TeamResponse;
import com.workmanagement.backend.team.service.TeamService;
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
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnTeam() throws Exception {
        TeamResponse response = TeamResponse.builder().id(1L).name("Dev Team").build();
        when(teamService.create(eq(10L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/10/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dev Team","description":"Test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Dev Team"));
    }

    @Test
    void findAll_shouldReturnPagedList() throws Exception {
        PageResponse<TeamResponse> page = PageResponse.<TeamResponse>builder()
                .items(List.of(TeamResponse.builder().id(1L).name("Dev Team").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(teamService.findAll(10L, 0, 20, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/v1/workspaces/10/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Dev Team"));
    }

    @Test
    void findById_shouldReturnTeam() throws Exception {
        TeamResponse response = TeamResponse.builder().id(1L).name("Dev Team").build();
        when(teamService.findById(10L, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/workspaces/10/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void update_shouldReturnUpdatedTeam() throws Exception {
        TeamResponse response = TeamResponse.builder().id(1L).name("Updated").build();
        when(teamService.update(eq(10L), eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/workspaces/10/teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    void disband_shouldReturnInactiveTeam() throws Exception {
        TeamResponse response = TeamResponse.builder().id(1L).status(CommonStatus.INACTIVE).build();
        when(teamService.disband(10L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/1/disband"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(teamService).disband(10L, 1L);
    }

}
