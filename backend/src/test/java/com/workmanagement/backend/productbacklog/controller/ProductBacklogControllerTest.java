package com.workmanagement.backend.productbacklog.controller;

import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogResponse;
import com.workmanagement.backend.productbacklog.service.ProductBacklogService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductBacklogControllerTest {

    @Mock
    private ProductBacklogService productBacklogService;

    @InjectMocks
    private ProductBacklogController productBacklogController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productBacklogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findByProject_shouldReturnBacklog() throws Exception {
        when(productBacklogService.findByProject(10L, 20L, 30L))
                .thenReturn(ProductBacklogResponse.builder().id(50L).name("Alpha Backlog").build());

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/backlog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alpha Backlog"));
    }

    @Test
    void update_shouldReturnUpdatedBacklog() throws Exception {
        when(productBacklogService.update(eq(10L), eq(20L), eq(30L), any()))
                .thenReturn(ProductBacklogResponse.builder().id(50L).name("Updated Backlog").build());

        mockMvc.perform(put("/api/v1/workspaces/10/teams/20/projects/30/backlog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Backlog"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Backlog"));
    }

}
