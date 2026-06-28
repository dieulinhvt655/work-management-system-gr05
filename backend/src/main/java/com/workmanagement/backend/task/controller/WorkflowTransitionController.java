package com.workmanagement.backend.task.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.task.dto.request.CreateWorkflowTransitionRequest;
import com.workmanagement.backend.task.dto.request.UpdateWorkflowTransitionRequest;
import com.workmanagement.backend.task.dto.response.WorkflowTransitionResponse;
import com.workmanagement.backend.task.service.WorkflowTransitionService;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/workflow/transitions")
@RequiredArgsConstructor
public class WorkflowTransitionController {

    private final WorkflowTransitionService workflowTransitionService;

    @GetMapping
    public ApiResponse<List<WorkflowTransitionResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(workflowTransitionService.findAll(workspaceId, teamId, projectId));
    }

    @PostMapping
    public ApiResponse<WorkflowTransitionResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateWorkflowTransitionRequest request
    ) {
        return ApiResponse.success(
                workflowTransitionService.create(workspaceId, teamId, projectId, request),
                "Tạo transition thành công"
        );
    }

    @PutMapping("/{transitionId}")
    public ApiResponse<WorkflowTransitionResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long transitionId,
            @Valid @RequestBody UpdateWorkflowTransitionRequest request
    ) {
        return ApiResponse.success(
                workflowTransitionService.update(workspaceId, teamId, projectId, transitionId, request),
                "Cập nhật transition thành công"
        );
    }

    @DeleteMapping("/{transitionId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long transitionId
    ) {
        workflowTransitionService.delete(workspaceId, teamId, projectId, transitionId);
        return ApiResponse.success(null, "Xóa transition thành công");
    }

}
