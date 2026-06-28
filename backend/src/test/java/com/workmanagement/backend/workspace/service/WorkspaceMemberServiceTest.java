package com.workmanagement.backend.workspace.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.workspace.dto.request.AddWorkspaceMemberRequest;
import com.workmanagement.backend.workspace.dto.request.UpdateWorkspaceMemberRequest;
import com.workmanagement.backend.workspace.dto.response.WorkspaceMemberResponse;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.mapper.WorkspaceMemberMapper;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberServiceTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private WorkspaceMemberMapper workspaceMemberMapper;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private WorkspaceMemberService workspaceMemberService;

    private Workspace workspace;
    private User owner;
    private User memberUser;
    private Role memberRole;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).fullName("Owner").status(UserStatus.ACTIVE).build();
        memberUser = User.builder().id(2L).fullName("Member").status(UserStatus.ACTIVE).build();
        memberRole = Role.builder().id(3L).name("Workspace Member").scope(RoleScope.WORKSPACE).build();
        workspace = Workspace.builder().id(10L).owner(owner).status(CommonStatus.ACTIVE).build();
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void findAll_shouldReturnActiveMembers() {
        WorkspaceMember member = WorkspaceMember.builder()
                .id(5L)
                .workspace(workspace)
                .user(memberUser)
                .role(memberRole)
                .status(MemberStatus.ACTIVE)
                .build();
        WorkspaceMemberResponse response = WorkspaceMemberResponse.builder().id(5L).build();

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyAccess(workspace);
        when(workspaceMemberRepository.findByWorkspaceIdAndStatus(10L, MemberStatus.ACTIVE))
                .thenReturn(List.of(member));
        when(workspaceMemberMapper.toResponse(member)).thenReturn(response);

        List<WorkspaceMemberResponse> result = workspaceMemberService.findAll(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void add_shouldCreateMember() {
        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest();
        request.setUserId(2L);
        request.setRoleId(3L);

        WorkspaceMember saved = WorkspaceMember.builder()
                .id(5L)
                .workspace(workspace)
                .user(memberUser)
                .role(memberRole)
                .status(MemberStatus.ACTIVE)
                .build();
        WorkspaceMemberResponse response = WorkspaceMemberResponse.builder().id(5L).build();

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(memberUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(roleService.getRole(3L)).thenReturn(memberRole);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(saved);
        when(workspaceMemberMapper.toResponse(saved)).thenReturn(response);

        WorkspaceMemberResponse result = workspaceMemberService.add(10L, request);

        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void add_shouldRejectDuplicateMember() {
        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest();
        request.setUserId(2L);
        request.setRoleId(3L);

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> workspaceMemberService.add(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS);
    }

    @Test
    void update_shouldChangeRoleAndStatus() {
        UpdateWorkspaceMemberRequest request = new UpdateWorkspaceMemberRequest();
        request.setRoleId(3L);
        request.setStatus(MemberStatus.INACTIVE);

        WorkspaceMember member = WorkspaceMember.builder()
                .id(5L)
                .workspace(workspace)
                .user(memberUser)
                .role(memberRole)
                .status(MemberStatus.ACTIVE)
                .build();
        WorkspaceMemberResponse response = WorkspaceMemberResponse.builder().id(5L).build();

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(workspaceMemberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(roleService.getRole(3L)).thenReturn(memberRole);
        when(workspaceMemberRepository.save(member)).thenReturn(member);
        when(workspaceMemberMapper.toResponse(member)).thenReturn(response);

        workspaceMemberService.update(10L, 5L, request);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(member.getRemovedAt()).isNotNull();
    }

    @Test
    void update_shouldRejectDeactivatingOwner() {
        UpdateWorkspaceMemberRequest request = new UpdateWorkspaceMemberRequest();
        request.setRoleId(3L);
        request.setStatus(MemberStatus.INACTIVE);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .id(5L)
                .workspace(workspace)
                .user(owner)
                .role(memberRole)
                .status(MemberStatus.ACTIVE)
                .build();

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(workspaceMemberRepository.findById(5L)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> workspaceMemberService.update(10L, 5L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

}
