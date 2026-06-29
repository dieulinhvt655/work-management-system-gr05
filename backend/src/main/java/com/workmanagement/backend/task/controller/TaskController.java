package com.workmanagement.backend.task.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.task.dto.request.AssignTaskRequest;
import com.workmanagement.backend.task.dto.request.CreateTaskRequest;
import com.workmanagement.backend.task.dto.request.UpdateTaskRequest;
import com.workmanagement.backend.task.dto.response.TaskResponse;
import com.workmanagement.backend.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/backlog/items/{itemId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** UC-4.5 — Tạo task chuẩn bị từ PBI */
    @PostMapping
    public ApiResponse<TaskResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return ApiResponse.success(
                taskService.createPreparationTask(workspaceId, teamId, projectId, itemId, request),
                "Tạo task thành công"
        );
    }

    /** UC-4.5 — Danh sách task chuẩn bị */
    @GetMapping
    public ApiResponse<List<TaskResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId
    ) {
        return ApiResponse.success(taskService.findPreparationTasks(workspaceId, teamId, projectId, itemId));
    }

    /** UC-4.5 — Chi tiết task chuẩn bị */
    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(taskService.findPreparationTaskById(workspaceId, teamId, projectId, itemId, taskId));
    }

    /** UC-4.6 — Cập nhật task chuẩn bị */
    @PutMapping("/{taskId}")
    public ApiResponse<TaskResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ApiResponse.success(
                taskService.updatePreparationTask(workspaceId, teamId, projectId, itemId, taskId, request),
                "Cập nhật task thành công"
        );
    }

    /** UC-4.7 — Xóa task chuẩn bị */
    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @PathVariable Long taskId
    ) {
        taskService.deletePreparationTask(workspaceId, teamId, projectId, itemId, taskId);
        return ApiResponse.success(null, "Xóa task thành công");
    }

    /** UC-4.8 — Gán thành viên dự kiến */
    @PatchMapping("/{taskId}/assign")
    public ApiResponse<TaskResponse> assign(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @PathVariable Long taskId,
            @Valid @RequestBody AssignTaskRequest request
    ) {
        return ApiResponse.success(
                taskService.assignPreparationTask(workspaceId, teamId, projectId, itemId, taskId, request),
                "Gán thành viên thành công"
        );
    }

}
