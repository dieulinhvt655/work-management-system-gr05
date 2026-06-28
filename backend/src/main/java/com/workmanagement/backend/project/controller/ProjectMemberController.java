package com.workmanagement.backend.project.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.project.dto.request.AddProjectMemberRequest;
import com.workmanagement.backend.project.dto.request.UpdateProjectMemberRequest;
import com.workmanagement.backend.project.dto.response.ProjectMemberResponse;
import com.workmanagement.backend.project.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    /** UC-2.8 — Danh sách thành viên dự án */
    @GetMapping
    public ApiResponse<List<ProjectMemberResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(projectMemberService.findAll(workspaceId, teamId, projectId));
    }

    /** UC-2.8 — Phân bổ thành viên vào dự án */
    @PostMapping
    public ApiResponse<ProjectMemberResponse> add(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        return ApiResponse.success(
                projectMemberService.add(workspaceId, teamId, projectId, request),
                "Phân bổ thành viên dự án thành công"
        );
    }

    /** UC-2.8 — Cập nhật vai trò thành viên dự án */
    @PatchMapping("/{memberId}")
    public ApiResponse<ProjectMemberResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateProjectMemberRequest request
    ) {
        return ApiResponse.success(
                projectMemberService.update(workspaceId, teamId, projectId, memberId, request),
                "Cập nhật thành viên dự án thành công"
        );
    }

}
