package com.workmanagement.backend.comment.controller;

import com.workmanagement.backend.comment.dto.request.CreateCommentRequest;
import com.workmanagement.backend.comment.dto.request.UpdateCommentRequest;
import com.workmanagement.backend.comment.dto.response.CommentResponse;
import com.workmanagement.backend.comment.service.CommentService;
import com.workmanagement.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** UC-6.1 — Tạo bình luận */
    @PostMapping
    public ApiResponse<CommentResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ApiResponse.success(
                commentService.create(workspaceId, teamId, projectId, taskId, request),
                "Tạo bình luận thành công"
        );
    }

    /** UC-6.4 — Danh sách trao đổi trong task */
    @GetMapping
    public ApiResponse<List<CommentResponse>> findByTask(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(commentService.findByTask(workspaceId, teamId, projectId, taskId));
    }

    /** UC-6.4 — Chi tiết bình luận */
    @GetMapping("/{commentId}")
    public ApiResponse<CommentResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commentService.findById(workspaceId, teamId, projectId, taskId, commentId));
    }

    /** UC-6.2 — Chỉnh sửa bình luận */
    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return ApiResponse.success(
                commentService.update(workspaceId, teamId, projectId, taskId, commentId, request),
                "Cập nhật bình luận thành công"
        );
    }

    /** UC-6.2 — Xóa bình luận */
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId
    ) {
        commentService.delete(workspaceId, teamId, projectId, taskId, commentId);
        return ApiResponse.success(null, "Xóa bình luận thành công");
    }

}
