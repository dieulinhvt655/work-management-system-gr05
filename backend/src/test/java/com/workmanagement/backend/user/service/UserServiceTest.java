package com.workmanagement.backend.user.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.dto.response.RoleResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private Role systemRole;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        systemRole = Role.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();
        activeUser = User.builder()
                .id(1L)
                .fullName("Admin")
                .email("admin@test.com")
                .username("admin")
                .passwordHash("encoded")
                .status(UserStatus.ACTIVE)
                .role(systemRole)
                .build();
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void getCurrentProfile_shouldReturnCurrentUser() {
        UserResponse expected = UserResponse.builder().id(1L).email("admin@test.com").build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(expected);

        UserResponse result = userService.getCurrentProfile();

        assertThat(result.getEmail()).isEqualTo("admin@test.com");
    }

    @Test
    void updateProfile_shouldUpdateFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");
        request.setPhone("0901234567");

        UserResponse expected = UserResponse.builder().id(1L).fullName("New Name").build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(expected);

        UserResponse result = userService.updateProfile(request);

        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(activeUser.getFullName()).isEqualTo("New Name");
        assertThat(activeUser.getPhone()).isEqualTo("0901234567");
    }

    @Test
    void updateProfile_shouldChangePasswordWhenCurrentPasswordValid() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Admin");
        request.setCurrentPassword("oldpass");
        request.setNewPassword("newpass123");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldpass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("new-encoded");
        when(userRepository.save(activeUser)).thenReturn(activeUser);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(UserResponse.builder().id(1L).build());

        userService.updateProfile(request);

        assertThat(activeUser.getPasswordHash()).isEqualTo("new-encoded");
    }

    @Test
    void create_shouldCreateUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("New User");
        request.setEmail("new@test.com");
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setRoleId(1L);

        User saved = User.builder().id(2L).fullName("New User").email("new@test.com").username("newuser").role(systemRole).build();
        UserResponse expected = UserResponse.builder().id(2L).email("new@test.com").build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(roleService.getRole(1L)).thenReturn(systemRole);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(saved, List.of())).thenReturn(expected);

        UserResponse result = userService.create(request);

        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnPagedUsers() {
        UserResponse item = UserResponse.builder().id(1L).build();
        PageImpl<User> page = new PageImpl<>(List.of(activeUser));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponse(activeUser)).thenReturn(item);

        PageResponse<UserResponse> result = userService.findAll(0, 20, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_shouldUpdateUserInfo() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setPhone("0999888777");

        UserResponse expected = UserResponse.builder().id(1L).fullName("Updated Name").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(expected);

        UserResponse result = userService.update(1L, request);

        assertThat(result.getFullName()).isEqualTo("Updated Name");
        assertThat(activeUser.getPhone()).isEqualTo("0999888777");
    }

    @Test
    void updateStatus_shouldChangeStatus() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        UserResponse expected = UserResponse.builder().id(2L).status(UserStatus.INACTIVE).build();
        User target = User.builder().id(2L).status(UserStatus.ACTIVE).role(systemRole).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(target, List.of())).thenReturn(expected);

        UserResponse result = userService.updateStatus(2L, request);

        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(target.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void updateStatus_shouldRejectSelfLock() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateUserRole_shouldAssignSystemRole() {
        User user = User.builder().id(1L).email("user@test.com").build();
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

    @Test
    void create_shouldThrowWhenEmailExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("exists@test.com");
        request.setUsername("user");
        request.setFullName("User");
        request.setPassword("pass123");

        when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
    }

}
