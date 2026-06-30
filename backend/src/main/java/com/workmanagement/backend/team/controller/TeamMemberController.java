package com.workmanagement.backend.team.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
import com.workmanagement.backend.team.dto.request.TransferTeamMemberRequest;
import com.workmanagement.backend.team.dto.request.UpdateTeamMemberRequest;
import com.workmanagement.backend.team.dto.response.TeamMemberResponse;
import com.workmanagement.backend.team.service.TeamMemberService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/members")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    /** UC-2.4 — Danh sách thành viên nhóm */
    @GetMapping
    public ApiResponse<List<TeamMemberResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId
    ) {
        return ApiResponse.success(teamMemberService.findAll(workspaceId, teamId));
    }

    /** UC-2.4 — Thêm thành viên vào nhóm */
    @PostMapping
    public ApiResponse<TeamMemberResponse> add(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request
    ) {
        return ApiResponse.success(
                teamMemberService.add(workspaceId, teamId, request),
                "Thêm thành viên nhóm thành công"
        );
    }

    /** UC-2.4 — Cập nhật thành viên nhóm */
    @PatchMapping("/{memberId}")
    public ApiResponse<TeamMemberResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateTeamMemberRequest request
    ) {
        return ApiResponse.success(
                teamMemberService.update(workspaceId, teamId, memberId, request),
                "Cập nhật thành viên nhóm thành công"
        );
    }

    /** UC-2.7 — Gán Team Leader */
    @PatchMapping("/{memberId}/assign-leader")
    public ApiResponse<TeamMemberResponse> assignLeader(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long memberId
    ) {
        return ApiResponse.success(
                teamMemberService.assignLeader(workspaceId, teamId, memberId),
                "Gán Team Leader thành công"
        );
    }

    /** UC-2.7 — Thu hồi Team Leader */
    @PatchMapping("/{memberId}/revoke-leader")
    public ApiResponse<TeamMemberResponse> revokeLeader(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long memberId
    ) {
        return ApiResponse.success(
                teamMemberService.revokeLeader(workspaceId, teamId, memberId),
                "Thu hồi Team Leader thành công"
        );
    }

    /** UC-2.6 — Điều chuyển nhân sự giữa các team */
    @PatchMapping("/{memberId}/transfer")
    public ApiResponse<TeamMemberResponse> transfer(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @Valid @RequestBody TransferTeamMemberRequest request
    ) {
        return ApiResponse.success(
                teamMemberService.transfer(workspaceId, teamId, memberId, request),
                "Điều chuyển nhân sự thành công"
        );
    }

}
