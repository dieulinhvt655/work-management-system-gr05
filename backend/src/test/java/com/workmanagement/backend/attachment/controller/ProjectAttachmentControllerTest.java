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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectAttachmentControllerTest {

    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private ProjectAttachmentController projectAttachmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectAttachmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_shouldReturnAttachments() throws Exception {
        when(attachmentService.findByProject(10L, 20L, 30L)).thenReturn(
                List.of(AttachmentResponse.builder().id(40L).fileName("spec.pdf").build())
        );

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fileName").value("spec.pdf"));
    }

    @Test
    void upload_shouldReturnAttachment() throws Exception {
        when(attachmentService.upload(eq(10L), eq(20L), eq(30L), any())).thenReturn(
                AttachmentResponse.builder().id(40L).fileName("spec.pdf").build()
        );

        mockMvc.perform(multipart("/api/v1/workspaces/10/teams/20/projects/30/attachments")
                        .file(new MockMultipartFile("file", "spec.pdf", "application/pdf", "data".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("spec.pdf"));
    }

    @Test
    void update_shouldReturnUpdatedAttachment() throws Exception {
        when(attachmentService.update(eq(10L), eq(20L), eq(30L), eq(40L), any())).thenReturn(
                AttachmentResponse.builder().id(40L).fileName("new.pdf").build()
        );

        mockMvc.perform(put("/api/v1/workspaces/10/teams/20/projects/30/attachments/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fileName":"new.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("new.pdf"));
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/10/teams/20/projects/30/attachments/40"))
                .andExpect(status().isOk());

        verify(attachmentService).delete(10L, 20L, 30L, 40L);
    }

    @Test
    void download_shouldReturnFile() throws Exception {
        when(attachmentService.download(10L, 20L, 30L, 40L)).thenReturn(
                new AttachmentService.DownloadableAttachment(
                        new ByteArrayResource("data".getBytes()),
                        "spec.pdf",
                        "application/pdf"
                )
        );

        mockMvc.perform(get("/api/v1/workspaces/10/teams/20/projects/30/attachments/40/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"spec.pdf\""));
    }

}
