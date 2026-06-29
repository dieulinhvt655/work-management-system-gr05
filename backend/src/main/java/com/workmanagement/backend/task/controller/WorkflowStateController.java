package com.workmanagement.backend.task.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.task.dto.request.CreateWorkflowStateRequest;
import com.workmanagement.backend.task.dto.request.UpdateWorkflowStateRequest;
import com.workmanagement.backend.task.dto.response.WorkflowStateResponse;
import com.workmanagement.backend.task.service.WorkflowStateService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/workflow/states")
@RequiredArgsConstructor
public class WorkflowStateController {

    private final WorkflowStateService workflowStateService;

    /** UC-5.0 — Danh sách trạng thái workflow */
    @GetMapping
    public ApiResponse<List<WorkflowStateResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(workflowStateService.findAll(workspaceId, teamId, projectId));
    }

    /** UC-5.0 — Tạo trạng thái workflow */
    @PostMapping
    public ApiResponse<WorkflowStateResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateWorkflowStateRequest request
    ) {
        return ApiResponse.success(
                workflowStateService.create(workspaceId, teamId, projectId, request),
                "Tạo trạng thái workflow thành công"
        );
    }

    /** UC-5.0 — Cập nhật trạng thái workflow */
    @PutMapping("/{stateId}")
    public ApiResponse<WorkflowStateResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long stateId,
            @Valid @RequestBody UpdateWorkflowStateRequest request
    ) {
        return ApiResponse.success(
                workflowStateService.update(workspaceId, teamId, projectId, stateId, request),
                "Cập nhật trạng thái workflow thành công"
        );
    }

    /** UC-5.0 — Xóa trạng thái workflow */
    @DeleteMapping("/{stateId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long stateId
    ) {
        workflowStateService.delete(workspaceId, teamId, projectId, stateId);
        return ApiResponse.success(null, "Xóa trạng thái workflow thành công");
    }

}
