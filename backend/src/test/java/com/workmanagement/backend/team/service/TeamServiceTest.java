package com.workmanagement.backend.team.service;

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
import com.workmanagement.backend.team.dto.request.CreateTeamRequest;
import com.workmanagement.backend.team.dto.request.UpdateTeamRequest;
import com.workmanagement.backend.team.dto.response.TeamResponse;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.mapper.TeamMapper;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private TeamMapper teamMapper;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private TeamService teamService;

    private Workspace workspace;
    private Team team;
    private Role teamLeaderRole;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        User owner = User.builder().id(1L).fullName("Owner").build();
        workspace = Workspace.builder().id(10L).owner(owner).status(CommonStatus.ACTIVE).build();
        team = Team.builder()
                .id(20L)
                .workspace(workspace)
                .name("Dev Team")
                .description("Team desc")
                .status(CommonStatus.ACTIVE)
                .build();
        teamLeaderRole = Role.builder().id(5L).name("Team Leader").scope(RoleScope.TEAM).build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldSaveTeam() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("New Team");
        request.setDescription("Description");

        Team saved = Team.builder().id(20L).workspace(workspace).name("New Team").status(CommonStatus.ACTIVE).build();
        TeamResponse response = TeamResponse.builder().id(20L).name("New Team").build();

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(teamRepository.save(any(Team.class))).thenReturn(saved);
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(Optional.empty());
        when(teamMapper.toResponse(saved, null)).thenReturn(response);

        TeamResponse result = teamService.create(10L, request);

        assertThat(result.getName()).isEqualTo("New Team");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnPagedTeams() {
        TeamResponse item = TeamResponse.builder().id(20L).name("Dev Team").build();
        PageImpl<Team> page = new PageImpl<>(List.of(team));

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyAccess(workspace);
        when(teamRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(Optional.empty());
        when(teamMapper.toResponse(team, null)).thenReturn(item);

        PageResponse<TeamResponse> result = teamService.findAll(10L, 0, 20, null, null);

        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void findById_shouldReturnTeamWithLeader() {
        User leaderUser = User.builder().id(2L).fullName("Leader").build();
        TeamMember leader = TeamMember.builder()
                .id(30L)
                .team(team)
                .workspaceMember(com.workmanagement.backend.workspace.entity.WorkspaceMember.builder()
                        .user(leaderUser)
                        .build())
                .build();
        TeamResponse response = TeamResponse.builder().id(20L).teamLeaderName("Leader").build();

        when(teamRepository.findByIdAndWorkspaceId(20L, 10L)).thenReturn(Optional.of(team));
        doNothing().when(workspaceService).verifyAccess(workspace);
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(Optional.of(teamLeaderRole));
        when(teamMemberRepository.findByTeamIdAndRole_IdAndStatus(20L, 5L, MemberStatus.ACTIVE))
                .thenReturn(List.of(leader));
        when(teamMapper.toResponse(team, leader)).thenReturn(response);

        TeamResponse result = teamService.findById(10L, 20L);

        assertThat(result.getTeamLeaderName()).isEqualTo("Leader");
    }

    @Test
    void update_shouldChangeName() {
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Updated Team");

        TeamResponse response = TeamResponse.builder().id(20L).name("Updated Team").build();

        when(teamRepository.findByIdAndWorkspaceId(20L, 10L)).thenReturn(Optional.of(team));
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(teamRepository.save(team)).thenReturn(team);
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(Optional.empty());
        when(teamMapper.toResponse(team, null)).thenReturn(response);

        TeamResponse result = teamService.update(10L, 20L, request);

        assertThat(result.getName()).isEqualTo("Updated Team");
        assertThat(team.getName()).isEqualTo("Updated Team");
    }

    @Test
    void disband_shouldSetInactive() {
        TeamResponse response = TeamResponse.builder().id(20L).status(CommonStatus.INACTIVE).build();

        when(teamRepository.findByIdAndWorkspaceId(20L, 10L)).thenReturn(Optional.of(team));
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(teamRepository.save(team)).thenReturn(team);
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM)).thenReturn(Optional.empty());
        when(teamMapper.toResponse(team, null)).thenReturn(response);

        TeamResponse result = teamService.disband(10L, 20L);

        assertThat(result.getStatus()).isEqualTo(CommonStatus.INACTIVE);
        assertThat(team.getStatus()).isEqualTo(CommonStatus.INACTIVE);
    }

    @Test
    void disband_shouldRejectAlreadyDisbanded() {
        team.setStatus(CommonStatus.INACTIVE);

        when(teamRepository.findByIdAndWorkspaceId(20L, 10L)).thenReturn(Optional.of(team));
        doNothing().when(workspaceService).verifyCanManage(workspace);

        assertThatThrownBy(() -> teamService.disband(10L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getTeam_shouldThrowWhenNotFound() {
        when(teamRepository.findByIdAndWorkspaceId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeam(10L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    void update_shouldRejectDisbandedTeam() {
        team.setStatus(CommonStatus.INACTIVE);
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Updated");

        when(teamRepository.findByIdAndWorkspaceId(20L, 10L)).thenReturn(Optional.of(team));
        doNothing().when(workspaceService).verifyCanManage(workspace);

        assertThatThrownBy(() -> teamService.update(10L, 20L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

}
