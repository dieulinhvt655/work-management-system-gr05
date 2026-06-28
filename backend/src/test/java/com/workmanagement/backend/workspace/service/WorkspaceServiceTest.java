package com.workmanagement.backend.workspace.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
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
        owner = User.builder().id(1L).fullName("Owner").email("owner@test.com").role(systemRole).build();
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

        Workspace saved = Workspace.builder().id(10L).owner(owner).name("New WS").status(CommonStatus.ACTIVE).build();
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("New WS").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(saved);
        when(roleRepository.findByNameAndScope("Workspace Owner", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceOwnerRole));
        when(workspaceMapper.toResponse(saved)).thenReturn(response);

        WorkspaceResponse result = workspaceService.create(request);

        assertThat(result.getName()).isEqualTo("New WS");
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnPagedWorkspaces() {
        WorkspaceResponse item = WorkspaceResponse.builder().id(10L).name("My Workspace").build();
        PageImpl<Workspace> page = new PageImpl<>(List.of(workspace));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(workspaceMapper.toResponse(workspace)).thenReturn(item);

        PageResponse<WorkspaceResponse> result = workspaceService.findAll(0, 20, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_shouldReturnWorkspaceWhenAccessible() {
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("My Workspace").build();

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceMapper.toResponse(workspace)).thenReturn(response);

        WorkspaceResponse result = workspaceService.findById(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void findById_shouldDenyWhenNotMember() {
        User outsider = User.builder().id(99L).fullName("Outsider").build();
        Role memberSystemRole = Role.builder().id(5L).name("Workspace Member").scope(RoleScope.SYSTEM).build();
        User currentUser = User.builder().id(1L).fullName("Current").role(memberSystemRole).build();
        workspace.setOwner(outsider);

        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndStatus(10L, 1L, MemberStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() -> workspaceService.findById(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
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

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByNameAndScope("Workspace Owner", RoleScope.WORKSPACE))
                .thenReturn(Optional.of(workspaceOwnerRole));
        when(workspaceMapper.toResponse(any())).thenReturn(WorkspaceResponse.builder().build());

        workspaceService.create(request);

        ArgumentCaptor<WorkspaceMember> captor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole().getName()).isEqualTo("Workspace Owner");
        assertThat(captor.getValue().getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

}
