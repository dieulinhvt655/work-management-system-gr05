package com.workmanagement.backend.productbacklog.controller;

import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.productbacklog.service.ProductBacklogItemService;
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
class ProductBacklogItemControllerTest {

    @Mock
    private ProductBacklogItemService productBacklogItemService;

    @InjectMocks
    private ProductBacklogItemController productBacklogItemController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productBacklogItemController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnPbi() throws Exception {
        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder()
                .id(60L)
                .title("Login feature")
                .status(PbiStatus.NEW)
                .build();
        when(productBacklogItemService.create(eq(10L), eq(20L), eq(30L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/backlog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Login feature"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Login feature"));
    }

    @Test
    void findAll_shouldReturnPagedList() throws Exception {
        PageResponse<ProductBacklogItemResponse> page = PageResponse.<ProductBacklogItemResponse>builder()
                .items(List.of(ProductBacklogItemResponse.builder().id(60L).title("Login feature").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(productBacklogItemService.findAll(10L, 20L, 30L, 0, 20, null, null, null, null))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/backlog/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("Login feature"));
    }

    @Test
    void findById_shouldReturnItem() throws Exception {
        when(productBacklogItemService.findById(10L, 20L, 30L, 60L))
                .thenReturn(ProductBacklogItemResponse.builder().id(60L).title("Login feature").build());

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(60));
    }

    @Test
    void update_shouldReturnUpdatedItem() throws Exception {
        when(productBacklogItemService.update(eq(10L), eq(20L), eq(30L), eq(60L), any()))
                .thenReturn(ProductBacklogItemResponse.builder().id(60L).title("Updated").build());

        mockMvc.perform(put("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60"))
                .andExpect(status().isOk());

        verify(productBacklogItemService).delete(10L, 20L, 30L, 60L);
    }

    @Test
    void markReady_shouldReturnReadyItem() throws Exception {
        when(productBacklogItemService.markReady(10L, 20L, 30L, 60L))
                .thenReturn(ProductBacklogItemResponse.builder().id(60L).status(PbiStatus.READY).build());

        mockMvc.perform(patch("/api/v1/workspaces/10/teams/20/projects/30/backlog/items/60/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

}
