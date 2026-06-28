package com.workmanagement.backend.security.controller;

import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.security.dto.request.AssignPermissionsRequest;
import com.workmanagement.backend.security.dto.request.CreateRoleRequest;
import com.workmanagement.backend.security.dto.request.UpdateRoleRequest;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.service.RoleService;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleResponse>> findAll(@RequestParam(required = false) RoleScope scope) {
        return ApiResponse.success(roleService.findAll(scope));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(roleService.findById(id));
    }

    @PostMapping
    public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success(roleService.create(request), "Tạo vai trò thành công");
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(roleService.update(id, request), "Cập nhật vai trò thành công");
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<RoleResponse> assignPermissions(
            @PathVariable Long id,
            @Valid @RequestBody AssignPermissionsRequest request
    ) {
        return ApiResponse.success(roleService.assignPermissions(id, request), "Gán quyền thành công");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success(null, "Xóa vai trò thành công");
    }

}
