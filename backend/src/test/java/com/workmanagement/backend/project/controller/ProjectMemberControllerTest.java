package com.workmanagement.backend.project.controller;

import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.project.dto.response.ProjectMemberResponse;
import com.workmanagement.backend.project.service.ProjectMemberService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectMemberControllerTest {

    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private ProjectMemberController projectMemberController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectMemberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_shouldReturnMembers() throws Exception {
        when(projectMemberService.findAll(10L, 20L, 30L))
                .thenReturn(List.of(ProjectMemberResponse.builder().id(1L).build()));

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void add_shouldReturnMember() throws Exception {
        when(projectMemberService.add(eq(10L), eq(20L), eq(30L), any()))
                .thenReturn(ProjectMemberResponse.builder().id(1L).build());

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamMemberId":7,"roleId":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void remove_shouldReturnSuccess() throws Exception {
        doNothing().when(projectMemberService).remove(10L, 20L, 30L, 9L);

        mockMvc.perform(delete("/api/v1/workspaces/10/teams/20/projects/30/members/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

}
