package com.workmanagement.backend.team.controller;

import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.team.dto.request.CreateTeamRequest;
import com.workmanagement.backend.team.dto.request.UpdateTeamRequest;
import com.workmanagement.backend.team.dto.response.TeamResponse;
import com.workmanagement.backend.team.service.TeamService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /** UC-2.3 — Tạo nhóm làm việc */
    @PostMapping
    public ApiResponse<TeamResponse> create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return ApiResponse.success(teamService.create(workspaceId, request), "Tạo nhóm thành công");
    }

    /** UC-2.4 — Danh sách nhóm */
    @GetMapping
    public ApiResponse<PageResponse<TeamResponse>> findAll(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CommonStatus status
    ) {
        return ApiResponse.success(teamService.findAll(workspaceId, page, size, keyword, status));
    }

    /** UC-2.4 — Chi tiết nhóm */
    @GetMapping("/{teamId}")
    public ApiResponse<TeamResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId
    ) {
        return ApiResponse.success(teamService.findById(workspaceId, teamId));
    }

    /** UC-2.4 — Cập nhật nhóm */
    @PutMapping("/{teamId}")
    public ApiResponse<TeamResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        return ApiResponse.success(teamService.update(workspaceId, teamId, request), "Cập nhật nhóm thành công");
    }

    /** UC-2.5 — Giải thể nhóm */
    @PatchMapping("/{teamId}/disband")
    public ApiResponse<TeamResponse> disband(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId
    ) {
        return ApiResponse.success(teamService.disband(workspaceId, teamId), "Giải thể nhóm thành công");
    }

}
