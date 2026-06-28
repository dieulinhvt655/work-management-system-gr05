package com.workmanagement.backend.task.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.PriorityLevel;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogItemRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.sprint.entity.Sprint;
import com.workmanagement.backend.sprint.repository.SprintRepository;
import com.workmanagement.backend.task.dto.request.AssignTaskRequest;
import com.workmanagement.backend.task.dto.request.CreateTaskRequest;
import com.workmanagement.backend.task.dto.request.UpdateTaskProgressRequest;
import com.workmanagement.backend.task.dto.request.RejectTaskRequest;
import com.workmanagement.backend.task.dto.request.UpdateTaskRequest;
import com.workmanagement.backend.task.dto.response.TaskResponse;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.entity.WorkflowState;
import com.workmanagement.backend.task.mapper.TaskMapper;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.user.entity.User;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private ProductBacklogItemRepository productBacklogItemRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private WorkflowStateService workflowStateService;
    @Mock
    private WorkflowTransitionService workflowTransitionService;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private TaskService taskService;

    private Project project;
    private ProductBacklogItem pbi;
    private ProjectMember reporter;
    private ProjectMember assignee;
    private Task preparationTask;
    private Task sprintTask;
    private Sprint sprint;
    private WorkflowState toDoState;
    private WorkflowState inProgressState;
    private WorkflowState reviewState;
    private WorkflowState doneState;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        Team team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        project = Project.builder().id(30L).team(team).status(ProjectStatus.ACTIVE).build();

        ProductBacklog backlog = ProductBacklog.builder().id(50L).project(project).build();
        pbi = ProductBacklogItem.builder()
                .id(60L)
                .backlog(backlog)
                .title("Login")
                .status(PbiStatus.READY)
                .build();

        User pmUser = User.builder().id(2L).fullName("PM").build();
        User assigneeUser = User.builder().id(3L).fullName("Dev").build();
        WorkspaceMember pmWs = WorkspaceMember.builder().id(5L).user(pmUser).build();
        WorkspaceMember devWs = WorkspaceMember.builder().id(6L).user(assigneeUser).build();
        TeamMember pmTm = TeamMember.builder().id(7L).workspaceMember(pmWs).build();
        TeamMember devTm = TeamMember.builder().id(8L).workspaceMember(devWs).build();
        reporter = ProjectMember.builder().id(11L).teamMember(pmTm).status(MemberStatus.ACTIVE).build();
        assignee = ProjectMember.builder().id(12L).teamMember(devTm).status(MemberStatus.ACTIVE).build();

        preparationTask = Task.builder()
                .id(70L)
                .pbi(pbi)
                .reporterMember(reporter)
                .title("Design login")
                .priority(PriorityLevel.HIGH)
                .status(TaskStatus.TO_DO)
                .progress(0)
                .build();

        sprint = Sprint.builder()
                .id(80L)
                .project(project)
                .name("Sprint 1")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .status(SprintStatus.ACTIVE)
                .build();

        toDoState = WorkflowState.builder().id(1L).project(project).code("to_do").name("To Do").build();
        inProgressState = WorkflowState.builder().id(2L).project(project).code("in_progress").name("In Progress").build();
        reviewState = WorkflowState.builder().id(3L).project(project).code("review").name("Review").build();
        doneState = WorkflowState.builder().id(4L).project(project).code("done").name("Done").isFinal(true).build();

        sprintTask = Task.builder()
                .id(90L)
                .pbi(pbi)
                .sprintId(80L)
                .reporterMember(reporter)
                .assigneeMember(assignee)
                .workflowState(toDoState)
                .title("Implement login")
                .priority(PriorityLevel.HIGH)
                .status(TaskStatus.TO_DO)
                .progress(0)
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void createPreparationTask_shouldSaveTask() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Design login");

        TaskResponse response = TaskResponse.builder().id(70L).title("Design login").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.of(reporter));
        when(taskRepository.save(any())).thenReturn(preparationTask);
        when(taskMapper.toResponse(preparationTask)).thenReturn(response);

        TaskResponse result = taskService.createPreparationTask(10L, 20L, 30L, 60L, request);

        assertThat(result.getTitle()).isEqualTo("Design login");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createPreparationTask_shouldRejectInvalidPbiStatus() {
        pbi.setStatus(PbiStatus.IN_SPRINT);
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Design login");

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));

        assertThatThrownBy(() -> taskService.createPreparationTask(10L, 20L, 30L, 60L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PBI_INVALID_STATUS);
    }

    @Test
    void assignPreparationTask_shouldAssignMember() {
        AssignTaskRequest request = new AssignTaskRequest();
        request.setAssigneeMemberId(12L);

        TaskResponse response = TaskResponse.builder().id(70L).assigneeMemberId(12L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(taskRepository.findByIdAndPbiId(70L, 60L)).thenReturn(Optional.of(preparationTask));
        when(projectMemberRepository.findByIdAndProjectId(12L, 30L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(preparationTask)).thenReturn(preparationTask);
        when(taskMapper.toResponse(preparationTask)).thenReturn(response);

        TaskResponse result = taskService.assignPreparationTask(10L, 20L, 30L, 60L, 70L, request);

        assertThat(result.getAssigneeMemberId()).isEqualTo(12L);
    }

    @Test
    void deletePreparationTask_shouldDeleteTodoTask() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(taskRepository.findByIdAndPbiId(70L, 60L)).thenReturn(Optional.of(preparationTask));
        when(taskRepository.existsByParentTaskId(70L)).thenReturn(false);

        taskService.deletePreparationTask(10L, 20L, 30L, 60L, 70L);

        verify(taskRepository).delete(preparationTask);
    }

    @Test
    void updatePreparationTask_shouldUpdateFields() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated design");

        TaskResponse response = TaskResponse.builder().id(70L).title("Updated design").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(taskRepository.findByIdAndPbiId(70L, 60L)).thenReturn(Optional.of(preparationTask));
        when(taskRepository.save(preparationTask)).thenReturn(preparationTask);
        when(taskMapper.toResponse(preparationTask)).thenReturn(response);

        TaskResponse result = taskService.updatePreparationTask(10L, 20L, 30L, 60L, 70L, request);

        assertThat(result.getTitle()).isEqualTo("Updated design");
    }

    @Test
    void createSprintTask_shouldCreateWithWorkflowState() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Implement login");

        pbi.setSprintId(80L);
        pbi.setStatus(PbiStatus.IN_SPRINT);

        TaskResponse response = TaskResponse.builder().id(90L).title("Implement login").sprintId(80L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));
        when(workflowStateService.ensureDefaultWorkflow(project)).thenReturn(toDoState);
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.of(reporter));
        when(taskRepository.save(any())).thenReturn(sprintTask);
        when(taskMapper.toResponse(sprintTask)).thenReturn(response);

        TaskResponse result = taskService.createSprintTask(10L, 20L, 30L, 80L, 60L, request);

        assertThat(result.getSprintId()).isEqualTo(80L);
    }

    @Test
    void createSprintTask_shouldRejectPbiNotInSprint() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Implement login");

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));

        assertThatThrownBy(() -> taskService.createSprintTask(10L, 20L, 30L, 80L, 60L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPRINT_PBI_INVALID);
    }

    @Test
    void activatePreparationTaskInSprint_shouldMoveTaskToSprint() {
        pbi.setSprintId(80L);
        pbi.setStatus(PbiStatus.IN_SPRINT);

        TaskResponse response = TaskResponse.builder().id(70L).sprintId(80L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));
        when(taskRepository.findByIdAndPbiId(70L, 60L)).thenReturn(Optional.of(preparationTask));
        when(workflowStateService.ensureDefaultWorkflow(project)).thenReturn(toDoState);
        when(taskRepository.save(preparationTask)).thenReturn(preparationTask);
        when(taskMapper.toResponse(preparationTask)).thenReturn(response);

        TaskResponse result = taskService.activatePreparationTaskInSprint(10L, 20L, 30L, 80L, 60L, 70L);

        assertThat(result.getSprintId()).isEqualTo(80L);
        assertThat(preparationTask.getSprintId()).isEqualTo(80L);
    }

    @Test
    void deleteSprintTask_shouldDeleteTodoTask() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findByIdAndSprintId(90L, 80L)).thenReturn(Optional.of(sprintTask));
        when(taskRepository.existsByParentTaskId(90L)).thenReturn(false);

        taskService.deleteSprintTask(10L, 20L, 30L, 80L, 90L);

        verify(taskRepository).delete(sprintTask);
    }

    @Test
    void confirmAssignment_shouldAssignMembers() {
        AssignTaskRequest request = new AssignTaskRequest();
        request.setAssigneeMemberId(12L);
        request.setReviewerMemberId(11L);

        TaskResponse response = TaskResponse.builder().id(90L).assigneeMemberId(12L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findByIdAndSprintId(90L, 80L)).thenReturn(Optional.of(sprintTask));
        when(projectMemberRepository.findByIdAndProjectId(12L, 30L)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.findByIdAndProjectId(11L, 30L)).thenReturn(Optional.of(reporter));
        when(taskRepository.save(sprintTask)).thenReturn(sprintTask);
        when(taskMapper.toResponse(sprintTask)).thenReturn(response);

        TaskResponse result = taskService.confirmAssignment(10L, 20L, 30L, 80L, 90L, request);

        assertThat(result.getAssigneeMemberId()).isEqualTo(12L);
    }

    @Test
    void rejectTask_shouldReopenTask() {
        sprintTask.setStatus(TaskStatus.REVIEW);
        sprintTask.setWorkflowState(reviewState);
        sprintTask.setProgress(100);

        RejectTaskRequest request = new RejectTaskRequest();
        request.setReason("Need more tests");

        TaskResponse response = TaskResponse.builder().id(90L).status(TaskStatus.REOPENED).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findByIdAndSprintId(90L, 80L)).thenReturn(Optional.of(sprintTask));
        when(workflowStateService.getStateByCode(30L, "in_progress")).thenReturn(inProgressState);
        doNothing().when(workflowTransitionService).validateTransition(30L, 3L, 2L);
        when(taskRepository.save(sprintTask)).thenReturn(sprintTask);
        when(taskMapper.toResponse(sprintTask)).thenReturn(response);

        TaskResponse result = taskService.rejectTask(10L, 20L, 30L, 80L, 90L, request);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.REOPENED);
        assertThat(sprintTask.getStatus()).isEqualTo(TaskStatus.REOPENED);
    }

    @Test
    void startWork_shouldMoveToInProgress() {
        TaskResponse response = TaskResponse.builder().id(90L).status(TaskStatus.IN_PROGRESS).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findByIdAndSprintId(90L, 80L)).thenReturn(Optional.of(sprintTask));
        when(workflowStateService.getStateByCode(30L, "in_progress")).thenReturn(inProgressState);
        doNothing().when(workflowTransitionService).validateTransition(30L, 1L, 2L);
        when(taskRepository.save(sprintTask)).thenReturn(sprintTask);
        when(taskMapper.toResponse(sprintTask)).thenReturn(response);

        TaskResponse result = taskService.startWork(10L, 20L, 30L, 80L, 90L);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(sprintTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void approveTask_shouldMarkDone() {
        sprintTask.setStatus(TaskStatus.REVIEW);
        sprintTask.setWorkflowState(reviewState);
        sprintTask.setProgress(100);

        TaskResponse response = TaskResponse.builder().id(90L).status(TaskStatus.DONE).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findByIdAndSprintId(90L, 80L)).thenReturn(Optional.of(sprintTask));
        when(workflowStateService.getStateByCode(30L, "done")).thenReturn(doneState);
        doNothing().when(workflowTransitionService).validateTransition(30L, 3L, 4L);
        when(taskRepository.save(sprintTask)).thenReturn(sprintTask);
        when(taskMapper.toResponse(sprintTask)).thenReturn(response);

        TaskResponse result = taskService.approveTask(10L, 20L, 30L, 80L, 90L);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(sprintTask.getCompletedAt()).isNotNull();
    }

    @Test
    void updateProgress_shouldMoveToReviewAt100() {
        sprintTask.setStatus(TaskStatus.IN_PROGRESS);
        sprintTask.setWorkflowState(inProgressState);
        sprintTask.setProgress(50);

        UpdateTaskProgressRequest request = new UpdateTaskProgressRequest();
        request.setProgress(100);

        TaskResponse response = TaskResponse.builder().id(90L).status(TaskStatus.REVIEW).progress(100).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(sprintRepository.findByIdAndProjectId(80L, 30L)).thenReturn(Optional.of(sprint));
        when(taskRepository.findByIdAndSprintId(90L, 80L)).thenReturn(Optional.of(sprintTask));
        when(workflowStateService.getStateByCode(30L, "review")).thenReturn(reviewState);
        doNothing().when(workflowTransitionService).validateTransition(30L, 2L, 3L);
        when(taskRepository.save(sprintTask)).thenReturn(sprintTask);
        when(taskMapper.toResponse(sprintTask)).thenReturn(response);

        TaskResponse result = taskService.updateProgress(10L, 20L, 30L, 80L, 90L, request);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.REVIEW);
    }

    @Test
    void findPreparationTasks_shouldReturnList() {
        TaskResponse response = TaskResponse.builder().id(70L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(pbi));
        when(taskRepository.findByPbiIdOrderByCreatedAtDesc(60L)).thenReturn(List.of(preparationTask));
        when(taskMapper.toResponse(preparationTask)).thenReturn(response);

        List<TaskResponse> result = taskService.findPreparationTasks(10L, 20L, 30L, 60L);

        assertThat(result).hasSize(1);
    }

}
