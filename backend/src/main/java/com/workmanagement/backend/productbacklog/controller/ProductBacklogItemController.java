package com.workmanagement.backend.productbacklog.controller;

import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.PbiType;
import com.workmanagement.backend.common.enums.PriorityLevel;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.productbacklog.dto.request.CreateProductBacklogItemRequest;
import com.workmanagement.backend.productbacklog.dto.request.UpdateProductBacklogItemRequest;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.productbacklog.service.ProductBacklogItemService;
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

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/backlog/items")
@RequiredArgsConstructor
public class ProductBacklogItemController {

    private final ProductBacklogItemService productBacklogItemService;

    /** UC-4.1 — Tạo PBI */
    @PostMapping
    public ApiResponse<ProductBacklogItemResponse> create(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProductBacklogItemRequest request
    ) {
        return ApiResponse.success(
                productBacklogItemService.create(workspaceId, teamId, projectId, request),
                "Tạo PBI thành công"
        );
    }

    /** UC-4.4, UC-4.10 — Danh sách / tìm kiếm / lọc PBI */
    @GetMapping
    public ApiResponse<PageResponse<ProductBacklogItemResponse>> findAll(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PbiStatus status,
            @RequestParam(required = false) PbiType type,
            @RequestParam(required = false) PriorityLevel priority
    ) {
        return ApiResponse.success(productBacklogItemService.findAll(
                workspaceId, teamId, projectId, page, size, keyword, status, type, priority
        ));
    }

    /** UC-4.4 — Chi tiết PBI */
    @GetMapping("/{itemId}")
    public ApiResponse<ProductBacklogItemResponse> findById(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId
    ) {
        return ApiResponse.success(productBacklogItemService.findById(workspaceId, teamId, projectId, itemId));
    }

    /** UC-4.2 — Cập nhật PBI */
    @PutMapping("/{itemId}")
    public ApiResponse<ProductBacklogItemResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateProductBacklogItemRequest request
    ) {
        return ApiResponse.success(
                productBacklogItemService.update(workspaceId, teamId, projectId, itemId, request),
                "Cập nhật PBI thành công"
        );
    }

    /** UC-4.3 — Xóa PBI */
    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId
    ) {
        productBacklogItemService.delete(workspaceId, teamId, projectId, itemId);
        return ApiResponse.success(null, "Xóa PBI thành công");
    }

    /** UC-4.9 — Xác nhận PBI sẵn sàng triển khai */
    @PatchMapping("/{itemId}/ready")
    public ApiResponse<ProductBacklogItemResponse> markReady(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @PathVariable Long itemId
    ) {
        return ApiResponse.success(
                productBacklogItemService.markReady(workspaceId, teamId, projectId, itemId),
                "Xác nhận PBI sẵn sàng triển khai thành công"
        );
    }

}
