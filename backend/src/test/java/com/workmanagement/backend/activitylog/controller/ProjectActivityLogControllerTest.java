package com.workmanagement.backend.activitylog.controller;

import com.workmanagement.backend.activitylog.dto.response.ActivityLogResponse;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectActivityLogControllerTest {

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProjectActivityLogController projectActivityLogController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectActivityLogController).build();
    }

    @Test
    void findByProject_shouldReturnLogs() throws Exception {
        PageResponse<ActivityLogResponse> page = PageResponse.<ActivityLogResponse>builder()
                .items(List.of(ActivityLogResponse.builder().id(1L).action("PROJECT_CREATED").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(activityLogService.findByProject(10L, 20L, 30L, 0, 20, null, null, null, null))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/activity-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].action").value("PROJECT_CREATED"));
    }

}
