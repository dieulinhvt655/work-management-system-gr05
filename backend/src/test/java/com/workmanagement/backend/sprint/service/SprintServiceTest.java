package com.workmanagement.backend.sprint.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
import com.workmanagement.backend.productbacklog.mapper.ProductBacklogItemMapper;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogItemRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.sprint.dto.request.CompleteSprintRequest;
import com.workmanagement.backend.sprint.dto.request.CreateSprintRequest;
import com.workmanagement.backend.sprint.dto.response.SprintProgressResponse;
import com.workmanagement.backend.sprint.dto.response.SprintResponse;
import com.workmanagement.backend.sprint.entity.Sprint;
import com.workmanagement.backend.sprint.mapper.SprintMapper;
import com.workmanagement.backend.sprint.repository.SprintRepository;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.service.TeamService;
import com.workmanagement.backend.workspace.entity.Workspace;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private SprintMapper sprintMapper;
    @Mock
    private ProjectService projectService;
    @Mock
    private TeamService teamService;
    @Mock
    private ProductBacklogItemRepository productBacklogItemRepository;
    @Mock
    private ProductBacklogItemMapper productBacklogItemMapper;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private SprintService sprintService;

    private Project project;
    private Sprint sprint;
    private ProductBacklogItem pbi;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        Team team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        project = Project.builder().id(30L).team(team).status(ProjectStatus.ACTIVE).build();

        sprint = Sprint.builder()
                .id(80L)
                .project(project)
                .name("Sprint 1")
                .goal("Ship login")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .status(SprintStatus.PLANNING)
                .build();

        ProductBacklog backlog = ProductBacklog.builder().id(50L).project(project).build();
        pbi = ProductBacklogItem.builder()
                .id(60L)
                .backlog(backlog)
                .title("Login")
                .status(PbiStatus.READY)
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldCreatePlanningSprint() {
        CreateSprintRequest request = new CreateSprintRequest();
        request.setName("Sprint 1");
        request.setGoal("Ship login");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(14));

        SprintResponse response = SprintResponse.builder().id(80L).name("Sprint 1").status(SprintStatus.PLANNING).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.save(any(Sprint.class))).thenReturn(sprint);
        when(productBacklogItemRepository.countBySprintId(80L)).thenReturn(0L);
        when(taskRepository.countBySprintId(80L)).thenReturn(0L);
        when(sprintMapper.toResponse(sprint, 0L, 0L)).thenReturn(response);
        doNothing().when(activityLogService).recordProjectEvent(any(), any(), any(), any(), any(), eq(project));

        SprintResponse result = sprintService.create(10L, 20L, 30L, request);

        assertThat(result.getName()).isEqualTo("Sprint 1");
        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    void create_shouldRejectNonProjectManager() {
        CreateSprintRequest request = new CreateSprintRequest();
        request.setName("Sprint 1");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(14));

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(false);

        assertThatThrownBy(() -> sprintService.create(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void start_shouldActivateSprint() {
        SprintResponse response = SprintResponse.builder().id(80L).status(SprintStatus.ACTIVE).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByProjectIdAndStatusAndIdNot(30L, SprintStatus.ACTIVE, 80L)).thenReturn(false);
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(productBacklogItemRepository.countBySprintId(80L)).thenReturn(0L);
        when(taskRepository.countBySprintId(80L)).thenReturn(0L);
        when(sprintMapper.toResponse(sprint, 0L, 0L)).thenReturn(response);
        doNothing().when(activityLogService).recordProjectEvent(any(), any(), any(), any(), any(), eq(project));

        SprintResponse result = sprintService.start(10L, 20L, 30L, 80L);

        assertThat(result.getStatus()).isEqualTo(SprintStatus.ACTIVE);
        assertThat(sprint.getStatus()).isEqualTo(SprintStatus.ACTIVE);
    }

    @Test
    void start_shouldRejectWhenAnotherActiveSprintExists() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.existsByProjectIdAndStatusAndIdNot(30L, SprintStatus.ACTIVE, 80L)).thenReturn(true);

        assertThatThrownBy(() -> sprintService.start(10L, 20L, 30L, 80L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPRINT_ALREADY_ACTIVE);
    }

    @Test
    void addPbi_shouldAssignReadyPbiToSprint() {
        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder()
                .id(60L)
                .sprintId(80L)
                .status(PbiStatus.IN_SPRINT)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));
        when(productBacklogItemRepository.save(pbi)).thenReturn(pbi);
        when(productBacklogItemMapper.toResponse(pbi)).thenReturn(response);
        doNothing().when(activityLogService).recordProjectEvent(any(), any(), any(), any(), any(), eq(project));

        ProductBacklogItemResponse result = sprintService.addPbi(10L, 20L, 30L, 80L, 60L);

        assertThat(result.getStatus()).isEqualTo(PbiStatus.IN_SPRINT);
        assertThat(pbi.getSprintId()).isEqualTo(80L);
    }

    @Test
    void addPbi_shouldRejectPbiNotReady() {
        pbi.setStatus(PbiStatus.NEW);

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));

        assertThatThrownBy(() -> sprintService.addPbi(10L, 20L, 30L, 80L, 60L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPRINT_PBI_INVALID);
    }

    @Test
    void getProgress_shouldAllowTeamLeader() {
        sprint.setStatus(SprintStatus.ACTIVE);
        Task doneTask = Task.builder()
                .id(90L)
                .pbi(pbi)
                .sprintId(80L)
                .status(TaskStatus.DONE)
                .progress(100)
                .completedAt(LocalDateTime.now())
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(false);
        when(teamService.isActiveTeamLeader(project.getTeam())).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findBySprintIdOrderByCreatedAtDesc(80L)).thenReturn(List.of(doneTask));

        SprintProgressResponse result = sprintService.getProgress(10L, 20L, 30L, 80L);

        assertThat(result.getDoneTasks()).isEqualTo(1);
        assertThat(result.getCompletionPercent()).isEqualTo(100);
    }

    @Test
    void getProgress_shouldRejectRegularMember() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(false);
        when(teamService.isActiveTeamLeader(project.getTeam())).thenReturn(false);

        assertThatThrownBy(() -> sprintService.getProgress(10L, 20L, 30L, 80L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPRINT_ACCESS_DENIED);
    }

    @Test
    void complete_shouldFinishActiveSprint() {
        sprint.setStatus(SprintStatus.ACTIVE);
        pbi.setSprintId(80L);
        pbi.setStatus(PbiStatus.IN_SPRINT);

        Task doneTask = Task.builder()
                .id(90L)
                .pbi(pbi)
                .sprintId(80L)
                .status(TaskStatus.DONE)
                .progress(100)
                .completedAt(LocalDateTime.now())
                .build();

        CompleteSprintRequest request = new CompleteSprintRequest();
        request.setSummary("Sprint delivered login feature");

        SprintResponse response = SprintResponse.builder()
                .id(80L)
                .status(SprintStatus.COMPLETED)
                .summary("Sprint delivered login feature")
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findBySprintIdOrderByCreatedAtDesc(80L)).thenReturn(List.of(doneTask));
        when(productBacklogItemRepository.findBySprintIdOrderByCreatedAtDesc(80L)).thenReturn(List.of(pbi));
        when(productBacklogItemRepository.saveAll(List.of(pbi))).thenReturn(List.of(pbi));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(productBacklogItemRepository.countBySprintId(80L)).thenReturn(1L);
        when(taskRepository.countBySprintId(80L)).thenReturn(1L);
        when(sprintMapper.toResponse(sprint, 1L, 1L)).thenReturn(response);
        doNothing().when(activityLogService).recordProjectEvent(any(), any(), any(), any(), any(), eq(project));

        SprintResponse result = sprintService.complete(10L, 20L, 30L, 80L, request);

        assertThat(result.getStatus()).isEqualTo(SprintStatus.COMPLETED);
        assertThat(pbi.getStatus()).isEqualTo(PbiStatus.COMPLETED);
    }

    @Test
    void complete_shouldRejectOpenTasks() {
        sprint.setStatus(SprintStatus.ACTIVE);
        Task openTask = Task.builder()
                .id(90L)
                .pbi(pbi)
                .sprintId(80L)
                .status(TaskStatus.IN_PROGRESS)
                .progress(50)
                .build();

        CompleteSprintRequest request = new CompleteSprintRequest();
        request.setSummary("Done");

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findBySprintIdOrderByCreatedAtDesc(80L)).thenReturn(List.of(openTask));

        assertThatThrownBy(() -> sprintService.complete(10L, 20L, 30L, 80L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPRINT_CANNOT_COMPLETE);
    }

}
