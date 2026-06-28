package com.workmanagement.backend.project.controller;

import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.project.dto.request.CreateProjectRequest;
import com.workmanagement.backend.project.dto.request.UpdateProjectRequest;
import com.workmanagement.backend.project.dto.response.ProjectResponse;
import com.workmanagement.backend.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** UC-3.1 — Tạo dự án mới */
    @PostMapping
    public ApiResponse<ProjectResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        return ApiResponse.success(
                projectService.create(workspaceId, teamId, request),
                "Tạo dự án thành công"
        );
    }

    /** UC-3.7 — Danh sách dự án */
    @GetMapping
    public ApiResponse<PageResponse<ProjectResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProjectStatus status
    ) {
        return ApiResponse.success(projectService.findAll(workspaceId, teamId, page, size, keyword, status));
    }

    /** UC-3.2 — Chi tiết dự án */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(projectService.findById(workspaceId, teamId, projectId));
    }

    /** UC-3.3 — Cập nhật thông tin dự án */
    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ApiResponse.success(
                projectService.update(workspaceId, teamId, projectId, request),
                "Cập nhật dự án thành công"
        );
    }

    /** UC-3.5 — Kích hoạt dự án */
    @PatchMapping("/{projectId}/activate")
    public ApiResponse<ProjectResponse> activate(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectService.activate(workspaceId, teamId, projectId),
                "Kích hoạt dự án thành công"
        );
    }

    /** UC-3.11 — Kết thúc dự án */
    @PatchMapping("/{projectId}/complete")
    public ApiResponse<ProjectResponse> complete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectService.complete(workspaceId, teamId, projectId),
                "Kết thúc dự án thành công"
        );
    }

    /** UC-3.12 — Lưu trữ dự án */
    @PatchMapping("/{projectId}/archive")
    public ApiResponse<ProjectResponse> archive(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectService.archive(workspaceId, teamId, projectId),
                "Lưu trữ dự án thành công"
        );
    }

}
