package com.workmanagement.backend.security.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.dto.request.CreatePermissionRequest;
import com.workmanagement.backend.security.dto.request.UpdatePermissionRequest;
import com.workmanagement.backend.security.dto.response.PermissionResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.mapper.PermissionMapper;
import com.workmanagement.backend.security.repository.PermissionRepository;
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
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void findAll_shouldReturnAllPermissions() {
        Permission permission = Permission.builder().id(1L).code("user:read").name("Read").module("user").build();
        PermissionResponse response = PermissionResponse.builder().id(1L).code("user:read").build();

        when(permissionRepository.findAll()).thenReturn(List.of(permission));
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        List<PermissionResponse> result = permissionService.findAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCode()).isEqualTo("user:read");
    }

    @Test
    void findAll_shouldFilterByModule() {
        Permission permission = Permission.builder().id(1L).code("user:read").module("user").build();
        when(permissionRepository.findByModule("user")).thenReturn(List.of(permission));
        when(permissionMapper.toResponse(permission)).thenReturn(PermissionResponse.builder().code("user:read").build());

        List<PermissionResponse> result = permissionService.findAll("user");

        assertThat(result).hasSize(1);
        verify(permissionRepository).findByModule("user");
    }

    @Test
    void create_shouldSavePermission() {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setCode("user:create");
        request.setName("Create user");
        request.setModule("user");

        Permission saved = Permission.builder().id(1L).code("user:create").name("Create user").module("user").build();
        PermissionResponse response = PermissionResponse.builder().id(1L).code("user:create").build();

        when(permissionRepository.existsByCode("user:create")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(saved);
        when(permissionMapper.toResponse(saved)).thenReturn(response);

        PermissionResponse result = permissionService.create(request);

        assertThat(result.getCode()).isEqualTo("user:create");
    }

    @Test
    void create_shouldThrowWhenCodeExists() {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setCode("user:read");

        when(permissionRepository.existsByCode("user:read")).thenReturn(true);

        assertThatThrownBy(() -> permissionService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_ALREADY_EXISTS);
    }

    @Test
    void update_shouldUpdateFields() {
        Permission permission = Permission.builder().id(1L).code("user:read").name("Old").module("user").build();
        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setName("New name");

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(permissionRepository.save(permission)).thenReturn(permission);
        when(permissionMapper.toResponse(permission))
                .thenReturn(PermissionResponse.builder().id(1L).name("New name").build());

        PermissionResponse result = permissionService.update(1L, request);

        assertThat(result.getName()).isEqualTo("New name");
        assertThat(permission.getName()).isEqualTo("New name");
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(permissionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> permissionService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_NOT_FOUND);
    }

    @Test
    void findAllByIds_shouldThrowWhenAnyMissing() {
        when(permissionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                Permission.builder().id(1L).code("a").build()
        ));

        assertThatThrownBy(() -> permissionService.findAllByIds(List.of(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_NOT_FOUND);
    }

}
