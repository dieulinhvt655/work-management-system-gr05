package com.workmanagement.backend.user.controller;

import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.user.dto.request.UpdateUserRoleRequest;
import com.workmanagement.backend.user.dto.response.UserRoleResponse;
import com.workmanagement.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** UC-1.9 — Cập nhật vai trò người dùng */
    @PatchMapping("/{id}/role")
    public ApiResponse<UserRoleResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ApiResponse.success(userService.updateUserRole(id, request), "Cập nhật vai trò thành công");
    }

}
