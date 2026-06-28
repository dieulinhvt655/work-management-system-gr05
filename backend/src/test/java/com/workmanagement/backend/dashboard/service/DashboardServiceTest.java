package com.workmanagement.backend.dashboard.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.dashboard.dto.response.DashboardSummaryResponse;
import com.workmanagement.backend.dashboard.dto.response.PersonalDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.ProjectDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.TaskStatusBreakdownResponse;
import com.workmanagement.backend.dashboard.mapper.DashboardMapper;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogItemRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.repository.ProjectRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.sprint.entity.Sprint;
import com.workmanagement.backend.sprint.repository.SprintRepository;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.team.service.TeamService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private TeamService teamService;
    @Mock
    private ProjectService projectService;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProductBacklogItemRepository productBacklogItemRepository;
    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardService dashboardService;

    private Workspace workspace;
    private Team team;
    private Project project;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        workspace = Workspace.builder().id(10L).name("Acme").status(CommonStatus.ACTIVE).build();
        team = Team.builder().id(20L).workspace(workspace).name("Platform").status(CommonStatus.ACTIVE).build();
        project = Project.builder().id(30L).team(team).name("WMS").status(ProjectStatus.ACTIVE).build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void getWorkspaceDashboard_shouldReturnSummary() {
        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyCanManage(workspace);
        when(teamRepository.findByWorkspaceId(10L)).thenReturn(List.of(team));
        when(projectRepository.findByTeamId(20L)).thenReturn(List.of(project));
        when(sprintRepository.existsByProjectIdAndStatus(30L, SprintStatus.ACTIVE)).thenReturn(true);
        when(workspaceMemberRepository.findByWorkspaceIdAndStatus(10L, MemberStatus.ACTIVE))
                .thenReturn(List.of(WorkspaceMember.builder().id(1L).build()));

        DashboardSummaryResponse response = dashboardService.getWorkspaceDashboard(10L);

        assertThat(response.getScopeType()).isEqualTo("workspace");
        assertThat(response.getTotalTeams()).isEqualTo(1);
        assertThat(response.getTotalProjects()).isEqualTo(1);
        assertThat(response.getActiveSprints()).isEqualTo(1);
        verify(workspaceService).verifyCanManage(workspace);
    }

    @Test
    void getTeamDashboard_shouldRequireTeamLeader() {
        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(teamService.isActiveTeamLeader(team)).thenReturn(false);

        assertThatThrownBy(() -> dashboardService.getTeamDashboard(10L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DASHBOARD_ACCESS_DENIED);
    }

    @Test
    void getTeamDashboard_shouldReturnSummaryForLeader() {
        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(teamService.isActiveTeamLeader(team)).thenReturn(true);
        when(projectRepository.findByTeamId(20L)).thenReturn(List.of(project));
        when(sprintRepository.existsByProjectIdAndStatus(30L, SprintStatus.ACTIVE)).thenReturn(false);
        when(teamMemberRepository.findByTeamIdAndStatus(20L, MemberStatus.ACTIVE))
                .thenReturn(List.of(TeamMember.builder().id(5L).build()));

        DashboardSummaryResponse response = dashboardService.getTeamDashboard(10L, 20L);

        assertThat(response.getScopeType()).isEqualTo("team");
        assertThat(response.getTotalProjects()).isEqualTo(1);
        assertThat(response.getTotalMembers()).isEqualTo(1);
    }

    @Test
    void getProjectDashboard_shouldRequireProjectManager() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(false);

        assertThatThrownBy(() -> dashboardService.getProjectDashboard(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DASHBOARD_ACCESS_DENIED);
    }

    @Test
    void getProjectDashboard_shouldReturnMetrics() {
        Sprint sprint = Sprint.builder()
                .id(80L)
                .project(project)
                .name("Sprint 1")
                .status(SprintStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .build();
        Task task = Task.builder().id(70L).status(TaskStatus.IN_PROGRESS).build();
        TaskStatusBreakdownResponse breakdown = TaskStatusBreakdownResponse.builder()
                .inProgress(1)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findFirstByProjectIdAndStatus(30L, SprintStatus.ACTIVE))
                .thenReturn(Optional.of(sprint));
        when(taskRepository.findBySprintIdOrderByCreatedAtDesc(80L)).thenReturn(List.of(task));
        when(productBacklogItemRepository.countByProjectId(30L)).thenReturn(5L);
        when(productBacklogItemRepository.countByProjectIdAndStatus(30L, PbiStatus.READY)).thenReturn(2L);
        when(productBacklogItemRepository.countByProjectIdAndStatus(30L, PbiStatus.IN_SPRINT)).thenReturn(1L);
        when(productBacklogItemRepository.countByProjectIdAndStatus(30L, PbiStatus.COMPLETED)).thenReturn(1L);
        when(projectMemberRepository.findByProjectIdAndStatus(30L, MemberStatus.ACTIVE)).thenReturn(List.of());
        when(dashboardMapper.toSprintDashboard(eq(sprint), any())).thenReturn(
                com.workmanagement.backend.dashboard.dto.response.SprintDashboardResponse.builder()
                        .sprintId(80L)
                        .sprintName("Sprint 1")
                        .build()
        );
        when(dashboardMapper.toTaskBreakdown(any())).thenReturn(breakdown);

        ProjectDashboardResponse response = dashboardService.getProjectDashboard(10L, 20L, 30L);

        assertThat(response.getProjectId()).isEqualTo(30L);
        assertThat(response.getTotalPbis()).isEqualTo(5);
        assertThat(response.getActiveSprint().getSprintId()).isEqualTo(80L);
        assertThat(response.getTaskBreakdown().getInProgress()).isEqualTo(1);
    }

    @Test
    void getPersonalDashboard_shouldRequireProjectMember() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getPersonalDashboard(10L, 20L, 30L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DASHBOARD_ACCESS_DENIED);
    }

    @Test
    void getPersonalDashboard_shouldReturnAssignedTasks() {
        User user = User.builder().id(2L).fullName("Alice").build();
        WorkspaceMember workspaceMember = WorkspaceMember.builder().user(user).build();
        TeamMember teamMember = TeamMember.builder().workspaceMember(workspaceMember).build();
        ProjectMember member = ProjectMember.builder().id(12L).teamMember(teamMember).project(project).build();
        Task task = Task.builder()
                .id(70L)
                .title("Fix bug")
                .status(TaskStatus.IN_PROGRESS)
                .progress(40)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(taskRepository.findAssignedByProjectIdAndUserId(30L, 2L)).thenReturn(List.of(task));
        when(dashboardMapper.toTaskBreakdown(any())).thenReturn(
                TaskStatusBreakdownResponse.builder().inProgress(1).build()
        );
        when(dashboardMapper.toUpcomingTasks(any(), eq(5))).thenReturn(List.of());
        when(dashboardMapper.toInProgressTasks(any())).thenReturn(List.of());

        PersonalDashboardResponse response = dashboardService.getPersonalDashboard(10L, 20L, 30L, null);

        assertThat(response.getTotalAssignedTasks()).isEqualTo(1);
        assertThat(response.getProjectName()).isEqualTo("WMS");
    }

}
