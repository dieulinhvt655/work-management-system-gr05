package com.workmanagement.backend.productbacklog.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.productbacklog.dto.request.UpdateProductBacklogRequest;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogResponse;
import com.workmanagement.backend.productbacklog.service.ProductBacklogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/backlog")
@RequiredArgsConstructor
public class ProductBacklogController {

    private final ProductBacklogService productBacklogService;

    @GetMapping
    public ApiResponse<ProductBacklogResponse> findByProject(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(productBacklogService.findByProject(workspaceId, teamId, projectId));
    }

    @PutMapping
    public ApiResponse<ProductBacklogResponse> update(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProductBacklogRequest request
    ) {
        return ApiResponse.success(
                productBacklogService.update(workspaceId, teamId, projectId, request),
                "Cập nhật backlog thành công"
        );
    }

}
