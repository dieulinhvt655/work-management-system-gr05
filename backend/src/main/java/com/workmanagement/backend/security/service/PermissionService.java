package com.workmanagement.backend.security.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.dto.request.AssignPermissionsRequest;
import com.workmanagement.backend.security.dto.request.CreatePermissionRequest;
import com.workmanagement.backend.security.dto.request.UpdatePermissionRequest;
import com.workmanagement.backend.security.dto.response.PermissionResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.mapper.PermissionMapper;
import com.workmanagement.backend.security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('permission:read')")
    public List<PermissionResponse> findAll(String module) {
        List<Permission> permissions = module != null && !module.isBlank()
                ? permissionRepository.findByModule(module)
                : permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('permission:read')")
    public PermissionResponse findById(Long id) {
        return permissionMapper.toResponse(getPermission(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('permission:create')")
    public PermissionResponse create(CreatePermissionRequest request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ErrorCode.PERMISSION_ALREADY_EXISTS, "Mã quyền đã tồn tại");
        }

        Permission permission = Permission.builder()
                .code(request.getCode())
                .name(request.getName())
                .module(request.getModule())
                .description(request.getDescription())
                .build();

        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Transactional
    @PreAuthorize("hasAuthority('permission:update')")
    public PermissionResponse update(Long id, UpdatePermissionRequest request) {
        Permission permission = getPermission(id);

        if (request.getName() != null) {
            permission.setName(request.getName());
        }
        if (request.getModule() != null) {
            permission.setModule(request.getModule());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }

        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Transactional
    @PreAuthorize("hasAuthority('permission:delete')")
    public void delete(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND, "Không tìm thấy quyền");
        }
        permissionRepository.deleteById(id);
    }

    Permission getPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_NOT_FOUND, "Không tìm thấy quyền"));
    }

    List<Permission> findAllByIds(List<Long> ids) {
        List<Permission> permissions = permissionRepository.findAllById(ids);
        if (permissions.size() != ids.size()) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND, "Một hoặc nhiều quyền không tồn tại");
        }
        return permissions;
    }

}
