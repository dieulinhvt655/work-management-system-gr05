package com.workmanagement.backend.project.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.dto.request.AddProjectMemberRequest;
import com.workmanagement.backend.project.dto.response.ProjectMemberResponse;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.mapper.ProjectMemberMapper;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.security.entity.Role;
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
        request.setTeamMemberId(7L);
        request.setRoleId(8L);

        ProjectMember saved = ProjectMember.builder()
                .id(9L)
                .project(project)
                .teamMember(teamMember)
                .role(contributorRole)
                .status(MemberStatus.ACTIVE)
                .build();
        ProjectMemberResponse response = ProjectMemberResponse.builder().id(9L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectMemberRepository.existsByProjectIdAndTeamMemberId(30L, 7L)).thenReturn(false);
        when(teamMemberRepository.findByIdAndTeamId(7L, 20L)).thenReturn(Optional.of(teamMember));
        when(roleService.getRole(8L)).thenReturn(contributorRole);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(saved);
        when(projectMemberMapper.toResponse(saved)).thenReturn(response);

        ProjectMemberResponse result = projectMemberService.add(10L, 20L, 30L, request);

        assertThat(result.getId()).isEqualTo(9L);
        verify(activityLogService).recordProjectEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void add_shouldRejectDuplicateMember() {
        AddProjectMemberRequest request = new AddProjectMemberRequest();
        request.setTeamMemberId(7L);
        request.setRoleId(8L);

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectMemberRepository.existsByProjectIdAndTeamMemberId(30L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.add(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
    }

}
