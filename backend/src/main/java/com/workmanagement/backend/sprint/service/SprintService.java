package com.workmanagement.backend.sprint.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
import com.workmanagement.backend.productbacklog.mapper.ProductBacklogItemMapper;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogItemRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.sprint.dto.request.CompleteSprintRequest;
import com.workmanagement.backend.sprint.dto.request.CreateSprintRequest;
import com.workmanagement.backend.sprint.dto.request.UpdateSprintRequest;
import com.workmanagement.backend.sprint.dto.response.BurndownPointResponse;
import com.workmanagement.backend.sprint.dto.response.SprintProgressResponse;
import com.workmanagement.backend.sprint.dto.response.SprintResponse;
import com.workmanagement.backend.sprint.entity.Sprint;
import com.workmanagement.backend.sprint.mapper.SprintMapper;
import com.workmanagement.backend.sprint.repository.SprintRepository;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SprintService {

    private static final Set<SprintStatus> EDITABLE_STATUSES = Set.of(SprintStatus.PLANNING, SprintStatus.ACTIVE);
    private static final Set<SprintStatus> PBI_MANAGE_STATUSES = Set.of(SprintStatus.PLANNING, SprintStatus.ACTIVE);
    private static final Set<PbiStatus> ADDABLE_PBI_STATUSES = Set.of(PbiStatus.READY);
    private static final Set<TaskStatus> OPEN_TASK_STATUSES = Set.of(
            TaskStatus.TO_DO, TaskStatus.IN_PROGRESS, TaskStatus.REVIEW, TaskStatus.REOPENED
    );
    private static final Set<SprintStatus> HISTORY_STATUSES = Set.of(SprintStatus.COMPLETED, SprintStatus.CANCELLED);

    private final SprintRepository sprintRepository;
    private final SprintMapper sprintMapper;
    private final ProjectService projectService;
    private final TeamService teamService;
    private final ProductBacklogItemRepository productBacklogItemRepository;
    private final ProductBacklogItemMapper productBacklogItemMapper;
    private final TaskRepository taskRepository;
    private final ActivityLogService activityLogService;

    /** UC-5.1 — Tạo sprint */
    @Transactional
    @PreAuthorize("hasAuthority('sprint:create')")
    public SprintResponse create(
            Long workspaceId,
            Long teamId,
            Long projectId,
            CreateSprintRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsSprintChanges(project);
        validateDateRange(request.getStartDate(), request.getEndDate());

        Sprint sprint = Sprint.builder()
                .project(project)
                .name(request.getName().trim())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(SprintStatus.PLANNING)
                .build();
        sprint = sprintRepository.save(sprint);

        logSprintEvent(project, ActivityLogAction.SPRINT_CREATED, sprint);
        return toResponse(sprint);
    }

    /** UC-5.1 — Chi tiết sprint */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('sprint:read')")
    public SprintResponse findById(Long workspaceId, Long teamId, Long projectId, Long sprintId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        Sprint sprint = getSprint(projectId, sprintId);
        return toResponse(sprint);
    }

    /** UC-5.1 — Danh sách sprint */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('sprint:read')")
    public PageResponse<SprintResponse> findAll(
            Long workspaceId,
            Long teamId,
            Long projectId,
            int page,
            int size,
            SprintStatus status
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Sprint> sprintPage = status == null
                ? sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                : sprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status, pageable);

        List<SprintResponse> items = sprintPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<SprintResponse>builder()
                .items(items)
                .page(sprintPage.getNumber())
                .size(sprintPage.getSize())
                .totalElements(sprintPage.getTotalElements())
                .totalPages(sprintPage.getTotalPages())
                .build();
    }

    /** UC-5.1 — Cập nhật sprint */
    @Transactional
    @PreAuthorize("hasAuthority('sprint:update')")
    public SprintResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            UpdateSprintRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsSprintChanges(project);

        Sprint sprint = getSprint(projectId, sprintId);
        ensureEditable(sprint);

        if (StringUtils.hasText(request.getName())) {
            sprint.setName(request.getName().trim());
        }
        if (request.getGoal() != null) {
            sprint.setGoal(request.getGoal());
        }
        if (request.getStartDate() != null) {
            sprint.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            sprint.setEndDate(request.getEndDate());
        }
        validateDateRange(sprint.getStartDate(), sprint.getEndDate());

        sprint = sprintRepository.save(sprint);
        logSprintEvent(project, ActivityLogAction.SPRINT_UPDATED, sprint);
        return toResponse(sprint);
    }

    /** UC-5.1 — Kích hoạt sprint */
    @Transactional
    @PreAuthorize("hasAuthority('sprint:update')")
    public SprintResponse start(Long workspaceId, Long teamId, Long projectId, Long sprintId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsSprintChanges(project);

        Sprint sprint = getSprint(projectId, sprintId);
        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new BusinessException(ErrorCode.SPRINT_INVALID_STATUS, "Chỉ kích hoạt sprint đang lên kế hoạch");
        }
        if (sprintRepository.existsByProjectIdAndStatusAndIdNot(projectId, SprintStatus.ACTIVE, sprintId)) {
            throw new BusinessException(ErrorCode.SPRINT_ALREADY_ACTIVE, "Dự án đã có sprint đang hoạt động");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        sprint = sprintRepository.save(sprint);

        logSprintEvent(project, ActivityLogAction.SPRINT_STARTED, sprint);
        return toResponse(sprint);
    }

    /** UC-5.1 — Hủy sprint */
    @Transactional
    @PreAuthorize("hasAuthority('sprint:delete')")
    public void cancel(Long workspaceId, Long teamId, Long projectId, Long sprintId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);

        Sprint sprint = getSprint(projectId, sprintId);
        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new BusinessException(ErrorCode.SPRINT_INVALID_STATUS, "Chỉ hủy sprint đang lên kế hoạch");
        }

        releaseSprintPbis(sprint.getId());
        sprint.setStatus(SprintStatus.CANCELLED);
        sprintRepository.save(sprint);

        logSprintEvent(project, ActivityLogAction.SPRINT_CANCELLED, sprint);
    }

    /** UC-5.2 — Thêm PBI vào sprint */
    @Transactional
    @PreAuthorize("hasAuthority('sprint:update')")
    public ProductBacklogItemResponse addPbi(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long itemId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);
        ensureProjectAcceptsSprintChanges(project);

        Sprint sprint = getSprint(projectId, sprintId);
        ensurePbiManageable(sprint);

        ProductBacklogItem item = getPbi(projectId, itemId);
        if (!ADDABLE_PBI_STATUSES.contains(item.getStatus())) {
            throw new BusinessException(
                    ErrorCode.SPRINT_PBI_INVALID,
                    "Chỉ thêm PBI ở trạng thái sẵn sàng triển khai"
            );
        }
        if (item.getSprintId() != null) {
            throw new BusinessException(ErrorCode.SPRINT_PBI_INVALID, "PBI đã thuộc sprint khác");
        }

        item.setSprintId(sprint.getId());
        item.setStatus(PbiStatus.IN_SPRINT);
        item = productBacklogItemRepository.save(item);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.SPRINT_PBI_ADDED,
                ActivityLogAction.TARGET_PBI,
                item.getId(),
                item.getTitle(),
                project
        );

        return productBacklogItemMapper.toResponse(item);
    }

    /** UC-5.2 — Gỡ PBI khỏi sprint */
    @Transactional
    @PreAuthorize("hasAuthority('sprint:update')")
    public void removePbi(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            Long itemId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);

        Sprint sprint = getSprint(projectId, sprintId);
        ensurePbiManageable(sprint);

        ProductBacklogItem item = getPbi(projectId, itemId);
        if (!sprint.getId().equals(item.getSprintId())) {
            throw new BusinessException(ErrorCode.SPRINT_PBI_INVALID, "PBI không thuộc sprint này");
        }

        ensurePbiHasNoOpenSprintTasks(sprint.getId(), item.getId());

        item.setSprintId(null);
        item.setStatus(PbiStatus.READY);
        productBacklogItemRepository.save(item);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.SPRINT_PBI_REMOVED,
                ActivityLogAction.TARGET_PBI,
                item.getId(),
                item.getTitle(),
                project
        );
    }

    /** UC-5.2 — Danh sách PBI trong sprint */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('sprint:read')")
    public List<ProductBacklogItemResponse> listPbis(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);
        getSprint(projectId, sprintId);

        return productBacklogItemRepository.findBySprintIdOrderByCreatedAtDesc(sprintId)
                .stream()
                .map(productBacklogItemMapper::toResponse)
                .toList();
    }

    /** UC-5.6 — Theo dõi tiến độ sprint */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('sprint:read')")
    public SprintProgressResponse getProgress(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyCanViewProgress(project);

        Sprint sprint = getSprint(projectId, sprintId);
        List<Task> tasks = taskRepository.findBySprintIdOrderByCreatedAtDesc(sprintId);

        long todoTasks = countByStatus(tasks, TaskStatus.TO_DO);
        long inProgressTasks = countByStatus(tasks, TaskStatus.IN_PROGRESS);
        long reviewTasks = countByStatus(tasks, TaskStatus.REVIEW);
        long doneTasks = countByStatus(tasks, TaskStatus.DONE);
        long cancelledTasks = countByStatus(tasks, TaskStatus.CANCELLED);
        long totalActiveTasks = tasks.size() - cancelledTasks;
        int completionPercent = totalActiveTasks == 0
                ? 0
                : (int) Math.round((doneTasks * 100.0) / totalActiveTasks);

        return SprintProgressResponse.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .status(sprint.getStatus())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .totalTasks(tasks.size())
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .reviewTasks(reviewTasks)
                .doneTasks(doneTasks)
                .cancelledTasks(cancelledTasks)
                .completionPercent(completionPercent)
                .burndown(buildBurndown(sprint, tasks))
                .build();
    }

    /** UC-5.8 — Tổng kết sprint */
    @Transactional
    @PreAuthorize("hasAuthority('sprint:update')")
    public SprintResponse complete(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long sprintId,
            CompleteSprintRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);

        Sprint sprint = getSprint(projectId, sprintId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.SPRINT_INVALID_STATUS, "Chỉ tổng kết sprint đang hoạt động");
        }

        List<Task> tasks = taskRepository.findBySprintIdOrderByCreatedAtDesc(sprintId);
        boolean hasOpenTasks = tasks.stream().anyMatch(task -> OPEN_TASK_STATUSES.contains(task.getStatus()));
        if (hasOpenTasks) {
            throw new BusinessException(
                    ErrorCode.SPRINT_CANNOT_COMPLETE,
                    "Sprint còn công việc chưa hoàn thành hoặc hủy"
            );
        }

        finalizeSprintPbis(sprint.getId(), tasks);

        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setSummary(request.getSummary().trim());
        sprint.setCompletedAt(LocalDateTime.now());
        sprint = sprintRepository.save(sprint);

        logSprintEvent(project, ActivityLogAction.SPRINT_COMPLETED, sprint);
        return toResponse(sprint);
    }

    /** UC-5.9 — Tra cứu lịch sử sprint */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('sprint:read')")
    public List<SprintResponse> findHistory(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);

        return sprintRepository
                .findByProjectIdAndStatusInOrderByCreatedAtDesc(projectId, List.copyOf(HISTORY_STATUSES))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    Sprint getSprint(Long projectId, Long sprintId) {
        return sprintRepository.findByIdAndProjectId(sprintId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPRINT_NOT_FOUND, "Không tìm thấy sprint"));
    }

    private ProductBacklogItem getPbi(Long projectId, Long itemId) {
        return productBacklogItemRepository.findByIdAndBacklog_Project_Id(itemId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PBI_NOT_FOUND, "Không tìm thấy PBI"));
    }

    private void ensureEditable(Sprint sprint) {
        if (!EDITABLE_STATUSES.contains(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.SPRINT_INVALID_STATUS, "Sprint không còn được chỉnh sửa");
        }
    }

    private void ensurePbiManageable(Sprint sprint) {
        if (!PBI_MANAGE_STATUSES.contains(sprint.getStatus())) {
            throw new BusinessException(ErrorCode.SPRINT_INVALID_STATUS, "Sprint không còn quản lý PBI");
        }
    }

    private void ensurePbiHasNoOpenSprintTasks(Long sprintId, Long pbiId) {
        boolean hasOpenTasks = taskRepository.findBySprintIdAndPbiId(sprintId, pbiId).stream()
                .anyMatch(task -> OPEN_TASK_STATUSES.contains(task.getStatus()));
        if (hasOpenTasks) {
            throw new BusinessException(
                    ErrorCode.SPRINT_PBI_INVALID,
                    "PBI còn công việc sprint chưa kết thúc"
            );
        }
    }

    private void releaseSprintPbis(Long sprintId) {
        List<ProductBacklogItem> pbis = productBacklogItemRepository.findBySprintIdOrderByCreatedAtDesc(sprintId);
        for (ProductBacklogItem item : pbis) {
            item.setSprintId(null);
            item.setStatus(PbiStatus.READY);
        }
        productBacklogItemRepository.saveAll(pbis);
    }

    private void finalizeSprintPbis(Long sprintId, List<Task> tasks) {
        List<ProductBacklogItem> pbis = productBacklogItemRepository.findBySprintIdOrderByCreatedAtDesc(sprintId);
        for (ProductBacklogItem item : pbis) {
            List<Task> pbiTasks = tasks.stream()
                    .filter(task -> item.getId().equals(task.getPbi().getId()))
                    .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                    .toList();

            if (pbiTasks.isEmpty()) {
                item.setSprintId(null);
                item.setStatus(PbiStatus.READY);
                continue;
            }

            if (pbiTasks.stream().allMatch(task -> task.getStatus() == TaskStatus.DONE)) {
                item.setStatus(PbiStatus.COMPLETED);
            } else {
                item.setSprintId(null);
                item.setStatus(PbiStatus.READY);
            }
        }
        productBacklogItemRepository.saveAll(pbis);
    }

    private List<BurndownPointResponse> buildBurndown(Sprint sprint, List<Task> tasks) {
        List<Task> activeTasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .toList();

        LocalDate end = LocalDate.now().isBefore(sprint.getEndDate())
                ? LocalDate.now()
                : sprint.getEndDate();

        List<BurndownPointResponse> points = new ArrayList<>();
        for (LocalDate date = sprint.getStartDate(); !date.isAfter(end); date = date.plusDays(1)) {
            long completed = countCompletedOnOrBefore(activeTasks, date);
            points.add(BurndownPointResponse.builder()
                    .date(date)
                    .completedTasks(completed)
                    .remainingTasks(activeTasks.size() - completed)
                    .build());
        }
        return points;
    }

    private long countCompletedOnOrBefore(List<Task> tasks, LocalDate date) {
        return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .filter(task -> task.getCompletedAt() == null
                        || !task.getCompletedAt().toLocalDate().isAfter(date))
                .count();
    }

    private long countByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream().filter(task -> task.getStatus() == status).count();
    }

    private SprintResponse toResponse(Sprint sprint) {
        long pbiCount = productBacklogItemRepository.countBySprintId(sprint.getId());
        long taskCount = taskRepository.countBySprintId(sprint.getId());
        return sprintMapper.toResponse(sprint, pbiCount, taskCount);
    }

    private void verifyProjectManager(Project project) {
        if (!projectService.isActiveProjectManager(project)) {
            throw new BusinessException(
                    ErrorCode.PROJECT_ACCESS_DENIED,
                    "Chỉ Project Manager mới được thực hiện thao tác này"
            );
        }
    }

    private void verifyCanViewProgress(Project project) {
        if (projectService.isActiveProjectManager(project)) {
            return;
        }
        if (teamService.isActiveTeamLeader(project.getTeam())) {
            return;
        }
        throw new BusinessException(
                ErrorCode.SPRINT_ACCESS_DENIED,
                "Chỉ Project Manager hoặc Team Leader mới được xem tiến độ sprint"
        );
    }

    private void ensureProjectAcceptsSprintChanges(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void logSprintEvent(Project project, String action, Sprint sprint) {
        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                action,
                ActivityLogAction.TARGET_SPRINT,
                sprint.getId(),
                sprint.getName(),
                project
        );
    }

}
