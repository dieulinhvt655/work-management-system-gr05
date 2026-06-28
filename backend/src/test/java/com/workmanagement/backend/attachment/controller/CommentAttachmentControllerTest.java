package com.workmanagement.backend.attachment.controller;

import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.service.AttachmentService;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentAttachmentControllerTest {

    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private CommentAttachmentController commentAttachmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentAttachmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_shouldReturnAttachments() throws Exception {
        when(attachmentService.findByComment(10L, 20L, 30L, 70L, 100L)).thenReturn(List.of(
                AttachmentResponse.builder().id(41L).fileName("note.png").commentId(100L).build()
        ));

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/tasks/70/comments/100/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fileName").value("note.png"));
    }

    @Test
    void upload_shouldReturnAttachment() throws Exception {
        when(attachmentService.uploadToComment(eq(10L), eq(20L), eq(30L), eq(70L), eq(100L), any()))
                .thenReturn(AttachmentResponse.builder().id(41L).fileName("note.png").commentId(100L).build());

        mockMvc.perform(multipart("/api/v1/workspaces/10/teams/20/projects/30/tasks/70/comments/100/attachments")
                        .file(new MockMultipartFile("file", "note.png", "image/png", "data".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(100));
    }

    @Test
    void delete_shouldInvokeService() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/workspaces/10/teams/20/projects/30/tasks/70/comments/100/attachments/41"))
                .andExpect(status().isOk());

        verify(attachmentService).deleteCommentAttachment(10L, 20L, 30L, 70L, 100L, 41L);
    }

}
