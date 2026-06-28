package com.workmanagement.backend.workspace.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.workspace.dto.request.AddWorkspaceMemberRequest;
import com.workmanagement.backend.workspace.dto.request.UpdateWorkspaceMemberRequest;
import com.workmanagement.backend.workspace.dto.response.WorkspaceMemberResponse;
import com.workmanagement.backend.workspace.service.WorkspaceMemberService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    /** UC-2.6 — Danh sách thành viên */
    @GetMapping
    public ApiResponse<List<WorkspaceMemberResponse>> findAll(@PathVariable Long workspaceId) {
        return ApiResponse.success(workspaceMemberService.findAll(workspaceId));
    }

    /** UC-2.6 — Thêm thành viên */
    @PostMapping
    public ApiResponse<WorkspaceMemberResponse> add(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request
    ) {
        return ApiResponse.success(
                workspaceMemberService.add(workspaceId, request),
                "Thêm thành viên thành công"
        );
    }

    /** UC-2.6 — Cập nhật thông tin thành viên */
    @PatchMapping("/{memberId}")
    public ApiResponse<WorkspaceMemberResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateWorkspaceMemberRequest request
    ) {
        return ApiResponse.success(
                workspaceMemberService.update(workspaceId, memberId, request),
                "Cập nhật thành viên thành công"
        );
    }

}
