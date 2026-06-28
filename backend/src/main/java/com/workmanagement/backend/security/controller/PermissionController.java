package com.workmanagement.backend.security.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.security.dto.request.CreatePermissionRequest;
import com.workmanagement.backend.security.dto.request.UpdatePermissionRequest;
import com.workmanagement.backend.security.dto.response.PermissionResponse;
import com.workmanagement.backend.security.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<List<PermissionResponse>> findAll(@RequestParam(required = false) String module) {
        return ApiResponse.success(permissionService.findAll(module));
    }

    @GetMapping("/{id}")
    public ApiResponse<PermissionResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(permissionService.findById(id));
    }

    @PostMapping
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request) {
        return ApiResponse.success(permissionService.create(request), "Tạo quyền thành công");
    }

    @PutMapping("/{id}")
    public ApiResponse<PermissionResponse> update(@PathVariable Long id, @RequestBody UpdatePermissionRequest request) {
        return ApiResponse.success(permissionService.update(id, request), "Cập nhật quyền thành công");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success(null, "Xóa quyền thành công");
    }

}
