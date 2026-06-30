package com.workmanagement.backend.workspace.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.workspace.dto.request.CreateWorkspaceRequest;
import com.workmanagement.backend.workspace.dto.request.UpdateWorkspaceRequest;
import com.workmanagement.backend.workspace.dto.response.WorkspaceResponse;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.mapper.WorkspaceMapper;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private WorkspaceMapper workspaceMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private WorkspaceService workspaceService;

    private User owner;
    private Role workspaceOwnerRole;
    private Workspace workspace;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        Role systemRole = Role.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();
        owner = User.builder().id(1L).fullName("Owner").email("owner@test.com").role(systemRole)
                .status(UserStatus.ACTIVE).build();
        workspaceOwnerRole = Role.builder().id(2L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build();
        workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("My Workspace")
                .description("Desc")
                .status(CommonStatus.ACTIVE)
                .build();
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldSaveWorkspaceAndOwnerMember() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName("New WS");
        request.setDescription("Description");
        request.setOwnerId(2L);

        User selectedOwner = User.builder().id(2L).fullName("Selected Owner")
                .status(UserStatus.ACTIVE).build();

        Workspace saved = Workspace.builder().id(10L).owner(selectedOwner).name("New WS").status(CommonStatus.ACTIVE).build();
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("New WS").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(selectedOwner));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(saved);
        when(roleRepository.findByNameAndScope("Workspace Owner", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceOwnerRole));
        mockDefaultRolesExist();
        when(workspaceMapper.toResponse(saved)).thenReturn(response);

        WorkspaceResponse result = workspaceService.create(request);

        assertThat(result.getName()).isEqualTo("New WS");
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUser()).isSameAs(selectedOwner);
        assertThat(memberCaptor.getValue().getAddedByOwner()).isSameAs(owner);
        assertThat(selectedOwner.getRole()).isSameAs(workspaceOwnerRole);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnPagedWorkspaces() {
        WorkspaceResponse item = WorkspaceResponse.builder().id(10L).name("My Workspace").build();
        PageImpl<Workspace> page = new PageImpl<>(List.of(workspace));

        when(workspaceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(workspaceMapper.toResponse(workspace)).thenReturn(item);

        PageResponse<WorkspaceResponse> result = workspaceService.findAll(0, 20, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_shouldReturnWorkspaceForSystemAdmin() {
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("My Workspace").build();

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceMapper.toResponse(workspace)).thenReturn(response);

        WorkspaceResponse result = workspaceService.findById(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void findById_shouldDenyNonAdmin() {
        User outsider = User.builder().id(99L).fullName("Outsider").build();
        Role memberSystemRole = Role.builder().id(5L).name("Workspace Member").scope(RoleScope.SYSTEM).build();
        User currentUser = User.builder().id(1L).fullName("Current").role(memberSystemRole).build();
        workspace.setOwner(outsider);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        assertThatThrownBy(() -> workspaceService.findById(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    @Test
    void findCurrent_shouldAllowActiveWorkspaceMember() {
        User workspaceOwner = User.builder().id(99L).fullName("Workspace Owner").build();
        Role memberRole = Role.builder().id(5L).name("Team Member").scope(RoleScope.TEAM).build();
        User member = User.builder().id(2L).fullName("Member").role(memberRole).build();
        workspace.setOwner(workspaceOwner);
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("My Workspace").build();

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(workspaceMemberRepository.findByUserIdAndStatus(2L, MemberStatus.ACTIVE))
                .thenReturn(List.of(WorkspaceMember.builder().workspace(workspace).user(member).build()));
        when(workspaceMapper.toResponse(workspace)).thenReturn(response);

        assertThat(workspaceService.findCurrent().getId()).isEqualTo(10L);
    }

    @Test
    void update_shouldDenyWorkspaceMember() {
        User workspaceOwner = User.builder().id(99L).fullName("Workspace Owner").build();
        Role memberRole = Role.builder().id(5L).name("Workspace Member").scope(RoleScope.WORKSPACE).build();
        User member = User.builder().id(2L).fullName("Member").role(memberRole).build();
        workspace.setOwner(workspaceOwner);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> workspaceService.update(10L, new UpdateWorkspaceRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateWorkspaceInfo() {
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        request.setName("Updated");
        request.setDescription("New desc");

        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("Updated").build();

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(workspace)).thenReturn(workspace);
        when(workspaceMapper.toResponse(workspace)).thenReturn(response);

        WorkspaceResponse result = workspaceService.update(10L, request);

        assertThat(result.getName()).isEqualTo("Updated");
        assertThat(workspace.getName()).isEqualTo("Updated");
    }

    @Test
    void close_shouldAllowOwnerToDeactivateOwnWorkspace() {
        Role ownerRole = Role.builder().id(2L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build();
        User workspaceOwner = User.builder().id(2L).fullName("Workspace Owner").role(ownerRole).build();
        workspace.setOwner(workspaceOwner);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(2L)).thenReturn(Optional.of(workspaceOwner));
        when(workspaceRepository.save(workspace)).thenReturn(workspace);
        when(workspaceMapper.toResponse(workspace))
                .thenReturn(WorkspaceResponse.builder().id(10L).status(CommonStatus.INACTIVE).build());

        WorkspaceResponse result = workspaceService.close(10L);

        assertThat(result.getStatus()).isEqualTo(CommonStatus.INACTIVE);
        assertThat(workspace.getStatus()).isEqualTo(CommonStatus.INACTIVE);
    }

    @Test
    void update_shouldDenyOwnerWhenWorkspaceIsInactive() {
        Role ownerRole = Role.builder().id(2L).name("Workspace Owner").scope(RoleScope.WORKSPACE).build();
        User workspaceOwner = User.builder().id(2L).fullName("Workspace Owner").role(ownerRole).build();
        workspace.setOwner(workspaceOwner);
        workspace.setStatus(CommonStatus.INACTIVE);
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        request.setDescription("Cannot update");

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(2L)).thenReturn(Optional.of(workspaceOwner));

        assertThatThrownBy(() -> workspaceService.update(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void update_shouldAllowSystemAdminToReactivateWorkspace() {
        workspace.setOwner(User.builder().id(99L).fullName("Workspace Owner").build());
        workspace.setStatus(CommonStatus.INACTIVE);

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(workspace)).thenReturn(workspace);
        when(workspaceMapper.toResponse(workspace))
                .thenReturn(WorkspaceResponse.builder().id(10L).status(CommonStatus.ACTIVE).build());

        WorkspaceResponse result = workspaceService.reactivate(10L);

        assertThat(result.getStatus()).isEqualTo(CommonStatus.ACTIVE);
        assertThat(workspace.getStatus()).isEqualTo(CommonStatus.ACTIVE);
    }

    @Test
    void update_shouldRejectDuplicateWorkspaceName() {
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        request.setName(" Existing Workspace ");

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.existsByNameIgnoreCaseAndIdNot("Existing Workspace", 10L)).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.update(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_NAME_ALREADY_EXISTS);

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void close_shouldSetStatusInactive() {
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).status(CommonStatus.INACTIVE).build();

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(workspace)).thenReturn(workspace);
        when(workspaceMapper.toResponse(workspace)).thenReturn(response);

        WorkspaceResponse result = workspaceService.close(10L);

        assertThat(result.getStatus()).isEqualTo(CommonStatus.INACTIVE);
        assertThat(workspace.getStatus()).isEqualTo(CommonStatus.INACTIVE);
    }

    @Test
    void close_shouldRejectWhenAlreadyClosed() {
        workspace.setStatus(CommonStatus.INACTIVE);

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> workspaceService.close(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void create_shouldUseWorkspaceOwnerRole() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName("WS");
        request.setOwnerId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByNameAndScope("Workspace Owner", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceOwnerRole));
        mockDefaultRolesExist();
        when(workspaceMapper.toResponse(any())).thenReturn(WorkspaceResponse.builder().build());

        workspaceService.create(request);

        ArgumentCaptor<WorkspaceMember> captor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole().getName()).isEqualTo("Workspace Owner");
        assertThat(captor.getValue().getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void create_shouldRejectDuplicateName() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName(" Existing Workspace ");
        request.setOwnerId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.existsByNameIgnoreCase("Existing Workspace")).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_NAME_ALREADY_EXISTS);

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectInactiveOwner() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName("WS");
        request.setOwnerId(2L);
        User inactiveOwner = User.builder().id(2L).status(UserStatus.INACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveOwner));

        assertThatThrownBy(() -> workspaceService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_INACTIVE);

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectOwnerAlreadyAssignedToWorkspace() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName("WS");
        request.setOwnerId(2L);
        User selectedOwner = User.builder().id(2L).status(UserStatus.ACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(selectedOwner));
        when(workspaceMemberRepository.existsByUserId(2L)).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS);

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void findCurrent_shouldRejectInactiveWorkspace() {
        Role memberRole = Role.builder().id(5L).name("Workspace Member").scope(RoleScope.WORKSPACE).build();
        User member = User.builder().id(2L).fullName("Member").role(memberRole).build();
        workspace.setStatus(CommonStatus.INACTIVE);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(workspaceMemberRepository.findByUserIdAndStatus(2L, MemberStatus.ACTIVE))
                .thenReturn(List.of(WorkspaceMember.builder().workspace(workspace).user(member).build()));

        assertThatThrownBy(workspaceService::findCurrent)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    private void mockDefaultRolesExist() {
        when(roleRepository.existsByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(true);
        when(roleRepository.existsByNameAndScope("Project Manager", RoleScope.PROJECT)).thenReturn(true);
        when(roleRepository.existsByNameAndScope("Team Member", RoleScope.TEAM)).thenReturn(true);
    }

}
