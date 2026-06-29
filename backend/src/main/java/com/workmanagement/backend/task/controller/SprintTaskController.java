package com.workmanagement.backend.task.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.task.dto.request.AssignTaskRequest;
import com.workmanagement.backend.task.dto.request.CreateTaskRequest;
import com.workmanagement.backend.task.dto.request.UpdateTaskProgressRequest;
import com.workmanagement.backend.task.dto.request.RejectTaskRequest;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints/{sprintId}/tasks")
@RequiredArgsConstructor
public class SprintTaskController {

    private final TaskService taskService;

    /** UC-5.3 — Danh sách task trong sprint */
    @GetMapping
    public ApiResponse<List<TaskResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId
    ) {
        return ApiResponse.success(taskService.findSprintTasks(workspaceId, teamId, projectId, sprintId));
    }

    /** UC-5.3 — Chi tiết task trong sprint */
    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(taskService.findSprintTaskById(workspaceId, teamId, projectId, sprintId, taskId));
    }

    /** UC-5.3 — Tạo task trong sprint */
    @PostMapping("/pbi/{itemId}")
    public ApiResponse<TaskResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long itemId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return ApiResponse.success(
                taskService.createSprintTask(workspaceId, teamId, projectId, sprintId, itemId, request),
                "Tạo task sprint thành công"
        );
    }

    /** UC-5.3 — Cập nhật task sprint */
    @PutMapping("/{taskId}")
    public ApiResponse<TaskResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ApiResponse.success(
                taskService.updateSprintTask(workspaceId, teamId, projectId, sprintId, taskId, request),
                "Cập nhật task thành công"
        );
    }

    /** UC-5.3 — Xóa task sprint */
    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId
    ) {
        taskService.deleteSprintTask(workspaceId, teamId, projectId, sprintId, taskId);
        return ApiResponse.success(null, "Xóa task thành công");
    }

    /** UC-5.3 — Kích hoạt task chuẩn bị vào sprint */
    @PostMapping("/pbi/{itemId}/activate/{taskId}")
    public ApiResponse<TaskResponse> activate(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long itemId,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(
                taskService.activatePreparationTaskInSprint(workspaceId, teamId, projectId, sprintId, itemId, taskId),
                "Kích hoạt task vào sprint thành công"
        );
    }

    /** UC-5.4 — Xác nhận phân công */
    @PatchMapping("/{taskId}/confirm-assignment")
    public ApiResponse<TaskResponse> confirmAssignment(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId,
            @Valid @RequestBody AssignTaskRequest request
    ) {
        return ApiResponse.success(
                taskService.confirmAssignment(workspaceId, teamId, projectId, sprintId, taskId, request),
                "Xác nhận phân công thành công"
        );
    }

    /** UC-5.5 — Bắt đầu thực hiện */
    @PatchMapping("/{taskId}/start")
    public ApiResponse<TaskResponse> start(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(
                taskService.startWork(workspaceId, teamId, projectId, sprintId, taskId),
                "Bắt đầu task thành công"
        );
    }

    /** UC-5.5 — Cập nhật tiến độ */
    @PatchMapping("/{taskId}/progress")
    public ApiResponse<TaskResponse> updateProgress(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskProgressRequest request
    ) {
        return ApiResponse.success(
                taskService.updateProgress(workspaceId, teamId, projectId, sprintId, taskId, request),
                "Cập nhật tiến độ thành công"
        );
    }

    /** UC-5.7 — Phê duyệt công việc */
    @PatchMapping("/{taskId}/approve")
    public ApiResponse<TaskResponse> approve(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(
                taskService.approveTask(workspaceId, teamId, projectId, sprintId, taskId),
                "Phê duyệt task thành công"
        );
    }

    /** UC-5.7 — Từ chối và mở lại công việc */
    @PatchMapping("/{taskId}/reject")
    public ApiResponse<TaskResponse> reject(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long taskId,
            @RequestBody(required = false) RejectTaskRequest request
    ) {
        return ApiResponse.success(
                taskService.rejectTask(workspaceId, teamId, projectId, sprintId, taskId, request),
                "Từ chối task thành công"
        );
    }

}
