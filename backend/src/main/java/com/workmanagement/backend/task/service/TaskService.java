package com.workmanagement.backend.task.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.PriorityLevel;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Set<PbiStatus> REFINEMENT_PBI_STATUSES = Set.of(
            PbiStatus.NEW, PbiStatus.READY, PbiStatus.ON_HOLD
    );
    private static final Set<TaskStatus> PREPARATION_DELETABLE = Set.of(TaskStatus.TO_DO, TaskStatus.CANCELLED);
    private static final Set<TaskStatus> SPRINT_DELETABLE = Set.of(TaskStatus.TO_DO, TaskStatus.CANCELLED);
    private static final Set<SprintStatus> SPRINT_TASK_ALLOWED = Set.of(SprintStatus.PLANNING, SprintStatus.ACTIVE);
    private static final Set<PbiStatus> SPRINT_PBI_STATUSES = Set.of(PbiStatus.IN_SPRINT);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProductBacklogItemRepository productBacklogItemRepository;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;
    private final WorkflowStateService workflowStateService;
    private final WorkflowTransitionService workflowTransitionService;
    private final ActivityLogService activityLogService;

    // --- UC 4.5-4.8: Preparation tasks ---

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('task:read')")
    public List<TaskResponse> findPreparationTasks(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        ProductBacklogItem pbi = getPbi(projectId, itemId);

        return taskRepository.findByPbiIdOrderByCreatedAtDesc(pbi.getId())
                .stream()
                .filter(Task::isPreparationTask)
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('task:read')")
    public TaskResponse findPreparationTaskById(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getPbi(projectId, itemId);

        Task task = getPreparationTask(itemId, taskId);
        return taskMapper.toResponse(task);
    }

    /** UC-4.5 — Phân rã PBI thành task chuẩn bị */
    @Transactional
    @PreAuthorize("hasAuthority('task:create')")
    public TaskResponse createPreparationTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId,
            CreateTaskRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);

        ProductBacklogItem pbi = getPbi(projectId, itemId);
        ensurePbiAcceptsRefinement(pbi);

        ProjectMember reporter = resolveReporter(project);
        Task parentTask = resolveParentTask(pbi.getId(), request.getParentTaskId(), null);

        Task task = Task.builder()
                .pbi(pbi)
                .parentTask(parentTask)
                .reporterMember(reporter)
                .assigneeMember(resolveOptionalMember(project.getId(), request.getAssigneeMemberId()))
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : PriorityLevel.MEDIUM)
                .status(TaskStatus.TO_DO)
                .progress(0)
                .deadline(request.getDeadline())
                .build();

        validateDateRange(task.getStartDate(), task.getDeadline());
        task = taskRepository.save(task);

        logTaskEvent(project, ActivityLogAction.TASK_CREATED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-4.6 — Cập nhật task chuẩn bị */
    @Transactional
    @PreAuthorize("hasAuthority('task:update')")
    public TaskResponse updatePreparationTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId,
            Long taskId,
            UpdateTaskRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);

        Task task = getPreparationTask(itemId, taskId);
        ensurePreparationEditable(task);
        applyTaskUpdate(task, request);

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_UPDATED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-4.7 — Xóa task chuẩn bị */
    @Transactional
    @PreAuthorize("hasAuthority('task:delete')")
    public void deletePreparationTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);

        Task task = getPreparationTask(itemId, taskId);

        if (!PREPARATION_DELETABLE.contains(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_CANNOT_DELETE, "Chỉ task chưa bắt đầu mới được xóa");
        }
        if (taskRepository.existsByParentTaskId(task.getId())) {
            throw new BusinessException(ErrorCode.TASK_CANNOT_DELETE, "Không thể xóa task có sub-task");
        }

        String title = task.getTitle();
        taskRepository.delete(task);
        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TASK_DELETED,
                ActivityLogAction.TARGET_TASK,
                taskId,
                title,
                project
        );
    }

    /** UC-4.8 — Gán thành viên dự kiến cho task chuẩn bị */
    @Transactional
    @PreAuthorize("hasAuthority('task:assign')")
    public TaskResponse assignPreparationTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId,
            Long taskId,
            AssignTaskRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);

        Task task = getPreparationTask(itemId, taskId);
        ensurePreparationEditable(task);

        task.setAssigneeMember(resolveActiveMember(project.getId(), request.getAssigneeMemberId()));
        if (request.getReviewerMemberId() != null) {
            task.setReviewerMember(resolveActiveMember(project.getId(), request.getReviewerMemberId()));
        }

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_ASSIGNED, task);
        return taskMapper.toResponse(task);
    }

    // --- UC 5.3-5.7: Sprint tasks ---

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('task:read')")
    public List<TaskResponse> findSprintTasks(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getSprint(projectId, sprintId);

        return taskRepository.findBySprintIdOrderByCreatedAtDesc(sprintId)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('task:read')")
    public TaskResponse findSprintTaskById(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);
        return taskMapper.toResponse(task);
    }

    /** UC-5.3 — Tạo task trong sprint */
    @Transactional
    @PreAuthorize("hasAuthority('task:create')")
    public TaskResponse createSprintTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long itemId,
            CreateTaskRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);

        Sprint sprint = getActiveSprint(projectId, sprintId);
        ProductBacklogItem pbi = getPbi(projectId, itemId);
        ensurePbiInSprint(pbi, sprint.getId());
        WorkflowState defaultState = workflowStateService.ensureDefaultWorkflow(project);

        ProjectMember reporter = resolveReporter(project);
        Task parentTask = resolveParentTask(pbi.getId(), request.getParentTaskId(), sprintId);

        Task task = Task.builder()
                .pbi(pbi)
                .sprintId(sprint.getId())
                .parentTask(parentTask)
                .reporterMember(reporter)
                .assigneeMember(resolveOptionalMember(project.getId(), request.getAssigneeMemberId()))
                .workflowState(defaultState)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : PriorityLevel.MEDIUM)
                .status(TaskStatus.TO_DO)
                .progress(0)
                .deadline(request.getDeadline())
                .build();

        validateDateRange(task.getStartDate(), task.getDeadline());
        task = taskRepository.save(task);

        logTaskEvent(project, ActivityLogAction.TASK_CREATED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-5.3 — Cập nhật task trong sprint */
    @Transactional
    @PreAuthorize("hasAuthority('task:update')")
    public TaskResponse updateSprintTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId,
            UpdateTaskRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);
        getActiveSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);
        ensureSprintTaskEditable(task);
        applyTaskUpdate(task, request);

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_UPDATED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-5.3 — Kích hoạt task chuẩn bị đưa vào sprint */
    @Transactional
    @PreAuthorize("hasAuthority('task:update')")
    public TaskResponse activatePreparationTaskInSprint(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long itemId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);

        Sprint sprint = getActiveSprint(projectId, sprintId);
        ProductBacklogItem pbi = getPbi(projectId, itemId);
        ensurePbiInSprint(pbi, sprint.getId());

        Task task = getPreparationTask(itemId, taskId);
        ensurePreparationEditable(task);

        WorkflowState defaultState = workflowStateService.ensureDefaultWorkflow(project);
        task.setSprintId(sprint.getId());
        task.setWorkflowState(defaultState);
        task.setStatus(TaskStatus.TO_DO);
        task.setProgress(0);
        task.setCompletedAt(null);

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_ACTIVATED_IN_SPRINT, task);
        return taskMapper.toResponse(task);
    }

    /** UC-5.3 — Xóa task trong sprint */
    @Transactional
    @PreAuthorize("hasAuthority('task:delete')")
    public void deleteSprintTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsTasks(project);
        getActiveSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);

        if (!SPRINT_DELETABLE.contains(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_CANNOT_DELETE, "Chỉ task chưa bắt đầu mới được xóa");
        }
        if (taskRepository.existsByParentTaskId(task.getId())) {
            throw new BusinessException(ErrorCode.TASK_CANNOT_DELETE, "Không thể xóa task có sub-task");
        }

        String title = task.getTitle();
        taskRepository.delete(task);
        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TASK_DELETED,
                ActivityLogAction.TARGET_TASK,
                taskId,
                title,
                project
        );
    }

    /** UC-5.4 — Rà soát và xác nhận phân công */
    @Transactional
    @PreAuthorize("hasAuthority('task:assign')")
    public TaskResponse confirmAssignment(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId,
            AssignTaskRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        getActiveSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);

        if (task.getStatus() != TaskStatus.TO_DO) {
            throw new BusinessException(ErrorCode.TASK_INVALID_STATUS, "Chỉ xác nhận phân công khi task chưa bắt đầu");
        }

        ProjectMember assignee = resolveActiveMember(project.getId(), request.getAssigneeMemberId());
        task.setAssigneeMember(assignee);
        if (request.getReviewerMemberId() != null) {
            task.setReviewerMember(resolveActiveMember(project.getId(), request.getReviewerMemberId()));
        }

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_ASSIGNMENT_CONFIRMED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-5.5 — Bắt đầu thực hiện công việc */
    @Transactional
    @PreAuthorize("hasAuthority('task:update')")
    public TaskResponse startWork(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        getActiveSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);
        verifyCanExecuteTask(project, task);

        if (task.getStatus() != TaskStatus.TO_DO && task.getStatus() != TaskStatus.REOPENED) {
            throw new BusinessException(ErrorCode.TASK_INVALID_STATUS, "Task đã được bắt đầu hoặc hoàn thành");
        }
        if (task.getAssigneeMember() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Task chưa được gán người thực hiện");
        }

        WorkflowState currentState = task.getWorkflowState();
        WorkflowState inProgressState = workflowStateService.getStateByCode(project.getId(), "in_progress");
        if (currentState != null) {
            workflowTransitionService.validateTransition(
                    project.getId(), currentState.getId(), inProgressState.getId()
            );
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setWorkflowState(inProgressState);
        if (task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }
        if (task.getProgress() == 0) {
            task.setProgress(1);
        }

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_STARTED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-5.5 — Cập nhật tiến độ công việc */
    @Transactional
    @PreAuthorize("hasAuthority('task:update')")
    public TaskResponse updateProgress(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId,
            UpdateTaskProgressRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        getActiveSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);
        verifyCanExecuteTask(project, task);

        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.TASK_INVALID_STATUS, "Task đã kết thúc");
        }

        int progress = request.getProgress();
        task.setProgress(progress);

        if (progress > 0 && task.getStatus() == TaskStatus.TO_DO) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setWorkflowState(workflowStateService.getStateByCode(project.getId(), "in_progress"));
            if (task.getStartDate() == null) {
                task.setStartDate(LocalDate.now());
            }
        }

        if (progress >= 100) {
            WorkflowState currentState = task.getWorkflowState();
            WorkflowState reviewState = workflowStateService.getStateByCode(project.getId(), "review");
            if (currentState != null) {
                workflowTransitionService.validateTransition(
                        project.getId(), currentState.getId(), reviewState.getId()
                );
            }
            task.setStatus(TaskStatus.REVIEW);
            task.setWorkflowState(reviewState);
            task.setProgress(100);
        }

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_PROGRESS_UPDATED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-5.7 — Kiểm tra và phê duyệt công việc */
    @Transactional
    @PreAuthorize("hasAuthority('task:update')")
    public TaskResponse approveTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        getActiveSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);
        verifyCanApproveTask(project, task);

        if (task.getStatus() != TaskStatus.REVIEW) {
            throw new BusinessException(ErrorCode.TASK_INVALID_STATUS, "Chỉ phê duyệt task đang chờ review");
        }

        WorkflowState currentState = task.getWorkflowState();
        WorkflowState doneState = workflowStateService.getStateByCode(project.getId(), "done");
        if (currentState != null) {
            workflowTransitionService.validateTransition(
                    project.getId(), currentState.getId(), doneState.getId()
            );
        }

        task.setStatus(TaskStatus.DONE);
        task.setWorkflowState(doneState);
        task.setProgress(100);
        task.setCompletedAt(LocalDateTime.now());

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_APPROVED, task);
        return taskMapper.toResponse(task);
    }

    /** UC-5.7 — Từ chối và mở lại công việc */
    @Transactional
    @PreAuthorize("hasAuthority('task:update')")
    public TaskResponse rejectTask(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long taskId,
            RejectTaskRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        getActiveSprint(projectId, sprintId);

        Task task = getSprintTask(sprintId, taskId);
        verifyCanApproveTask(project, task);

        if (task.getStatus() != TaskStatus.REVIEW) {
            throw new BusinessException(ErrorCode.TASK_INVALID_STATUS, "Chỉ từ chối task đang chờ review");
        }

        WorkflowState currentState = task.getWorkflowState();
        WorkflowState inProgressState = workflowStateService.getStateByCode(project.getId(), "in_progress");
        if (currentState != null) {
            workflowTransitionService.validateTransition(
                    project.getId(), currentState.getId(), inProgressState.getId()
            );
        }

        task.setStatus(TaskStatus.REOPENED);
        task.setWorkflowState(inProgressState);
        task.setProgress(Math.min(task.getProgress(), 90));
        task.setCompletedAt(null);
        if (request != null && StringUtils.hasText(request.getReason())) {
            String note = "[Từ chối] " + request.getReason().trim();
            task.setDescription(task.getDescription() == null ? note : task.getDescription() + "\n" + note);
        }

        task = taskRepository.save(task);
        logTaskEvent(project, ActivityLogAction.TASK_REJECTED, task);
        return taskMapper.toResponse(task);
    }

    // --- Helpers ---

    private ProductBacklogItem getPbi(Long projectId, Long itemId) {
        return productBacklogItemRepository.findByIdAndBacklog_Project_Id(itemId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PBI_NOT_FOUND, "Không tìm thấy PBI"));
    }

    private Task getPreparationTask(Long pbiId, Long taskId) {
        return taskRepository.findByIdAndPbiId(taskId, pbiId)
                .filter(Task::isPreparationTask)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND, "Không tìm thấy task chuẩn bị"));
    }

    private Task getSprintTask(Long sprintId, Long taskId) {
        return taskRepository.findByIdAndSprintId(taskId, sprintId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND, "Không tìm thấy task sprint"));
    }

    private Sprint getSprint(Long projectId, Long sprintId) {
        return sprintRepository.findByIdAndProjectId(sprintId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPRINT_NOT_FOUND, "Không tìm thấy sprint"));
    }

    private Sprint getActiveSprint(Long projectId, Long sprintId) {
        Sprint sprint = getSprint(projectId, sprintId);
        if (!SPRINT_TASK_ALLOWED.contains(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Sprint không còn hoạt động");
        }
        return sprint;
    }

    private void ensurePbiAcceptsRefinement(ProductBacklogItem pbi) {
        if (!REFINEMENT_PBI_STATUSES.contains(pbi.getStatus())) {
            throw new BusinessException(ErrorCode.PBI_INVALID_STATUS, "PBI không ở trạng thái cho phép phân rã task");
        }
    }

    private void ensurePbiInSprint(ProductBacklogItem pbi, Long sprintId) {
        if (!SPRINT_PBI_STATUSES.contains(pbi.getStatus()) || !sprintId.equals(pbi.getSprintId())) {
            throw new BusinessException(
                    ErrorCode.SPRINT_PBI_INVALID,
                    "PBI phải thuộc sprint và ở trạng thái in_sprint"
            );
        }
    }

    private void ensurePreparationEditable(Task task) {
        if (!task.isPreparationTask()) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "Không phải task chuẩn bị");
        }
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.TASK_INVALID_STATUS, "Task đã kết thúc");
        }
    }

    private void ensureSprintTaskEditable(Task task) {
        if (task.isPreparationTask()) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "Không phải task sprint");
        }
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.TASK_INVALID_STATUS, "Task đã kết thúc");
        }
    }

    private void applyTaskUpdate(Task task, UpdateTaskRequest request) {
        if (StringUtils.hasText(request.getTitle())) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getProgress() != null && !task.isPreparationTask()) {
            task.setProgress(request.getProgress());
        }
        validateDateRange(task.getStartDate(), task.getDeadline());
    }

    private Task resolveParentTask(Long pbiId, Long parentTaskId, Long sprintId) {
        if (parentTaskId == null) {
            return null;
        }
        Task parent = taskRepository.findByIdAndPbiId(parentTaskId, pbiId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND, "Không tìm thấy task cha"));

        if (sprintId == null && !parent.isPreparationTask()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Task cha phải là task chuẩn bị");
        }
        if (sprintId != null && !sprintId.equals(parent.getSprintId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Task cha phải cùng sprint");
        }
        return parent;
    }

    private ProjectMember resolveReporter(Project project) {
        return projectMemberRepository
                .findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                        project.getId(),
                        SecurityUtils.getCurrentUserId(),
                        MemberStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PROJECT_MEMBER_NOT_FOUND,
                        "Bạn cần là thành viên dự án để tạo task"
                ));
    }

    private ProjectMember resolveActiveMember(Long projectId, Long memberId) {
        return projectMemberRepository.findByIdAndProjectId(memberId, projectId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PROJECT_MEMBER_NOT_FOUND,
                        "Thành viên không hợp lệ"
                ));
    }

    private ProjectMember resolveOptionalMember(Long projectId, Long memberId) {
        if (memberId == null) {
            return null;
        }
        return resolveActiveMember(projectId, memberId);
    }

    private void verifyProjectManager(Project project) {
        if (!projectService.isActiveProjectManager(project)) {
            throw new BusinessException(
                    ErrorCode.PROJECT_ACCESS_DENIED,
                    "Chỉ Project Manager mới được thực hiện thao tác này"
            );
        }
    }

    private void verifyCanExecuteTask(Project project, Task task) {
        if (projectService.isActiveProjectManager(project)) {
            return;
        }
        if (isActiveAssignee(task)) {
            return;
        }
        throw new BusinessException(ErrorCode.TASK_ACCESS_DENIED, "Không có quyền thực hiện task này");
    }

    private void verifyCanApproveTask(Project project, Task task) {
        if (projectService.isActiveProjectManager(project)) {
            return;
        }
        if (isActiveReviewer(task)) {
            return;
        }
        throw new BusinessException(ErrorCode.TASK_ACCESS_DENIED, "Không có quyền phê duyệt task này");
    }

    private boolean isActiveAssignee(Task task) {
        ProjectMember assignee = task.getAssigneeMember();
        return assignee != null
                && assignee.getStatus() == MemberStatus.ACTIVE
                && assignee.getTeamMember().getWorkspaceMember().getUser().getId()
                .equals(SecurityUtils.getCurrentUserId());
    }

    private boolean isActiveReviewer(Task task) {
        ProjectMember reviewer = task.getReviewerMember();
        return reviewer != null
                && reviewer.getStatus() == MemberStatus.ACTIVE
                && reviewer.getTeamMember().getWorkspaceMember().getUser().getId()
                .equals(SecurityUtils.getCurrentUserId());
    }

    private void ensureProjectAcceptsTasks(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
    }

    private void validateDateRange(LocalDate start, LocalDate deadline) {
        if (start != null && deadline != null && deadline.isBefore(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Deadline phải sau ngày bắt đầu");
        }
    }

    private void logTaskEvent(Project project, String action, Task task) {
        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                action,
                ActivityLogAction.TARGET_TASK,
                task.getId(),
                task.getTitle(),
                project
        );
    }

}
