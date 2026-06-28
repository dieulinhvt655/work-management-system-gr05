package com.workmanagement.backend.attachment.controller;

import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.service.AttachmentService;
import com.workmanagement.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/tasks/{taskId}/comments/{commentId}/attachments")
@RequiredArgsConstructor
public class CommentAttachmentController {

    private final AttachmentService attachmentService;

    /** UC-6.3 — Danh sách tệp đính kèm bình luận */
    @GetMapping
    public ApiResponse<List<AttachmentResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(attachmentService.findByComment(
                workspaceId, teamId, projectId, taskId, commentId
        ));
    }

    /** UC-6.3 — Đính kèm tệp vào bình luận */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AttachmentResponse> upload(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(
                attachmentService.uploadToComment(workspaceId, teamId, projectId, taskId, commentId, file),
                "Đính kèm tệp thành công"
        );
    }

    /** UC-6.3 — Xóa tệp đính kèm */
    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @PathVariable Long attachmentId
    ) {
        attachmentService.deleteCommentAttachment(
                workspaceId, teamId, projectId, taskId, commentId, attachmentId
        );
        return ApiResponse.success(null, "Xóa tệp đính kèm thành công");
    }

    /** UC-6.3 — Tải tệp đính kèm */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @PathVariable Long attachmentId
    ) {
        AttachmentService.DownloadableAttachment downloadable = attachmentService.downloadCommentAttachment(
                workspaceId, teamId, projectId, taskId, commentId, attachmentId
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadable.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadable.fileName() + "\"")
                .body(downloadable.resource());
    }

}
