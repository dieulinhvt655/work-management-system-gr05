package com.workmanagement.backend.user.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.user.dto.request.UpdateUserRoleRequest;
import com.workmanagement.backend.user.dto.response.UserRoleResponse;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;

    /** UC-1.9 — Gán vai trò hệ thống cho người dùng */
    @Transactional
    @PreAuthorize("hasAuthority('user:assign-role')")
    public UserRoleResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));

        Role role = roleService.getRole(request.getRoleId());
        if (role.getScope() != RoleScope.SYSTEM) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ có thể gán vai trò hệ thống (SYSTEM scope)");
        }

        user.setRole(role);
        userRepository.save(user);

        return UserRoleResponse.builder()
                .userId(user.getId())
                .role(roleService.findById(role.getId()))
                .build();
    }

}
