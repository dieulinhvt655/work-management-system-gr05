package com.workmanagement.backend.user.controller;

import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.user.dto.request.CreateUserRequest;
import com.workmanagement.backend.user.dto.request.UpdateProfileRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserRoleRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserStatusRequest;
import com.workmanagement.backend.user.dto.response.UserResponse;
import com.workmanagement.backend.user.dto.response.UserRoleResponse;
import com.workmanagement.backend.user.service.UserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** UC-1.4 — Xem hồ sơ cá nhân */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentProfile() {
        return ApiResponse.success(userService.getCurrentProfile());
    }

    /** UC-1.4 — Cập nhật hồ sơ cá nhân */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(request), "Cập nhật hồ sơ thành công");
    }

    /** UC-1.5 — Tạo tài khoản người dùng */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.create(request), "Tạo tài khoản thành công");
    }

    /** UC-1.6 — Xem danh sách tài khoản */
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return ApiResponse.success(userService.findAll(
                page, size, keyword, status, workspaceId, roleId,
                teamId, sortBy, sortDirection
        ));
    }

    /** UC-1.6 — Xem chi tiết tài khoản */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(userService.findById(id));
    }

    /** UC-1.7 — Cập nhật thông tin tài khoản */
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(userService.update(id, request), "Cập nhật tài khoản thành công");
    }

    /** UC-1.8 — Khoá / mở khoá tài khoản */
    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ApiResponse.success(userService.updateStatus(id, request), "Cập nhật trạng thái thành công");
    }

    /** UC-1.9 — Cập nhật vai trò người dùng */
    @PatchMapping("/{id}/role")
    public ApiResponse<UserRoleResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ApiResponse.success(userService.updateUserRole(id, request), "Cập nhật vai trò thành công");
    }

}
