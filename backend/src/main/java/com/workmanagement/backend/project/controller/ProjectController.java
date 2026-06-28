package com.workmanagement.backend.project.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.project.dto.request.CreateProjectRequest;
import com.workmanagement.backend.project.dto.response.ProjectResponse;
import com.workmanagement.backend.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** Tạo dự án (tiền đề UC-2.8) */
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

    /** Chi tiết dự án */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(projectService.findById(workspaceId, teamId, projectId));
    }

}
