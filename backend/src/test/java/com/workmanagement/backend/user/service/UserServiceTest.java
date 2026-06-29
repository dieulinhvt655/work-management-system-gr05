package com.workmanagement.backend.user.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.auth.service.RefreshTokenService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
import com.workmanagement.backend.team.dto.response.TeamMemberResponse;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.team.service.TeamMemberService;
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
import com.workmanagement.backend.user.util.EmployeeCodeGenerator;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.repository.WorkspaceRepository;
import com.workmanagement.backend.workspace.service.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberService teamMemberService;
    @Mock
    private EmployeeCodeGenerator employeeCodeGenerator;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private Role systemRole;
    private Role workspaceMemberRole;
    private Role teamMemberRole;
    private Workspace workspace;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        systemRole = Role.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();
        workspaceMemberRole = Role.builder().id(2L).name("Workspace Member").scope(RoleScope.WORKSPACE).build();
        teamMemberRole = Role.builder().id(4L).name("Team Member").scope(RoleScope.TEAM).build();
        workspace = Workspace.builder().id(10L).name("Acme").status(CommonStatus.ACTIVE).build();
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
        lenient().when(employeeCodeGenerator.generateUnique()).thenReturn("123456");
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
        request.setAvatarUrl("https://example.com/avatar.png");
        request.setDescription("Backend developer");

        UserResponse expected = UserResponse.builder().id(1L).fullName("Admin").build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(expected);

        UserResponse result = userService.updateProfile(request);

        assertThat(result.getFullName()).isEqualTo("Admin");
        assertThat(activeUser.getFullName()).isEqualTo("Admin");
        assertThat(activeUser.getPhone()).isEqualTo("0901234567");
        assertThat(activeUser.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(activeUser.getDescription()).isEqualTo("Backend developer");
        verify(activityLogService).recordOrgEvent(
                1L,
                ActivityLogAction.USER_PROFILE_UPDATED,
                ActivityLogAction.TARGET_USER,
                1L,
                "phone,avatarUrl,description"
        );
    }

    @Test
    void updateProfile_shouldClearDescriptionWhenBlank() {
        activeUser.setDescription("Current description");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDescription("   ");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(UserResponse.builder().id(1L).build());

        userService.updateProfile(request);

        assertThat(activeUser.getDescription()).isNull();
        verify(activityLogService).recordOrgEvent(
                1L,
                ActivityLogAction.USER_PROFILE_UPDATED,
                ActivityLogAction.TARGET_USER,
                1L,
                "description"
        );
    }

    @Test
    void updateProfile_shouldChangePasswordWhenCurrentPasswordValid() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Admin");
        request.setCurrentPassword("oldpass");
        request.setNewPassword("Newpass1!");
        request.setConfirmNewPassword("Newpass1!");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldpass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("Newpass1!")).thenReturn("new-encoded");
        when(userRepository.save(activeUser)).thenReturn(activeUser);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(UserResponse.builder().id(1L).build());

        userService.updateProfile(request);

        assertThat(activeUser.getPasswordHash()).isEqualTo("new-encoded");
    }

    @Test
    void updateProfile_shouldOnlyLogActuallyChangedField() {
        activeUser.setPhone("0900000000");
        activeUser.setAvatarUrl("https://example.com/current.png");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("0911111111");
        request.setAvatarUrl("https://example.com/current.png");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(activeUser, List.of())).thenReturn(UserResponse.builder().id(1L).build());

        userService.updateProfile(request);

        verify(activityLogService).recordOrgEvent(
                1L,
                ActivityLogAction.USER_PROFILE_UPDATED,
                ActivityLogAction.TARGET_USER,
                1L,
                "phone"
        );
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateProfile_shouldRejectWhenNothingChangedWithoutSavingOrLogging() {
        activeUser.setPhone("0900000000");
        activeUser.setAvatarUrl("https://example.com/current.png");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone(" 0900000000 ");
        request.setAvatarUrl("https://example.com/current.png");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không có thông tin nào thay đổi")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(userRepository, never()).save(any());
        verify(activityLogService, never()).recordOrgEvent(any(), any(), any(), any(), any());
    }

    @Test
    void updateProfile_shouldRejectWhenNewPasswordEqualsCurrentPassword() {
        UpdateProfileRequest request = passwordChangeRequest("Current1!", "Current1!");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldpass", "encoded")).thenReturn(true);
        when(passwordEncoder.matches("Current1!", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu mới không được trùng với mật khẩu hiện tại")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(activityLogService, never()).recordOrgEvent(any(), any(), any(), any(), any());
    }

    @Test
    void updateProfile_shouldRejectInactiveAccount() {
        activeUser.setStatus(UserStatus.INACTIVE);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.updateProfile(new UpdateProfileRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_INACTIVE);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldRejectInvalidProfileData() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("invalid-phone");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldRejectWrongCurrentPassword() {
        UpdateProfileRequest request = passwordChangeRequest("Newpass1!", "Newpass1!");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldpass", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void updateProfile_shouldRejectMismatchedPasswordConfirmation() {
        UpdateProfileRequest request = passwordChangeRequest("Newpass1!", "Different1!");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldpass", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateProfile_shouldRejectPasswordOutsidePolicy() {
        UpdateProfileRequest request = passwordChangeRequest("weakpass", "weakpass");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("oldpass", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void create_shouldCreateUserAssignWorkspaceAndLogActivity() {
        CreateUserRequest request = validCreateRequest();
        request.setUsername("linh.vuthidieu");

        User saved = User.builder()
                .id(2L)
                .fullName("New User")
                .email("new@test.com")
                .username("linh.vuthidieu")
                .role(workspaceMemberRole)
                .build();
        UserResponse expected = UserResponse.builder().id(2L).email("new@test.com").build();
        WorkspaceMember member = WorkspaceMember.builder().id(20L).workspace(workspace).user(saved).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("linh.vuthidieu")).thenReturn(false);
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(teamRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.of(Team.builder().id(5L).build()));
        when(roleRepository.findByNameAndScope("Workspace Member", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceMemberRole));
        stubTeamAssignment();
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(member);
        when(rolePermissionRepository.findPermissionsByRoleId(2L)).thenReturn(List.of());
        when(userMapper.toResponse(saved, List.of())).thenReturn(expected);

        UserResponse result = userService.create(request);

        assertThat(result.getEmail()).isEqualTo("new@test.com");
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isEqualTo("Linhvuthidieu@123");
        verify(workspaceService).verifyCanManage(workspace);
        verify(activityLogService).recordOrgEvent(
                1L,
                ActivityLogAction.USER_CREATED,
                ActivityLogAction.TARGET_USER,
                2L,
                "new@test.com"
        );
        verify(activityLogService).recordOrgEvent(
                1L,
                ActivityLogAction.WORKSPACE_MEMBER_ADDED,
                ActivityLogAction.TARGET_WORKSPACE_MEMBER,
                20L,
                "New User"
        );
        verify(teamMemberService).add(eq(10L), eq(5L), any(AddTeamMemberRequest.class));
    }

    @Test
    void create_shouldSucceedWithoutDepartment() {
        CreateUserRequest request = validCreateRequest();
        request.setTeamId(null);

        User saved = User.builder().id(2L).email("new@test.com").role(workspaceMemberRole).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("user123456")).thenReturn(false);
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(roleRepository.findByNameAndScope("Workspace Member", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceMemberRole));
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenReturn(WorkspaceMember.builder().id(20L).build());
        when(rolePermissionRepository.findPermissionsByRoleId(2L)).thenReturn(List.of());
        when(userMapper.toResponse(saved, List.of())).thenReturn(UserResponse.builder().id(2L).build());

        userService.create(request);

        verify(teamRepository, never()).findByIdAndWorkspaceId(any(), any());
        verify(teamMemberService, never()).add(any(), any(), any());
    }

    @Test
    void create_shouldUseDefaultRoleWhenRoleMissing() {
        CreateUserRequest request = validCreateRequest();
        request.setRoleId(null);

        User saved = User.builder().id(2L).email("new@test.com").role(workspaceMemberRole).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("user123456")).thenReturn(false);
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(teamRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.of(Team.builder().id(5L).build()));
        when(roleRepository.findByNameAndScope("Workspace Member", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceMemberRole));
        stubTeamAssignment();
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenReturn(WorkspaceMember.builder().id(20L).build());
        when(rolePermissionRepository.findPermissionsByRoleId(2L)).thenReturn(List.of());
        when(userMapper.toResponse(saved, List.of())).thenReturn(UserResponse.builder().id(2L).build());

        userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(workspaceMemberRole);
    }

    @Test
    void create_shouldUseRoleIdWhenProvided() {
        CreateUserRequest request = validCreateRequest();
        request.setRoleId(1L);

        User saved = User.builder().id(2L).email("new@test.com").role(systemRole).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("user123456")).thenReturn(false);
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(teamRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.of(Team.builder().id(5L).build()));
        when(roleService.getRole(1L)).thenReturn(systemRole);
        when(roleRepository.findByNameAndScope("Workspace Member", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceMemberRole));
        stubTeamAssignment();
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenReturn(WorkspaceMember.builder().id(20L).build());
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(saved, List.of())).thenReturn(UserResponse.builder().id(2L).build());

        userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(systemRole);
        verify(roleService).getRole(1L);
        verify(roleRepository).findByNameAndScope("Workspace Member", RoleScope.WORKSPACE);
    }

    @Test
    void create_shouldRejectWorkspaceOwnerCreatingSystemAdmin() {
        CreateUserRequest request = validCreateRequest();
        request.setRoleId(1L);
        activeUser.setRole(Role.builder()
                .id(3L)
                .name("Workspace Owner")
                .scope(RoleScope.WORKSPACE)
                .build());

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("user123456")).thenReturn(false);
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(teamRepository.findByIdAndWorkspaceId(5L, 10L))
                .thenReturn(Optional.of(Team.builder().id(5L).build()));
        when(roleService.getRole(1L)).thenReturn(systemRole);
        when(roleRepository.findByNameAndScope("Workspace Member", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceMemberRole));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_shouldResolveRoleFromRoleKey() {
        CreateUserRequest request = validCreateRequest();
        request.setRole("WORKSPACE_OWNER");
        request.setRoleId(null);

        Role workspaceOwnerRole = Role.builder().id(3L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build();
        User saved = User.builder().id(2L).email("new@test.com").role(workspaceOwnerRole).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("user123456")).thenReturn(false);
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(teamRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.of(Team.builder().id(5L).build()));
        when(roleRepository.findFirstByName("Workspace Owner")).thenReturn(Optional.of(workspaceOwnerRole));
        stubTeamAssignment();
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenReturn(WorkspaceMember.builder().id(20L).build());
        when(rolePermissionRepository.findPermissionsByRoleId(3L)).thenReturn(List.of());
        when(userMapper.toResponse(saved, List.of())).thenReturn(UserResponse.builder().id(2L).build());

        userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(workspaceOwnerRole);
        verify(roleRepository).findFirstByName("Workspace Owner");
    }

    @Test
    void create_shouldRejectTeamOutsideWorkspace() {
        CreateUserRequest request = validCreateRequest();

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(teamRepository.findByIdAndWorkspaceId(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Phòng ban / nhóm không thuộc workspace đã chọn")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(userRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnPagedUsers() {
        UserResponse item = UserResponse.builder().id(1L).build();
        PageImpl<User> page = new PageImpl<>(List.of(activeUser));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponse(activeUser)).thenReturn(item);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        PageResponse<UserResponse> result = userService.findAll(
                0, 20, null, null, null, null, null, "createdAt", "desc");

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAll_shouldRejectRoleWithoutUserScope() {
        activeUser.setRole(teamMemberRole);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.findAll(
                0, 20, null, null, null, null, null, "createdAt", "desc"))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findAll_shouldRejectWorkspaceOutsideOwnerScope() {
        activeUser.setRole(Role.builder()
                .id(3L)
                .name("Workspace Owner")
                .scope(RoleScope.WORKSPACE)
                .build());
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.findAll(
                0, 20, null, null, 99L, null, null, "createdAt", "desc"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findAll_shouldRejectInvalidSortField() {
        assertThatThrownBy(() -> userService.findAll(
                0, 20, null, null, null, null, null, "passwordHash", "asc"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void findById_shouldAllowWorkspaceOwnerForUserInOwnedWorkspace() {
        Role ownerRole = Role.builder()
                .id(3L)
                .name("Workspace Owner")
                .scope(RoleScope.WORKSPACE)
                .build();
        activeUser.setRole(ownerRole);
        User target = User.builder().id(2L).email("member@test.com").role(workspaceMemberRole).build();
        UserResponse expected = UserResponse.builder().id(2L).email("member@test.com").build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());
        when(workspaceMemberRepository.existsByWorkspaceIdInAndUserIdAndStatus(
                Set.of(10L), 2L, MemberStatus.ACTIVE)).thenReturn(true);
        when(rolePermissionRepository.findPermissionsByRoleId(2L)).thenReturn(List.of());
        when(userMapper.toResponse(target, List.of())).thenReturn(expected);

        UserResponse result = userService.findById(2L);

        assertThat(result.getEmail()).isEqualTo("member@test.com");
    }

    @Test
    void findById_shouldRejectWorkspaceOwnerForUserOutsideOwnedWorkspace() {
        activeUser.setRole(Role.builder()
                .id(3L)
                .name("Workspace Owner")
                .scope(RoleScope.WORKSPACE)
                .build());
        User target = User.builder().id(2L).role(workspaceMemberRole).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.findById(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_shouldUpdateUserInfo() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setPhone("0999888777");
        request.setDescription("Updated description");

        User target = User.builder()
                .id(2L)
                .fullName("Old Name")
                .email("target@test.com")
                .username("target")
                .status(UserStatus.ACTIVE)
                .role(workspaceMemberRole)
                .build();
        UserResponse expected = UserResponse.builder().id(2L).fullName("Updated Name").build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);
        when(rolePermissionRepository.findPermissionsByRoleId(2L)).thenReturn(List.of());
        when(userMapper.toResponse(target, List.of())).thenReturn(expected);

        UserResponse result = userService.update(2L, request);

        assertThat(result.getFullName()).isEqualTo("Updated Name");
        assertThat(target.getPhone()).isEqualTo("0999888777");
        assertThat(target.getDescription()).isEqualTo("Updated description");
        verify(userRepository).save(target);
        verify(activityLogService).recordOrgEvent(
                1L,
                ActivityLogAction.USER_UPDATED,
                ActivityLogAction.TARGET_USER,
                2L,
                "fullName,phone,description"
        );
    }

    @Test
    void update_shouldAllowWorkspaceOwnerToUpdateSensitiveFieldsInsideScope() {
        activeUser.setRole(Role.builder()
                .id(3L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build());
        User target = User.builder()
                .id(2L)
                .fullName("Old Name")
                .email("old@test.com")
                .username("olduser")
                .status(UserStatus.ACTIVE)
                .role(workspaceMemberRole)
                .build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Legal New Name");
        request.setEmail(" NEW@TEST.COM ");
        request.setUsername(" NewUser ");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());
        when(workspaceMemberRepository.existsByWorkspaceIdInAndUserIdAndStatus(
                Set.of(10L), 2L, MemberStatus.ACTIVE)).thenReturn(true);
        when(userRepository.save(target)).thenReturn(target);
        when(rolePermissionRepository.findPermissionsByRoleId(2L)).thenReturn(List.of());
        when(userMapper.toResponse(target, List.of())).thenReturn(
                UserResponse.builder().id(2L).fullName("Legal New Name").build());

        userService.update(2L, request);

        assertThat(target.getFullName()).isEqualTo("Legal New Name");
        assertThat(target.getEmail()).isEqualTo("new@test.com");
        assertThat(target.getUsername()).isEqualTo("newuser");
    }

    @Test
    void update_shouldRejectWorkspaceOwnerOutsideScope() {
        activeUser.setRole(Role.builder()
                .id(3L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build());
        User target = User.builder().id(2L).status(UserStatus.ACTIVE).role(workspaceMemberRole).build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Unauthorized Change");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.update(2L, request))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(target);
    }

    @Test
    void updateStatus_shouldChangeStatus() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        UserResponse expected = UserResponse.builder().id(2L).status(UserStatus.INACTIVE).build();
        User target = User.builder().id(2L).status(UserStatus.ACTIVE).role(systemRole).build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(target)).thenReturn(target);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of());
        when(userMapper.toResponse(target, List.of())).thenReturn(expected);

        UserResponse result = userService.updateStatus(2L, request);

        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(target.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(refreshTokenService).revokeAllForUser(2L);
        verify(activityLogService).recordOrgEvent(
                1L,
                ActivityLogAction.USER_LOCKED,
                ActivityLogAction.TARGET_USER,
                2L,
                null
        );
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
    void updateStatus_shouldRejectDeletedStatus() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.DELETED);

        assertThatThrownBy(() -> userService.updateStatus(2L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldRejectNullInputs() {
        assertThatThrownBy(() -> userService.updateStatus(null, new UpdateUserStatusRequest()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> userService.updateStatus(2L, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatus_shouldAllowWorkspaceOwnerForUserInOwnedWorkspace() {
        activeUser.setRole(Role.builder()
                .id(3L)
                .name("Workspace Owner")
                .scope(RoleScope.WORKSPACE)
                .build());
        User target = User.builder()
                .id(2L)
                .email("member@test.com")
                .status(UserStatus.INACTIVE)
                .role(workspaceMemberRole)
                .build();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.ACTIVE);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());
        when(workspaceMemberRepository.existsByWorkspaceIdInAndUserIdAndStatus(
                Set.of(10L), 2L, MemberStatus.ACTIVE)).thenReturn(true);
        when(userRepository.save(target)).thenReturn(target);
        when(rolePermissionRepository.findPermissionsByRoleId(2L)).thenReturn(List.of());
        when(userMapper.toResponse(target, List.of()))
                .thenReturn(UserResponse.builder().id(2L).status(UserStatus.ACTIVE).build());

        UserResponse result = userService.updateStatus(2L, request);

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(refreshTokenService, never()).revokeAllForUser(any());
        verify(activityLogService).recordOrgEvent(
                1L, ActivityLogAction.USER_UNLOCKED, ActivityLogAction.TARGET_USER, 2L, "member@test.com");
    }

    @Test
    void updateStatus_shouldRejectWorkspaceOwnerOutsideScope() {
        activeUser.setRole(Role.builder()
                .id(3L)
                .name("Workspace Owner")
                .scope(RoleScope.WORKSPACE)
                .build());
        User target = User.builder().id(2L).status(UserStatus.ACTIVE).build();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.INACTIVE);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.updateStatus(2L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void updateUserRole_shouldAssignSystemRole() {
        User user = User.builder().id(2L).email("user@test.com").build();
        RoleResponse roleResponse = RoleResponse.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(1L);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(roleService.getRole(1L)).thenReturn(systemRole);
        when(roleService.findById(1L)).thenReturn(roleResponse);

        UserRoleResponse result = userService.updateUserRole(2L, request);

        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getRole().getName()).isEqualTo("System Admin");
        assertThat(user.getRole()).isEqualTo(systemRole);
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(2L);
        verify(activityLogService).recordOrgEvent(
                1L, ActivityLogAction.USER_ROLE_UPDATED, ActivityLogAction.TARGET_USER, 2L, "System Admin");
    }

    @Test
    void updateUserRole_shouldAssignWorkspaceRole() {
        User user = User.builder().id(2L).email("user@test.com").build();
        Role workspaceRole = Role.builder().id(2L).name("Workspace Member").scope(RoleScope.WORKSPACE).build();
        RoleResponse roleResponse = RoleResponse.builder().id(2L).name("Workspace Member").scope(RoleScope.WORKSPACE).build();
        WorkspaceMember membership = WorkspaceMember.builder()
                .id(20L)
                .workspace(workspace)
                .user(user)
                .role(Role.builder().id(3L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build())
                .status(MemberStatus.ACTIVE)
                .build();

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(2L);
        request.setWorkspaceId(10L);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(roleService.getRole(2L)).thenReturn(workspaceRole);
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership));
        when(roleService.findById(2L)).thenReturn(roleResponse);

        UserRoleResponse result = userService.updateUserRole(2L, request);

        assertThat(result.getRole().getName()).isEqualTo("Workspace Member");
        assertThat(result.getWorkspaceId()).isEqualTo(10L);
        assertThat(user.getRole()).isEqualTo(workspaceRole);
        assertThat(membership.getRole()).isEqualTo(workspaceRole);
        verify(workspaceMemberRepository).save(membership);
        verify(refreshTokenService).revokeAllForUser(2L);
    }

    @Test
    void updateUserRole_shouldRejectTeamOrProjectRole() {
        User user = User.builder().id(2L).build();
        Role teamRole = Role.builder().id(3L).name("Team Leader").scope(RoleScope.TEAM).build();

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(3L);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(roleService.getRole(3L)).thenReturn(teamRole);

        assertThatThrownBy(() -> userService.updateUserRole(2L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void updateUserRole_shouldThrowWhenUserNotFound() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(1L);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(99L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateUserRole_shouldRejectSelfChange() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(1L);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.updateUserRole(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể tự thay đổi vai trò của chính mình");
    }

    @Test
    void updateUserRole_shouldRejectWorkspaceOwnerAssigningSystemAdmin() {
        activeUser.setRole(Role.builder().id(3L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build());
        User target = User.builder().id(2L).build();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(1L);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleService.getRole(1L)).thenReturn(systemRole);

        assertThatThrownBy(() -> userService.updateUserRole(2L, request))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(target);
    }

    @Test
    void updateUserRole_shouldRejectWorkspaceOwnerOutsideScope() {
        activeUser.setRole(Role.builder().id(3L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build());
        User target = User.builder().id(2L).build();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoleId(2L);
        request.setWorkspaceId(99L);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleService.getRole(2L)).thenReturn(workspaceMemberRole);
        when(workspaceService.getWorkspace(99L)).thenReturn(
                Workspace.builder().id(99L).status(CommonStatus.ACTIVE).build());
        when(workspaceRepository.findIdsByOwnerId(1L)).thenReturn(List.of(10L));
        when(workspaceMemberRepository.findOwnedWorkspaceIds(1L, MemberStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.updateUserRole(2L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_shouldThrowWhenEmailExists() {
        CreateUserRequest request = validCreateRequest();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
    }

    private CreateUserRequest validCreateRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("New User");
        request.setEmail("new@test.com");
        request.setWorkspaceId(10L);
        request.setTeamId(5L);
        return request;
    }

    private void stubTeamAssignment() {
        when(roleRepository.findByNameAndScope("Team Member", RoleScope.TEAM))
                .thenReturn(Optional.of(teamMemberRole));
        when(teamMemberService.add(eq(10L), eq(5L), any(AddTeamMemberRequest.class)))
                .thenReturn(TeamMemberResponse.builder().id(30L).build());
    }

    private UpdateProfileRequest passwordChangeRequest(String newPassword, String confirmation) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("oldpass");
        request.setNewPassword(newPassword);
        request.setConfirmNewPassword(confirmation);
        return request;
    }

}
