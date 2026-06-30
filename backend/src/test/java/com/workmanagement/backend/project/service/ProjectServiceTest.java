package com.workmanagement.backend.project.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.dto.request.CreateProjectRequest;
import com.workmanagement.backend.project.dto.request.UpdateProjectRequest;
import com.workmanagement.backend.project.dto.response.ProjectResponse;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.mapper.ProjectMapper;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.repository.ProjectRepository;
import com.workmanagement.backend.project.util.ProjectCodeGenerator;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogRepository;
import com.workmanagement.backend.notification.service.NotificationService;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.service.TeamService;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.task.entity.WorkflowState;
import com.workmanagement.backend.task.entity.WorkflowTransition;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.task.repository.WorkflowStateRepository;
import com.workmanagement.backend.task.repository.WorkflowTransitionRepository;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private TeamService teamService;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private ProjectCodeGenerator projectCodeGenerator;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkflowStateRepository workflowStateRepository;
    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;
    @Mock
    private ProductBacklogRepository productBacklogRepository;

    @InjectMocks
    private ProjectService projectService;

    private Team team;
    private Project project;
    private TeamMember pmTeamMember;
    private Role pmRole;
    private Role contributorRole;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        User pmUser = User.builder().id(2L).fullName("PM User").build();
        WorkspaceMember wsMember = WorkspaceMember.builder().id(5L).user(pmUser).build();
        pmTeamMember = TeamMember.builder()
                .id(7L)
                .team(team)
                .workspaceMember(wsMember)
                .status(MemberStatus.ACTIVE)
                .build();
        project = Project.builder()
                .id(30L)
                .team(team)
                .projectManagerMember(pmTeamMember)
                .code("PRJ-001")
                .name("Alpha")
                .status(ProjectStatus.DRAFT)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();
        pmRole = Role.builder().id(1L).name("Project Manager").scope(RoleScope.PROJECT).build();
        contributorRole = Role.builder().id(2L).name("Project Contributor").scope(RoleScope.PROJECT).build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldCreateDraftProject() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Alpha");
        request.setObjective("Objective");
        request.setScope("Scope");
        request.setDescription("Description");
        request.setStartDate(LocalDate.of(2026, 1, 1));
        ProjectResponse response = ProjectResponse.builder().id(30L).name("Alpha").build();
        WorkflowState defaultState = WorkflowState.builder().id(50L).build();
        ProductBacklog backlog = ProductBacklog.builder().id(60L).build();
        Project savedProject = Project.builder()
                .id(30L)
                .team(team)
                .code("PRJ-20260101010101001-1234")
                .name("Alpha")
                .status(ProjectStatus.DRAFT)
                .build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.existsByTeamIdAndNameIgnoreCase(20L, "Alpha")).thenReturn(false);
        when(projectCodeGenerator.generateUnique()).thenReturn("PRJ-20260101010101001-1234");
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(workflowStateRepository.existsByProjectId(savedProject.getId())).thenReturn(false);
        when(workflowStateRepository.save(any(WorkflowState.class))).thenReturn(defaultState);
        when(productBacklogRepository.findByProjectId(savedProject.getId())).thenReturn(Optional.empty());
        when(productBacklogRepository.save(any(ProductBacklog.class))).thenReturn(backlog);
        when(projectMapper.toResponse(savedProject)).thenReturn(response);

        ProjectResponse result = projectService.create(10L, 20L, request);

        assertThat(result.getName()).isEqualTo("Alpha");
        verify(projectMemberRepository, Mockito.never()).save(any(ProjectMember.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnPagedProjects() {
        ProjectResponse response = ProjectResponse.builder().id(30L).name("Alpha").build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(teamService).verifyTeamAccess(team);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(project)));
        when(projectMapper.toResponse(project)).thenReturn(response);

        PageResponse<ProjectResponse> result = projectService.findAll(10L, 20L, 0, 20, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("Alpha");
    }

    @Test
    void findAll_shouldReturnEmptyWhenNoAccessibleProjects() {
        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(teamService).verifyTeamAccess(team);
        doThrow(new BusinessException(ErrorCode.TEAM_ACCESS_DENIED, "denied"))
                .when(teamService).verifyCanManageProject(team);
        when(projectMemberRepository.findManagedProjectIdsByTeamAndUser(20L, 2L, MemberStatus.ACTIVE))
                .thenReturn(List.of());
        when(projectMemberRepository.findParticipatingProjectIdsByTeamAndUser(20L, 2L, MemberStatus.ACTIVE))
                .thenReturn(List.of());

        PageResponse<ProjectResponse> result = projectService.findAll(10L, 20L, 0, 20, null, null);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(projectRepository, Mockito.never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void update_shouldUpdateProjectInfo() {
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("Beta");
        request.setDescription("New description");
        request.setObjective("New objective");
        request.setScope("New scope");

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).name("Beta").build()
        );

        ProjectResponse result = projectService.update(10L, 20L, 30L, request);

        assertThat(result.getName()).isEqualTo("Beta");
        assertThat(project.getName()).isEqualTo("Beta");
    }

    @Test
    void update_shouldNotSaveWhenNoChanges() {
        UpdateProjectRequest request = new UpdateProjectRequest();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(ProjectResponse.builder().id(30L).name("Alpha").build());

        ProjectResponse result = projectService.update(10L, 20L, 30L, request);

        assertThat(result.getName()).isEqualTo("Alpha");
    }

    @Test
    void update_shouldRejectPmChangeForProjectManager() {
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setProjectManagerMemberId(8L);

        TeamMember otherMember = TeamMember.builder()
                .id(8L)
                .team(team)
                .workspaceMember(WorkspaceMember.builder()
                        .id(6L)
                        .user(User.builder().id(3L).fullName("Other").build())
                        .build())
                .status(MemberStatus.ACTIVE)
                .build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doThrow(new BusinessException(ErrorCode.TEAM_ACCESS_DENIED, "denied"))
                .when(teamService).verifyCanManageProject(team);

        assertThatThrownBy(() -> projectService.update(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TEAM_ACCESS_DENIED);
    }

    @Test
    void update_shouldRejectWhenNotTeamLeaderOrPm() {
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("Beta");

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doThrow(new BusinessException(ErrorCode.TEAM_ACCESS_DENIED, "denied"))
                .when(teamService).verifyCanManageProject(team);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);

        assertThatThrownBy(() -> projectService.update(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void activate_shouldChangeDraftToActive() {
        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).status(ProjectStatus.ACTIVE).build()
        );

        projectService.activate(10L, 20L, 30L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(activityLogService).recordProjectEvent(
                2L,
                "PROJECT_ACTIVATED",
                "PROJECT",
                30L,
                "Alpha",
                project
        );
        verify(notificationService).notifyProjectActivated(eq(project), anyList());
    }

    @Test
    void activate_shouldRejectNonDraftProject() {
        project.setStatus(ProjectStatus.ACTIVE);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);

        assertThatThrownBy(() -> projectService.activate(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void activate_shouldRejectWhenProjectManagerMissing() {
        project.setProjectManagerMember(null);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);

        assertThatThrownBy(() -> projectService.activate(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void activate_shouldRejectWhenProjectManagerInactive() {
        pmTeamMember.setStatus(MemberStatus.INACTIVE);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);

        assertThatThrownBy(() -> projectService.activate(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void complete_shouldChangeActiveToCompleted() {
        project.setStatus(ProjectStatus.ACTIVE);
        Role teamLeaderRole = Role.builder().id(3L).name("Team Leader").scope(RoleScope.TEAM).build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(30L)).thenReturn(List.of());
        when(roleRepository.findByNameAndScope("Team Leader", RoleScope.TEAM))
                .thenReturn(Optional.of(teamLeaderRole));
        when(teamMemberRepository.findByTeamIdAndRole_IdAndStatus(20L, 3L, MemberStatus.ACTIVE))
                .thenReturn(List.of());
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).status(ProjectStatus.COMPLETED).build()
        );

        ProjectResponse result = projectService.complete(10L, 20L, 30L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(activityLogService).recordProjectEvent(
                2L,
                "PROJECT_COMPLETED",
                "PROJECT",
                30L,
                "Alpha",
                project
        );
        verify(notificationService).notifyProjectCompleted(eq(project), anyList());
    }

    @Test
    void complete_shouldRejectWhenTasksAreUnfinished() {
        project.setStatus(ProjectStatus.ACTIVE);
        Task openTask = Task.builder().id(91L).status(TaskStatus.IN_PROGRESS).build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(30L)).thenReturn(List.of(openTask));

        assertThatThrownBy(() -> projectService.complete(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(projectRepository, Mockito.never()).save(any(Project.class));
        verify(activityLogService, Mockito.never()).recordProjectEvent(any(), any(), any(), any(), any(), any());
        verify(notificationService, Mockito.never()).notifyProjectCompleted(any(), anyList());
    }

    @Test
    void complete_shouldRejectNonPm() {
        project.setStatus(ProjectStatus.ACTIVE);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.complete(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void archive_shouldChangeCompletedToArchived() {
        project.setStatus(ProjectStatus.COMPLETED);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).status(ProjectStatus.ARCHIVED).build()
        );

        projectService.archive(10L, 20L, 30L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        verify(activityLogService).recordProjectEvent(
                2L,
                "PROJECT_ARCHIVED",
                "PROJECT",
                30L,
                "Alpha",
                project
        );
    }

    @Test
    void archive_shouldRejectNonCompletedProject() {
        project.setStatus(ProjectStatus.ACTIVE);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);

        assertThatThrownBy(() -> projectService.archive(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void archive_shouldRejectAlreadyArchivedProject() {
        project.setStatus(ProjectStatus.ARCHIVED);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.archive(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dự án đã được lưu trữ");
    }

}
