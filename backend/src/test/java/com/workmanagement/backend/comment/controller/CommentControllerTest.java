package com.workmanagement.backend.comment.controller;

import com.workmanagement.backend.comment.dto.response.CommentResponse;
import com.workmanagement.backend.comment.service.CommentService;
import com.workmanagement.backend.common.enums.CommentStatus;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnComment() throws Exception {
        when(commentService.create(eq(10L), eq(20L), eq(30L), eq(70L), any()))
                .thenReturn(CommentResponse.builder().id(100L).content("Looks good").build());

        mockMvc.perform(post("/api/v1/workspaces/10/teams/20/projects/30/tasks/70/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Looks good"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Looks good"));
    }

    @Test
    void findByTask_shouldReturnThread() throws Exception {
        when(commentService.findByTask(10L, 20L, 30L, 70L)).thenReturn(List.of(
                CommentResponse.builder().id(100L).content("Looks good").build()
        ));

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/tasks/70/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Looks good"));
    }

    @Test
    void update_shouldReturnEditedComment() throws Exception {
        when(commentService.update(eq(10L), eq(20L), eq(30L), eq(70L), eq(100L), any()))
                .thenReturn(CommentResponse.builder()
                        .id(100L)
                        .content("Updated")
                        .status(CommentStatus.EDITED)
                        .build());

        mockMvc.perform(put("/api/v1/workspaces/10/teams/20/projects/30/tasks/70/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EDITED"));
    }

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/10/teams/20/projects/30/tasks/70/comments/100"))
                .andExpect(status().isOk());

        verify(commentService).delete(10L, 20L, 30L, 70L, 100L);
    }

}
