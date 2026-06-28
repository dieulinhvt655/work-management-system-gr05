package com.workmanagement.backend.attachment.controller;

import com.workmanagement.backend.attachment.dto.request.UpdateAttachmentRequest;
import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.service.AttachmentService;
import com.workmanagement.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/attachments")
@RequiredArgsConstructor
public class ProjectAttachmentController {

    private final AttachmentService attachmentService;

    /** UC-3.9 — Danh sách tài liệu dự án */
    @GetMapping
    public ApiResponse<List<AttachmentResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(attachmentService.findByProject(workspaceId, teamId, projectId));
    }

    /** UC-3.8 — Upload tài liệu dự án */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AttachmentResponse> upload(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(
                attachmentService.upload(workspaceId, teamId, projectId, file),
                "Upload tài liệu thành công"
        );
    }

    /** UC-3.8 — Cập nhật tên tài liệu */
    @PutMapping("/{attachmentId}")
    public ApiResponse<AttachmentResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long attachmentId,
            @Valid @RequestBody UpdateAttachmentRequest request
    ) {
        return ApiResponse.success(
                attachmentService.update(workspaceId, teamId, projectId, attachmentId, request),
                "Cập nhật tài liệu thành công"
        );
    }

    /** UC-3.8 — Xóa tài liệu dự án */
    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long attachmentId
    ) {
        attachmentService.delete(workspaceId, teamId, projectId, attachmentId);
        return ApiResponse.success(null, "Xóa tài liệu thành công");
    }

    /** UC-3.9 — Tải tài liệu dự án */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long attachmentId
    ) {
        AttachmentService.DownloadableAttachment downloadable = attachmentService.download(
                workspaceId, teamId, projectId, attachmentId
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadable.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadable.fileName() + "\"")
                .body(downloadable.resource());
    }

}
