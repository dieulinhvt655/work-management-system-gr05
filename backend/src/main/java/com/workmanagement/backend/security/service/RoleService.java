package com.workmanagement.backend.security.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.dto.request.AssignPermissionsRequest;
import com.workmanagement.backend.security.dto.request.CreateRoleRequest;
import com.workmanagement.backend.security.dto.request.UpdateRoleRequest;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.entity.RolePermission;
import com.workmanagement.backend.security.mapper.RoleMapper;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionService permissionService;
    private final RoleMapper roleMapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('role:read')")
    public List<RoleResponse> findAll(RoleScope scope) {
        List<Role> roles = scope != null
                ? roleRepository.findByScope(scope)
                : roleRepository.findAll();
        return roles.stream().map(this::toResponseWithPermissions).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('role:read')")
    public RoleResponse findById(Long id) {
        return toResponseWithPermissions(getRole(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('role:create')")
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByNameAndScope(request.getName(), request.getScope())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vai trò đã tồn tại trong scope này");
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .scope(request.getScope())
                .build();
        role = roleRepository.save(role);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            replacePermissions(role, request.getPermissionIds());
        }

        return toResponseWithPermissions(role);
    }

    @Transactional
    @PreAuthorize("hasAuthority('role:update')")
    public RoleResponse update(Long id, UpdateRoleRequest request) {
        Role role = getRole(id);

        if (request.getName() != null) {
            RoleScope scope = request.getScope() != null ? request.getScope() : role.getScope();
            if (!request.getName().equals(role.getName()) || scope != role.getScope()) {
                if (roleRepository.existsByNameAndScope(request.getName(), scope)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vai trò đã tồn tại trong scope này");
                }
            }
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getScope() != null) {
            role.setScope(request.getScope());
        }
        roleRepository.save(role);

        if (request.getPermissionIds() != null) {
            replacePermissions(role, request.getPermissionIds());
        }

        return toResponseWithPermissions(role);
    }

    @Transactional
    @PreAuthorize("hasAuthority('role:delete')")
    public void delete(Long id) {
        Role role = getRole(id);
        if (userRepository.existsByRole_Id(role.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể xóa vai trò đang được gán cho người dùng");
        }
        rolePermissionRepository.deleteByRole_Id(role.getId());
        roleRepository.delete(role);
    }

    /** UC-1.9 — Gán / thay thế toàn bộ permission cho role */
    @Transactional
    @PreAuthorize("hasAuthority('role:assign-permission')")
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = getRole(roleId);
        replacePermissions(role, request.getPermissionIds());
        return toResponseWithPermissions(role);
    }

    public Role getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò"));
    }

    private void replacePermissions(Role role, List<Long> permissionIds) {
        rolePermissionRepository.deleteByRole_Id(role.getId());
        List<Permission> permissions = permissionService.findAllByIds(permissionIds);
        for (Permission permission : permissions) {
            rolePermissionRepository.save(new RolePermission(role, permission));
        }
    }

    private RoleResponse toResponseWithPermissions(Role role) {
        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(role.getId());
        return roleMapper.toResponse(role, permissions);
    }

}
