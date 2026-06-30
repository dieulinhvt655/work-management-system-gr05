package com.workmanagement.backend.team.controller;

import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.team.dto.response.TeamMemberResponse;
import com.workmanagement.backend.team.service.TeamMemberService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeamMemberControllerTest {

    @Mock
    private TeamMemberService teamMemberService;

    @InjectMocks
    private TeamMemberController teamMemberController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamMemberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_shouldReturnMembers() throws Exception {
        when(teamMemberService.findAll(10L, 20L))
                .thenReturn(List.of(TeamMemberResponse.builder().id(1L).build()));

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void add_shouldReturnMember() throws Exception {
        TeamMemberResponse response = TeamMemberResponse.builder().id(1L).build();
        when(teamMemberService.add(eq(10L), eq(20L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workspaceMemberId":5,"roleId":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void assignLeader_shouldReturnLeader() throws Exception {
        TeamMemberResponse response = TeamMemberResponse.builder().id(1L).build();
        when(teamMemberService.assignLeader(10L, 20L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/members/1/assign-leader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));

        verify(teamMemberService).assignLeader(10L, 20L, 1L);
    }

    @Test
    void revokeLeader_shouldReturnSuccess() throws Exception {
        TeamMemberResponse response = TeamMemberResponse.builder().id(1L).build();
        when(teamMemberService.revokeLeader(10L, 20L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/members/1/revoke-leader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));

        verify(teamMemberService).revokeLeader(10L, 20L, 1L);
    }

    @Test
    void transfer_shouldReturnSuccess() throws Exception {
        TeamMemberResponse response = TeamMemberResponse.builder().id(7L).build();
        when(teamMemberService.transfer(eq(10L), eq(20L), eq(7L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/members/7/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetTeamId":30}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));

        verify(teamMemberService).transfer(eq(10L), eq(20L), eq(7L), any());
    }

}
