package com.workmanagement.backend.user.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.user.dto.request.UpdateUserRoleRequest;
import com.workmanagement.backend.user.dto.response.UserRoleResponse;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUserRole_shouldAssignSystemRole() {
        User user = User.builder().id(1L).email("user@test.com").build();
        Role systemRole = Role.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();
        RoleResponse roleResponse = RoleResponse.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleService.getRole(1L)).thenReturn(systemRole);
        when(roleService.findById(1L)).thenReturn(roleResponse);

        UserRoleResponse result = userService.updateUserRole(1L, request);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getRole().getName()).isEqualTo("System Admin");
        assertThat(user.getRole()).isEqualTo(systemRole);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRole_shouldRejectNonSystemRole() {
        User user = User.builder().id(1L).build();
        Role workspaceRole = Role.builder().id(2L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build();

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleService.getRole(2L)).thenReturn(workspaceRole);

        assertThatThrownBy(() -> userService.updateUserRole(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateUserRole_shouldThrowWhenUserNotFound() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(1L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(99L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

}
