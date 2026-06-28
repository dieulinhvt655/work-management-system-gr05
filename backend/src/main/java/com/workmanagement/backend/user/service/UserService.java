package com.workmanagement.backend.user.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.user.dto.request.CreateUserRequest;
import com.workmanagement.backend.user.dto.request.UpdateProfileRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserRoleRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserStatusRequest;
import com.workmanagement.backend.user.dto.response.UserResponse;
import com.workmanagement.backend.user.dto.response.UserRoleResponse;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.mapper.UserMapper;
import com.workmanagement.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** UC-1.4 — Xem hồ sơ cá nhân */
    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        return toDetailResponse(getCurrentUser());
    }

    /** UC-1.4 — Cập nhật hồ sơ cá nhân */
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());

        if (StringUtils.hasText(request.getNewPassword())) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng nhập mật khẩu hiện tại");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu hiện tại không đúng");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        return toDetailResponse(userRepository.save(user));
    }

    /** UC-1.5 — Tạo tài khoản người dùng */
    @Transactional
    @PreAuthorize("hasAuthority('user:create')")
    public UserResponse create(CreateUserRequest request) {
        validateUniqueEmail(request.getEmail(), null);
        validateUniqueUsername(request.getUsername(), null);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .build();

        if (request.getRoleId() != null) {
            Role role = roleService.getRole(request.getRoleId());
            if (role.getScope() != RoleScope.SYSTEM) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ có thể gán vai trò hệ thống (SYSTEM scope)");
            }
            user.setRole(role);
        }

        return toDetailResponse(userRepository.save(user));
    }

    /** UC-1.6 — Xem danh sách tài khoản */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:read')")
    public PageResponse<UserResponse> findAll(int page, int size, String keyword, UserStatus status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<User> spec = buildSearchSpec(keyword, status);

        Page<User> result = userRepository.findAll(spec, pageable);

        return PageResponse.<UserResponse>builder()
                .items(result.getContent().stream().map(userMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** UC-1.6 — Xem chi tiết tài khoản */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:read')")
    public UserResponse findById(Long id) {
        return toDetailResponse(getUser(id));
    }

    /** UC-1.7 — Cập nhật thông tin tài khoản */
    @Transactional
    @PreAuthorize("hasAuthority('user:update')")
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getUser(id);

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            validateUniqueEmail(request.getEmail(), id);
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
            validateUniqueUsername(request.getUsername(), id);
            user.setUsername(request.getUsername());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return toDetailResponse(userRepository.save(user));
    }

    /** UC-1.8 — Khoá / mở khoá tài khoản */
    @Transactional
    @PreAuthorize("hasAuthority('user:lock')")
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        if (request.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể đặt trạng thái DELETED qua API này");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể thay đổi trạng thái tài khoản của chính mình");
        }

        User user = getUser(id);
        user.setStatus(request.getStatus());

        return toDetailResponse(userRepository.save(user));
    }

    /** UC-1.9 — Gán vai trò hệ thống cho người dùng */
    @Transactional
    @PreAuthorize("hasAuthority('user:assign-role')")
    public UserRoleResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = getUser(userId);

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

    private User getCurrentUser() {
        return getUser(SecurityUtils.getCurrentUserId());
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private UserResponse toDetailResponse(User user) {
        return userMapper.toResponse(user, loadPermissions(user));
    }

    private List<Permission> loadPermissions(User user) {
        if (user.getRole() == null) {
            return List.of();
        }
        return rolePermissionRepository.findPermissionsByRoleId(user.getRole().getId());
    }

    private void validateUniqueEmail(String email, Long excludeId) {
        boolean exists = excludeId == null
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được đăng ký");
        }
    }

    private void validateUniqueUsername(String username, Long excludeId) {
        boolean exists = excludeId == null
                ? userRepository.existsByUsername(username)
                : userRepository.existsByUsernameAndIdNot(username, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "Username đã được sử dụng");
        }
    }

    private Specification<User> buildSearchSpec(String keyword, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), UserStatus.DELETED));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
