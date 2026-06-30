package com.workmanagement.backend.workspace.controller;

import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.workspace.dto.request.CreateWorkspaceRequest;
import com.workmanagement.backend.workspace.dto.request.UpdateWorkspaceRequest;
import com.workmanagement.backend.workspace.dto.response.WorkspaceResponse;
import com.workmanagement.backend.workspace.service.WorkspaceService;
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
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /** UC-2.1 — Tạo workspace */
    @PostMapping
    public ApiResponse<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return ApiResponse.success(workspaceService.create(request), "Tạo workspace thành công");
    }

    /** UC-2.2 — Danh sách workspace */
    @GetMapping
    public ApiResponse<PageResponse<WorkspaceResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CommonStatus status
    ) {
        return ApiResponse.success(workspaceService.findAll(page, size, keyword, status));
    }

    /** UC-2.2 — Chi tiết workspace */
    @GetMapping("/current")
    public ApiResponse<WorkspaceResponse> findCurrent() {
        return ApiResponse.success(workspaceService.findCurrent());
    }

    /** UC-2.2 — System Admin tra cứu workspace theo id */
    @GetMapping("/{id}")
    public ApiResponse<WorkspaceResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(workspaceService.findById(id));
    }

    /** UC-2.2 — Cập nhật workspace */
    @PutMapping("/{id}")
    public ApiResponse<WorkspaceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        return ApiResponse.success(workspaceService.update(id, request), "Cập nhật workspace thành công");
    }

    /** UC-2.10 — Đóng workspace */
    @PatchMapping("/{id}/close")
    public ApiResponse<WorkspaceResponse> close(@PathVariable Long id) {
        return ApiResponse.success(workspaceService.close(id), "Đóng workspace thành công");
    }

    /** UC-2.10 — System Admin kích hoạt lại workspace */
    @PatchMapping("/{id}/reactivate")
    public ApiResponse<WorkspaceResponse> reactivate(@PathVariable Long id) {
        return ApiResponse.success(workspaceService.reactivate(id), "Kích hoạt lại workspace thành công");
    }

}
