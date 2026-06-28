package com.workmanagement.backend.security.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.dto.request.AssignPermissionsRequest;
import com.workmanagement.backend.security.dto.request.CreateRoleRequest;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.mapper.RoleMapper;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void create_shouldSaveRoleWithPermissions() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Custom Role");
        request.setDescription("Test");
        request.setScope(RoleScope.WORKSPACE);
        request.setPermissionIds(List.of(1L, 2L));

        Role saved = Role.builder().id(1L).name("Custom Role").scope(RoleScope.WORKSPACE).build();
        List<Permission> permissions = List.of(
                Permission.builder().id(1L).code("user:read").build(),
                Permission.builder().id(2L).code("user:create").build()
        );
        RoleResponse response = RoleResponse.builder().id(1L).name("Custom Role").build();

        when(roleRepository.existsByNameAndScope("Custom Role", RoleScope.WORKSPACE)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(saved);
        when(permissionService.findAllByIds(List.of(1L, 2L))).thenReturn(permissions);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(permissions);
        when(roleMapper.toResponse(saved, permissions)).thenReturn(response);

        RoleResponse result = roleService.create(request);

        assertThat(result.getName()).isEqualTo("Custom Role");
        verify(rolePermissionRepository).deleteByRole_Id(1L);
    }

    @Test
    void create_shouldThrowWhenDuplicateNameInScope() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Existing");
        request.setScope(RoleScope.SYSTEM);

        when(roleRepository.existsByNameAndScope("Existing", RoleScope.SYSTEM)).thenReturn(true);

        assertThatThrownBy(() -> roleService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void assignPermissions_shouldReplaceAllPermissions() {
        Role role = Role.builder().id(1L).name("Admin").scope(RoleScope.SYSTEM).build();
        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(List.of(1L));

        Permission permission = Permission.builder().id(1L).code("role:read").build();
        RoleResponse response = RoleResponse.builder().id(1L).name("Admin").build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionService.findAllByIds(List.of(1L))).thenReturn(List.of(permission));
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of(permission));
        when(roleMapper.toResponse(role, List.of(permission))).thenReturn(response);

        RoleResponse result = roleService.assignPermissions(1L, request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(rolePermissionRepository).deleteByRole_Id(1L);
    }

    @Test
    void delete_shouldThrowWhenRoleAssignedToUser() {
        Role role = Role.builder().id(1L).name("Admin").build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.existsByRole_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> roleService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getRole_shouldThrowWhenNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRole(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }

}
