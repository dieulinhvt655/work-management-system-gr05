package com.workmanagement.backend.workspace.controller;

import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.workspace.dto.response.WorkspaceMemberResponse;
import com.workmanagement.backend.workspace.service.WorkspaceMemberService;
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
class WorkspaceMemberControllerTest {

    @Mock
    private WorkspaceMemberService workspaceMemberService;

    @InjectMocks
    private WorkspaceMemberController workspaceMemberController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(workspaceMemberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_shouldReturnMembers() throws Exception {
        WorkspaceMemberResponse member = WorkspaceMemberResponse.builder().id(1L).workspaceId(10L).build();
        when(workspaceMemberService.findAll(10L)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/v1/workspaces/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void add_shouldReturnCreatedMember() throws Exception {
        WorkspaceMemberResponse response = WorkspaceMemberResponse.builder().id(2L).workspaceId(10L).build();
        when(workspaceMemberService.add(eq(10L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"roleId":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void update_shouldReturnUpdatedMember() throws Exception {
        WorkspaceMemberResponse response = WorkspaceMemberResponse.builder().id(2L).workspaceId(10L).build();
        when(workspaceMemberService.update(eq(10L), eq(2L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workspaces/10/members/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId":3,"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));

        verify(workspaceMemberService).update(eq(10L), eq(2L), any());
    }

}
