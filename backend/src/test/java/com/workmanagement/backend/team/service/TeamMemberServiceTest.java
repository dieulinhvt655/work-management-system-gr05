package com.workmanagement.backend.team.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
import com.workmanagement.backend.team.dto.request.UpdateTeamMemberRequest;
import com.workmanagement.backend.team.dto.response.TeamMemberResponse;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.mapper.TeamMemberMapper;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.service.WorkspaceService;
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
class TeamMemberServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private TeamMemberMapper teamMemberMapper;
    @Mock
    private TeamService teamService;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private TeamMemberService teamMemberService;

    private Workspace workspace;
    private Team team;
    private WorkspaceMember workspaceMember;
    private Role teamMemberRole;
    private Role teamLeaderRole;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        User user = User.builder().id(2L).fullName("Member").build();
        workspaceMember = WorkspaceMember.builder()
                .id(5L)
                .workspace(workspace)
                .user(user)
                .status(MemberStatus.ACTIVE)
                .build();
        teamMemberRole = Role.builder().id(3L).name("Team Member").scope(RoleScope.TEAM).build();
        teamLeaderRole = Role.builder().id(4L).name("Team Leader").scope(RoleScope.TEAM).build();
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void findAll_shouldReturnActiveMembers() {
        TeamMember member = TeamMember.builder()
                .id(7L)
                .team(team)
                .workspaceMember(workspaceMember)
                .role(teamMemberRole)
                .status(MemberStatus.ACTIVE)
                .build();
        TeamMemberResponse response = TeamMemberResponse.builder().id(7L).build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(workspaceService).verifyAccess(workspace);
        when(teamMemberRepository.findByTeamIdAndStatus(20L, MemberStatus.ACTIVE)).thenReturn(List.of(member));
        when(teamMemberMapper.toResponse(member)).thenReturn(response);

        List<TeamMemberResponse> result = teamMemberService.findAll(10L, 20L);

        assertThat(result).hasSize(1);
    }

    @Test
    void add_shouldCreateMember() {
        AddTeamMemberRequest request = new AddTeamMemberRequest();
        request.setWorkspaceMemberId(5L);
        request.setRoleId(3L);

        TeamMember saved = TeamMember.builder()
                .id(7L)
                .team(team)
                .workspaceMember(workspaceMember)
                .role(teamMemberRole)
                .status(MemberStatus.ACTIVE)
                .build();
        TeamMemberResponse response = TeamMemberResponse.builder().id(7L).build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        doNothing().when(teamService).ensureTeamActive(team);
        when(teamMemberRepository.existsByTeamIdAndWorkspaceMemberId(20L, 5L)).thenReturn(false);
        when(workspaceMemberRepository.findById(5L)).thenReturn(Optional.of(workspaceMember));
        when(roleService.getRole(3L)).thenReturn(teamMemberRole);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(saved);
        when(teamMemberMapper.toResponse(saved)).thenReturn(response);

        TeamMemberResponse result = teamMemberService.add(10L, 20L, request);

        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void add_shouldRejectDuplicate() {
        AddTeamMemberRequest request = new AddTeamMemberRequest();
        request.setWorkspaceMemberId(5L);
        request.setRoleId(3L);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        doNothing().when(teamService).ensureTeamActive(team);
        when(teamMemberRepository.existsByTeamIdAndWorkspaceMemberId(20L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> teamMemberService.add(10L, 20L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEAM_MEMBER_ALREADY_EXISTS);
    }

    @Test
    void assignLeader_shouldSetLeaderAndDemoteOthers() {
        TeamMember existingLeader = TeamMember.builder()
                .id(8L)
                .team(team)
                .role(teamLeaderRole)
                .status(MemberStatus.ACTIVE)
                .build();
        TeamMember member = TeamMember.builder()
                .id(7L)
                .team(team)
                .workspaceMember(workspaceMember)
                .role(teamMemberRole)
                .status(MemberStatus.ACTIVE)
                .build();
        TeamMemberResponse response = TeamMemberResponse.builder().id(7L).build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        doNothing().when(teamService).ensureTeamActive(team);
        when(teamMemberRepository.findByIdAndTeamId(7L, 20L)).thenReturn(Optional.of(member));
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(Optional.of(teamLeaderRole));
        when(roleRepository.findByNameAndScope("Team Member", RoleScope.TEAM)).thenReturn(Optional.of(teamMemberRole));
        when(teamMemberRepository.findByTeamIdAndRole_IdAndStatus(20L, 4L, MemberStatus.ACTIVE))
                .thenReturn(List.of(existingLeader));
        when(teamMemberRepository.save(member)).thenReturn(member);
        when(teamMemberMapper.toResponse(member)).thenReturn(response);

        TeamMemberResponse result = teamMemberService.assignLeader(10L, 20L, 7L);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(member.getRole().getName()).isEqualTo("Team Leader");
        assertThat(existingLeader.getRole().getName()).isEqualTo("Team Member");
    }

    @Test
    void update_shouldRejectDeactivatingLeader() {
        UpdateTeamMemberRequest request = new UpdateTeamMemberRequest();
        request.setRoleId(4L);
        request.setStatus(MemberStatus.INACTIVE);

        TeamMember member = TeamMember.builder()
                .id(7L)
                .team(team)
                .workspaceMember(workspaceMember)
                .role(teamLeaderRole)
                .status(MemberStatus.ACTIVE)
                .build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        doNothing().when(teamService).ensureTeamActive(team);
        when(teamMemberRepository.findByIdAndTeamId(7L, 20L)).thenReturn(Optional.of(member));
        when(roleService.getRole(4L)).thenReturn(teamLeaderRole);
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(Optional.of(teamLeaderRole));
        when(roleRepository.findByNameAndScope("Team Member", RoleScope.TEAM)).thenReturn(Optional.of(teamMemberRole));
        when(teamMemberRepository.findByTeamIdAndRole_IdAndStatus(20L, 4L, MemberStatus.ACTIVE))
                .thenReturn(List.of(member));

        assertThatThrownBy(() -> teamMemberService.update(10L, 20L, 7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

}
