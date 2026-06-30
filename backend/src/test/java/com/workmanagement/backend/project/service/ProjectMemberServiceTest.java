package com.workmanagement.backend.project.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.dto.request.AddProjectMemberRequest;
import com.workmanagement.backend.project.dto.request.UpdateProjectMemberRequest;
import com.workmanagement.backend.project.dto.response.ProjectMemberResponse;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.mapper.ProjectMemberMapper;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.service.TeamService;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private ProjectService projectService;
    @Mock
    private TeamService teamService;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private Team team;
    private Project project;
    private TeamMember teamMember;
    private Role contributorRole;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        User user = User.builder().id(2L).fullName("Dev").build();
        WorkspaceMember wsMember = WorkspaceMember.builder().id(5L).user(user).build();
        teamMember = TeamMember.builder()
                .id(7L)
                .team(team)
                .workspaceMember(wsMember)
                .status(MemberStatus.ACTIVE)
                .build();
        project = Project.builder()
                .id(30L)
                .team(team)
                .projectManagerMember(teamMember)
                .code("PRJ-01")
                .name("Project")
                .status(ProjectStatus.DRAFT)
                .build();
        contributorRole = Role.builder().id(8L).name("Project Contributor").scope(RoleScope.PROJECT).build();
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void findAll_shouldReturnActiveMembers() {
        ProjectMember member = ProjectMember.builder()
                .id(9L)
                .project(project)
                .teamMember(teamMember)
                .role(contributorRole)
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMemberResponse response = ProjectMemberResponse.builder().id(9L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(projectMemberRepository.findByProjectIdAndStatus(30L, MemberStatus.ACTIVE)).thenReturn(List.of(member));
        when(projectMemberMapper.toResponse(member)).thenReturn(response);

        List<ProjectMemberResponse> result = projectMemberService.findAll(10L, 20L, 30L);

        assertThat(result).hasSize(1);
    }

    @Test
    void add_shouldAssignTeamMemberToProject() {
        AddProjectMemberRequest request = new AddProjectMemberRequest();
        request.setTeamMemberId(8L);
        request.setRoleId(8L);

        TeamMember newMember = TeamMember.builder()
                .id(8L)
                .team(team)
                .workspaceMember(WorkspaceMember.builder()
                        .id(6L)
                        .user(User.builder().id(3L).fullName("Dev 2").build())
                        .build())
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMember saved = ProjectMember.builder()
                .id(9L)
                .project(project)
                .teamMember(newMember)
                .role(contributorRole)
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMemberResponse response = ProjectMemberResponse.builder().id(9L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(teamMemberRepository.findByIdAndTeamId(8L, 20L)).thenReturn(Optional.of(newMember));
        when(roleService.getRole(8L)).thenReturn(contributorRole);
        when(projectMemberRepository.findByProjectIdAndTeamMemberId(30L, 8L)).thenReturn(Optional.empty());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(saved);
        when(projectMemberMapper.toResponse(saved)).thenReturn(response);

        ProjectMemberResponse result = projectMemberService.add(10L, 20L, 30L, request);

        assertThat(result.getId()).isEqualTo(9L);
        verify(activityLogService).recordProjectEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_shouldPromoteMemberToProjectManager() {
        TeamMember newPm = TeamMember.builder()
                .id(8L)
                .team(team)
                .workspaceMember(WorkspaceMember.builder()
                        .id(6L)
                        .user(User.builder().id(3L).fullName("Lead").build())
                        .build())
                .status(MemberStatus.ACTIVE)
                .build();
        Role pmProjectRole = Role.builder().id(9L).name("Project Manager").scope(RoleScope.PROJECT).build();
        ProjectMember oldPmMember = ProjectMember.builder()
                .id(9L)
                .project(project)
                .teamMember(teamMember)
                .role(pmProjectRole)
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMember candidateMember = ProjectMember.builder()
                .id(10L)
                .project(project)
                .teamMember(newPm)
                .role(contributorRole)
                .status(MemberStatus.ACTIVE)
                .build();

        project.setProjectManagerMember(teamMember);

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectMemberRepository.findByIdAndProjectId(10L, 30L)).thenReturn(Optional.of(candidateMember));
        when(roleService.getRole(9L)).thenReturn(pmProjectRole);
        when(roleRepository.findByNameAndScope("Project Contributor", RoleScope.PROJECT))
                .thenReturn(Optional.of(contributorRole));
        when(projectMemberRepository.findByProjectIdAndTeamMemberId(30L, teamMember.getId()))
                .thenReturn(Optional.of(oldPmMember));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectMemberMapper.toResponse(candidateMember)).thenReturn(ProjectMemberResponse.builder().id(10L).build());

        UpdateProjectMemberRequest request = new UpdateProjectMemberRequest();
        request.setRoleId(9L);

        ProjectMemberResponse result = projectMemberService.update(10L, 20L, 30L, 10L, request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(project.getProjectManagerMember().getId()).isEqualTo(8L);
        assertThat(oldPmMember.getRole().getName()).isEqualTo("Project Contributor");
        verify(projectMemberRepository).save(oldPmMember);
    }

    @Test
    void remove_shouldMarkMemberInactive() {
        TeamMember otherMember = TeamMember.builder()
                .id(8L)
                .team(team)
                .workspaceMember(WorkspaceMember.builder()
                        .id(6L)
                        .user(User.builder().id(3L).fullName("Dev 2").build())
                        .build())
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMember member = ProjectMember.builder()
                .id(9L)
                .project(project)
                .teamMember(otherMember)
                .role(contributorRole)
                .status(MemberStatus.ACTIVE)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectMemberRepository.findByIdAndProjectId(9L, 30L)).thenReturn(Optional.of(member));
        when(projectMemberRepository.save(member)).thenReturn(member);

        projectMemberService.remove(10L, 20L, 30L, 9L);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.INACTIVE);
        verify(activityLogService).recordProjectEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void add_shouldRejectDuplicateMember() {
        AddProjectMemberRequest request = new AddProjectMemberRequest();
        request.setTeamMemberId(8L);
        request.setRoleId(8L);

        TeamMember newMember = TeamMember.builder()
                .id(8L)
                .team(team)
                .workspaceMember(WorkspaceMember.builder()
                        .id(6L)
                        .user(User.builder().id(3L).fullName("Dev 2").build())
                        .build())
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMember existingActiveMember = ProjectMember.builder()
                .id(9L)
                .project(project)
                .teamMember(newMember)
                .role(contributorRole)
                .status(MemberStatus.ACTIVE)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(teamMemberRepository.findByIdAndTeamId(8L, 20L)).thenReturn(Optional.of(newMember));
        when(roleService.getRole(8L)).thenReturn(contributorRole);
        when(projectMemberRepository.findByProjectIdAndTeamMemberId(30L, 8L))
                .thenReturn(Optional.of(existingActiveMember));

        assertThatThrownBy(() -> projectMemberService.add(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
    }

    @Test
    void add_shouldAllowProjectManagerWhenTeamLeaderCheckFails() {
        AddProjectMemberRequest request = new AddProjectMemberRequest();
        request.setTeamMemberId(8L);
        request.setRoleId(8L);

        TeamMember newMember = TeamMember.builder()
                .id(8L)
                .team(team)
                .workspaceMember(WorkspaceMember.builder()
                        .id(6L)
                        .user(User.builder().id(3L).fullName("Dev 2").status(com.workmanagement.backend.common.enums.UserStatus.ACTIVE).build())
                        .build())
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMember saved = ProjectMember.builder()
                .id(9L)
                .project(project)
                .teamMember(newMember)
                .role(contributorRole)
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMemberResponse response = ProjectMemberResponse.builder().id(9L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doThrow(new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED, "denied"))
                .when(teamService).verifyCanManageProject(team);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(teamMemberRepository.findByIdAndTeamId(8L, 20L)).thenReturn(Optional.of(newMember));
        when(roleService.getRole(8L)).thenReturn(contributorRole);
        when(projectMemberRepository.findByProjectIdAndTeamMemberId(30L, 8L)).thenReturn(Optional.empty());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(saved);
        when(projectMemberMapper.toResponse(saved)).thenReturn(response);

        ProjectMemberResponse result = projectMemberService.add(10L, 20L, 30L, request);

        assertThat(result.getId()).isEqualTo(9L);
    }

}
