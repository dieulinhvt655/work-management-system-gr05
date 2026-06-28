package com.workmanagement.backend.sprint.controller;

import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.sprint.dto.request.CompleteSprintRequest;
import com.workmanagement.backend.sprint.dto.request.CreateSprintRequest;
import com.workmanagement.backend.sprint.dto.request.UpdateSprintRequest;
import com.workmanagement.backend.sprint.dto.response.SprintProgressResponse;
import com.workmanagement.backend.sprint.dto.response.SprintResponse;
import com.workmanagement.backend.sprint.service.SprintService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    /** UC-5.1 — Tạo sprint */
    @PostMapping
    public ApiResponse<SprintResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateSprintRequest request
    ) {
        return ApiResponse.success(
                sprintService.create(workspaceId, teamId, projectId, request),
                "Tạo sprint thành công"
        );
    }

    /** UC-5.1 — Danh sách sprint */
    @GetMapping
    public ApiResponse<PageResponse<SprintResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SprintStatus status
    ) {
        return ApiResponse.success(sprintService.findAll(workspaceId, teamId, projectId, page, size, status));
    }

    /** UC-5.9 — Lịch sử sprint */
    @GetMapping("/history")
    public ApiResponse<List<SprintResponse>> findHistory(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(sprintService.findHistory(workspaceId, teamId, projectId));
    }

    /** UC-5.1 — Chi tiết sprint */
    @GetMapping("/{sprintId}")
    public ApiResponse<SprintResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId
    ) {
        return ApiResponse.success(sprintService.findById(workspaceId, teamId, projectId, sprintId));
    }

    /** UC-5.1 — Cập nhật sprint */
    @PutMapping("/{sprintId}")
    public ApiResponse<SprintResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @Valid @RequestBody UpdateSprintRequest request
    ) {
        return ApiResponse.success(
                sprintService.update(workspaceId, teamId, projectId, sprintId, request),
                "Cập nhật sprint thành công"
        );
    }

    /** UC-5.1 — Kích hoạt sprint */
    @PatchMapping("/{sprintId}/start")
    public ApiResponse<SprintResponse> start(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId
    ) {
        return ApiResponse.success(
                sprintService.start(workspaceId, teamId, projectId, sprintId),
                "Kích hoạt sprint thành công"
        );
    }

    /** UC-5.1 — Hủy sprint */
    @PatchMapping("/{sprintId}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId
    ) {
        sprintService.cancel(workspaceId, teamId, projectId, sprintId);
        return ApiResponse.success(null, "Hủy sprint thành công");
    }

    /** UC-5.8 — Tổng kết sprint */
    @PatchMapping("/{sprintId}/complete")
    public ApiResponse<SprintResponse> complete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @Valid @RequestBody CompleteSprintRequest request
    ) {
        return ApiResponse.success(
                sprintService.complete(workspaceId, teamId, projectId, sprintId, request),
                "Tổng kết sprint thành công"
        );
    }

    /** UC-5.6 — Theo dõi tiến độ sprint */
    @GetMapping("/{sprintId}/progress")
    public ApiResponse<SprintProgressResponse> getProgress(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId
    ) {
        return ApiResponse.success(sprintService.getProgress(workspaceId, teamId, projectId, sprintId));
    }

    /** UC-5.2 — Danh sách PBI trong sprint */
    @GetMapping("/{sprintId}/pbis")
    public ApiResponse<List<ProductBacklogItemResponse>> listPbis(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId
    ) {
        return ApiResponse.success(sprintService.listPbis(workspaceId, teamId, projectId, sprintId));
    }

    /** UC-5.2 — Thêm PBI vào sprint */
    @PostMapping("/{sprintId}/pbis/{itemId}")
    public ApiResponse<ProductBacklogItemResponse> addPbi(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long itemId
    ) {
        return ApiResponse.success(
                sprintService.addPbi(workspaceId, teamId, projectId, sprintId, itemId),
                "Thêm PBI vào sprint thành công"
        );
    }

    /** UC-5.2 — Gỡ PBI khỏi sprint */
    @DeleteMapping("/{sprintId}/pbis/{itemId}")
    public ApiResponse<Void> removePbi(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long itemId
    ) {
        sprintService.removePbi(workspaceId, teamId, projectId, sprintId, itemId);
        return ApiResponse.success(null, "Gỡ PBI khỏi sprint thành công");
    }

}
