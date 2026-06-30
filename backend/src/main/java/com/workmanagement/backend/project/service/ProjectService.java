package com.workmanagement.backend.project.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
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
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogRepository;
import com.workmanagement.backend.notification.service.NotificationService;
import com.workmanagement.backend.project.mapper.ProjectMapper;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.repository.ProjectRepository;
import com.workmanagement.backend.project.util.ProjectCodeGenerator;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.service.TeamService;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.entity.WorkflowState;
import com.workmanagement.backend.task.entity.WorkflowTransition;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.task.repository.WorkflowStateRepository;
import com.workmanagement.backend.task.repository.WorkflowTransitionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String TEAM_LEADER_ROLE = "Team Leader";
    private static final String PROJECT_MANAGER_ROLE = "Project Manager";
    private static final String PROJECT_CONTRIBUTOR_ROLE = "Project Contributor";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;
    private final ProjectCodeGenerator projectCodeGenerator;
    private final NotificationService notificationService;
    private final TaskRepository taskRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final ProductBacklogRepository productBacklogRepository;

    /** UC-3.1 — Tạo dự án mới */
    @Transactional
    @PreAuthorize("hasAuthority('project:create')")
    public ProjectResponse create(Long workspaceId, Long teamId, CreateProjectRequest request) {
        validateCreateRequest(request);
        Team team = teamService.getTeam(workspaceId, teamId);
        teamService.verifyCanManageProject(team);
        ensureTeamActive(team);

        String projectName = request.getName().trim();
        if (projectRepository.existsByTeamIdAndNameIgnoreCase(teamId, projectName)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên dự án đã tồn tại trong nhóm");
        }

        validateDateRange(request.getStartDate(), request.getEndDate());
        String projectCode = projectCodeGenerator.generateUnique();

        Project project = Project.builder()
                .team(team)
                .code(projectCode)
                .name(projectName)
                .description(request.getDescription())
                .objective(request.getObjective())
                .scope(request.getScope())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ProjectStatus.DRAFT)
                .build();
        project = projectRepository.save(project);

        if (request.getProjectManagerMemberId() != null) {
            TeamMember projectManager = resolveActiveTeamMember(teamId, request.getProjectManagerMemberId());
            Role pmRole = findProjectRole(PROJECT_MANAGER_ROLE);
            project.setProjectManagerMember(projectManager);
            projectRepository.save(project);

            projectMemberRepository.save(ProjectMember.builder()
                    .project(project)
                    .teamMember(projectManager)
                    .role(pmRole)
                    .status(MemberStatus.ACTIVE)
                    .build());
        }

        ensureDefaultWorkflow(project);
        ensureDefaultBacklog(project);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_CREATED,
                ActivityLogAction.TARGET_PROJECT,
                project.getId(),
                project.getName(),
                project
        );

        return projectMapper.toResponse(project);
    }

    /** UC-3.7 — Danh sách dự án trong nhóm */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public PageResponse<ProjectResponse> findAll(
            Long workspaceId,
            Long teamId,
            int page,
            int size,
            String keyword,
            ProjectStatus status
    ) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }
        Team team = teamService.getTeam(workspaceId, teamId);
        teamService.verifyTeamAccess(team);

        List<Long> accessibleProjectIds = resolveAccessibleProjectIds(team, teamId);
        if (accessibleProjectIds != null && accessibleProjectIds.isEmpty()) {
            return PageResponse.<ProjectResponse>builder()
                    .items(List.of())
                    .page(0)
                    .size(Math.min(Math.max(size, 1), 100))
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Project> spec = buildSearchSpec(teamId, keyword, status, accessibleProjectIds);
        Page<Project> result = projectRepository.findAll(spec, pageable);

        return PageResponse.<ProjectResponse>builder()
                .items(result.getContent().stream().map(projectMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** UC-3.2 — Chi tiết dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public ProjectResponse findById(Long workspaceId, Long teamId, Long projectId) {
        Project project = getProject(workspaceId, teamId, projectId);
        verifyProjectAccess(project);
        return projectMapper.toResponse(project);
    }

    /** UC-3.3 — Cập nhật thông tin dự án */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public ProjectResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            UpdateProjectRequest request
    ) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        Project project = getProject(workspaceId, teamId, projectId);
        verifyCanUpdateProject(project);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã lưu trữ, không thể cập nhật");
        }
        ensureProjectEditable(project);

        boolean changed = false;
        Team team = project.getTeam();
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : project.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : project.getEndDate();
        validateDateRange(startDate, endDate);

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (newName.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên dự án không được để trống");
            }
            if (!newName.equalsIgnoreCase(project.getName())
                    && projectRepository.existsByTeamIdAndNameIgnoreCase(team.getId(), newName)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên dự án đã tồn tại trong nhóm");
            }
            if (!newName.equals(project.getName())) {
                project.setName(newName);
                changed = true;
            }
        }
        if (request.getDescription() != null) {
            if (!request.getDescription().equals(project.getDescription())) {
                project.setDescription(request.getDescription());
                changed = true;
            }
        }
        if (request.getObjective() != null) {
            if (!request.getObjective().equals(project.getObjective())) {
                project.setObjective(request.getObjective());
                changed = true;
            }
        }
        if (request.getScope() != null) {
            if (!request.getScope().equals(project.getScope())) {
                project.setScope(request.getScope());
                changed = true;
            }
        }
        if (request.getStartDate() != null) {
            if (!request.getStartDate().equals(project.getStartDate())) {
                project.setStartDate(request.getStartDate());
                changed = true;
            }
        }
        if (request.getEndDate() != null) {
            if (!request.getEndDate().equals(project.getEndDate())) {
                project.setEndDate(request.getEndDate());
                changed = true;
            }
        }

        if (request.getProjectManagerMemberId() != null) {
            Long currentPmId = project.getProjectManagerMember() != null
                    ? project.getProjectManagerMember().getId()
                    : null;
            if (!request.getProjectManagerMemberId().equals(currentPmId)) {
                ensureCanReassignProjectManager(team);
                reassignProjectManager(project, teamId, request.getProjectManagerMemberId());
                changed = true;
            }
        }

        if (!changed) {
            return projectMapper.toResponse(project);
        }

        project = projectRepository.save(project);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_UPDATED,
                ActivityLogAction.TARGET_PROJECT,
                project.getId(),
                project.getName(),
                project
        );

        return projectMapper.toResponse(project);
    }

    /** UC-3.5 — Kích hoạt dự án (DRAFT → ACTIVE) */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public ProjectResponse activate(Long workspaceId, Long teamId, Long projectId) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");

        Project project = getProject(workspaceId, teamId, projectId);
        teamService.verifyCanManageProject(project.getTeam());
        ensureProjectEditable(project);

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ dự án nháp mới được kích hoạt");
        }
        if (project.getProjectManagerMember() == null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Cần gán Project Manager trước khi kích hoạt dự án"
            );
        }
        if (project.getProjectManagerMember().getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Project Manager phải đang hoạt động trước khi kích hoạt dự án"
            );
        }

        project.setStatus(ProjectStatus.ACTIVE);
        project = projectRepository.save(project);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_ACTIVATED,
                ActivityLogAction.TARGET_PROJECT,
                project.getId(),
                project.getName(),
                project
        );

        notificationService.notifyProjectActivated(project, collectProjectActivationRecipientUserIds(project));

        return projectMapper.toResponse(project);
    }

    /** UC-3.11 — Kết thúc dự án (ACTIVE → COMPLETED) */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public ProjectResponse complete(Long workspaceId, Long teamId, Long projectId) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");

        Project project = getProject(workspaceId, teamId, projectId);
        verifyIsProjectManager(project);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ dự án đang hoạt động mới được kết thúc");
        }
        if (hasUnfinishedTasks(project)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Dự án còn task chưa hoàn thành, không thể kết thúc"
            );
        }

        project.setStatus(ProjectStatus.COMPLETED);
        project = projectRepository.save(project);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_COMPLETED,
                ActivityLogAction.TARGET_PROJECT,
                project.getId(),
                project.getName(),
                project
        );

        notificationService.notifyProjectCompleted(project, collectProjectCompletionRecipientUserIds(project));

        return projectMapper.toResponse(project);
    }

    /** UC-3.12 — Lưu trữ dự án (COMPLETED → ARCHIVED) */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public ProjectResponse archive(Long workspaceId, Long teamId, Long projectId) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");

        Project project = getProject(workspaceId, teamId, projectId);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã được lưu trữ");
        }
        teamService.verifyCanManageProject(project.getTeam());

        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ dự án đã kết thúc mới được lưu trữ");
        }

        project.setStatus(ProjectStatus.ARCHIVED);
        project = projectRepository.save(project);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_ARCHIVED,
                ActivityLogAction.TARGET_PROJECT,
                project.getId(),
                project.getName(),
                project
        );

        return projectMapper.toResponse(project);
    }

    /** Hỗ trợ UC-3.x — Tra cứu dự án theo workspace/team */
    public Project getProject(Long workspaceId, Long teamId, Long projectId) {
        Team team = teamService.getTeam(workspaceId, teamId);
        return projectRepository.findByIdAndTeamId(projectId, team.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "Không tìm thấy dự án"));
    }

    /** Hỗ trợ UC-3.2 — Kiểm tra quyền xem dự án */
    public void verifyProjectAccess(Project project) {
        teamService.verifyTeamAccess(project.getTeam());
    }

    /** Team Leader hoặc Project Manager được cập nhật dự án */
    public void verifyCanUpdateProject(Project project) {
        try {
            teamService.verifyCanManageProject(project.getTeam());
        } catch (BusinessException ex) {
            if (!isActiveProjectManager(project)) {
                throw new BusinessException(
                        ErrorCode.PROJECT_ACCESS_DENIED,
                        "Chỉ Team Leader hoặc Project Manager mới được cập nhật dự án"
                );
            }
        }
    }

    private void verifyIsProjectManager(Project project) {
        if (!isActiveProjectManager(project)) {
            throw new BusinessException(
                    ErrorCode.PROJECT_ACCESS_DENIED,
                    "Chỉ Project Manager mới được thực hiện thao tác này"
            );
        }
    }

    /** Hỗ trợ UC-3.3 — Kiểm tra user hiện tại có phải Project Manager đang hoạt động */
    public boolean isActiveProjectManager(Project project) {
        TeamMember pm = project.getProjectManagerMember();
        return pm != null
                && pm.getStatus() == MemberStatus.ACTIVE
                && pm.getWorkspaceMember() != null
                && pm.getWorkspaceMember().getUser() != null
                && pm.getWorkspaceMember().getUser().getId().equals(SecurityUtils.getCurrentUserId());
    }

    private void reassignProjectManager(Project project, Long teamId, Long newPmTeamMemberId) {
        TeamMember newPm = resolveActiveTeamMember(teamId, newPmTeamMemberId);
        TeamMember oldPm = project.getProjectManagerMember();
        Role pmRole = findProjectRole(PROJECT_MANAGER_ROLE);
        Role contributorRole = findProjectRole(PROJECT_CONTRIBUTOR_ROLE);

        if (oldPm != null) {
            projectMemberRepository.findByProjectIdAndTeamMemberId(project.getId(), oldPm.getId())
                    .ifPresent(oldPmMember -> {
                        oldPmMember.setRole(contributorRole);
                        projectMemberRepository.save(oldPmMember);
                    });
        }

        ProjectMember newPmMember = projectMemberRepository
                .findByProjectIdAndTeamMemberId(project.getId(), newPm.getId())
                .orElseGet(() -> ProjectMember.builder()
                        .project(project)
                        .teamMember(newPm)
                        .status(MemberStatus.ACTIVE)
                        .build());
        newPmMember.setRole(pmRole);
        newPmMember.setStatus(MemberStatus.ACTIVE);
        newPmMember.setRemovedAt(null);
        projectMemberRepository.save(newPmMember);

        project.setProjectManagerMember(newPm);
    }

    private void ensureCanReassignProjectManager(Team team) {
        teamService.verifyCanManageProject(team);
    }

    private void ensureDefaultWorkflow(Project project) {
        if (workflowStateRepository.existsByProjectId(project.getId())) {
            return;
        }

        WorkflowState toDo = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("To Do")
                .code("to_do")
                .position(0)
                .isDefault(true)
                .isFinal(false)
                .build());
        WorkflowState inProgress = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("In Progress")
                .code("in_progress")
                .position(1)
                .isDefault(false)
                .isFinal(false)
                .build());
        WorkflowState review = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("Review")
                .code("review")
                .position(2)
                .isDefault(false)
                .isFinal(false)
                .build());
        WorkflowState done = workflowStateRepository.save(WorkflowState.builder()
                .project(project)
                .name("Done")
                .code("done")
                .position(3)
                .isDefault(false)
                .isFinal(true)
                .build());

        workflowTransitionRepository.saveAll(List.of(
                buildTransition(project, toDo, inProgress, "Bắt đầu"),
                buildTransition(project, inProgress, review, "Gửi duyệt"),
                buildTransition(project, review, done, "Phê duyệt"),
                buildTransition(project, review, inProgress, "Trả lại")
        ));
    }

    private void ensureDefaultBacklog(Project project) {
        if (productBacklogRepository.findByProjectId(project.getId()).isPresent()) {
            return;
        }
        productBacklogRepository.save(ProductBacklog.builder()
                .project(project)
                .name(project.getName() + " Backlog")
                .description("Product backlog của dự án " + project.getName())
                .build());
    }

    private WorkflowTransition buildTransition(
            Project project,
            WorkflowState from,
            WorkflowState to,
            String name
    ) {
        return WorkflowTransition.builder()
                .project(project)
                .fromState(from)
                .toState(to)
                .name(name)
                .build();
    }

    private TeamMember resolveActiveTeamMember(Long teamId, Long teamMemberId) {
        return teamMemberRepository.findByIdAndTeamId(teamMemberId, teamId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TEAM_MEMBER_NOT_FOUND,
                        "Thành viên phải thuộc nhóm và đang active"
                ));
    }

    private void ensureTeamActive(Team team) {
        if (team.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhóm đã giải thể");
        }
    }

    private void ensureProjectEditable(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
    }

    private List<Long> collectProjectActivationRecipientUserIds(Project project) {
        LinkedHashSet<Long> recipientUserIds = new LinkedHashSet<>();

        TeamMember projectManager = project.getProjectManagerMember();
        if (projectManager != null
                && projectManager.getWorkspaceMember() != null
                && projectManager.getWorkspaceMember().getUser() != null
                && projectManager.getWorkspaceMember().getUser().getId() != null) {
            recipientUserIds.add(projectManager.getWorkspaceMember().getUser().getId());
        }

        List<ProjectMember> activeMembers = Optional.ofNullable(
                projectMemberRepository.findByProjectIdAndStatus(project.getId(), MemberStatus.ACTIVE)
        ).orElse(List.of());
        for (ProjectMember member : activeMembers) {
            if (member == null || member.getTeamMember() == null) {
                continue;
            }
            TeamMember teamMember = member.getTeamMember();
            if (teamMember.getWorkspaceMember() == null
                    || teamMember.getWorkspaceMember().getUser() == null
                    || teamMember.getWorkspaceMember().getUser().getId() == null) {
                continue;
            }
            recipientUserIds.add(teamMember.getWorkspaceMember().getUser().getId());
        }

        return new ArrayList<>(recipientUserIds);
    }

    private List<Long> collectProjectCompletionRecipientUserIds(Project project) {
        LinkedHashSet<Long> recipientUserIds = new LinkedHashSet<>();

        TeamMember projectManager = project.getProjectManagerMember();
        if (projectManager != null
                && projectManager.getWorkspaceMember() != null
                && projectManager.getWorkspaceMember().getUser() != null
                && projectManager.getWorkspaceMember().getUser().getId() != null) {
            recipientUserIds.add(projectManager.getWorkspaceMember().getUser().getId());
        }

        Team team = project.getTeam();
        if (team != null && team.getId() != null) {
            List<TeamMember> leaders = Optional.ofNullable(teamMemberRepository.findByTeamIdAndRole_IdAndStatus(
                    team.getId(),
                    findTeamLeaderRole().getId(),
                    MemberStatus.ACTIVE
            )).orElse(List.of());
            for (TeamMember leader : leaders) {
                addRecipientUserId(recipientUserIds, leader);
            }
        }

        List<ProjectMember> activeMembers = Optional.ofNullable(
                projectMemberRepository.findByProjectIdAndStatus(project.getId(), MemberStatus.ACTIVE)
        ).orElse(List.of());
        for (ProjectMember member : activeMembers) {
            if (member == null) {
                continue;
            }
            addRecipientUserId(recipientUserIds, member.getTeamMember());
        }

        return new ArrayList<>(recipientUserIds);
    }

    private void addRecipientUserId(LinkedHashSet<Long> recipientUserIds, TeamMember teamMember) {
        if (teamMember == null
                || teamMember.getWorkspaceMember() == null
                || teamMember.getWorkspaceMember().getUser() == null
                || teamMember.getWorkspaceMember().getUser().getId() == null) {
            return;
        }
        recipientUserIds.add(teamMember.getWorkspaceMember().getUser().getId());
    }

    private boolean hasUnfinishedTasks(Project project) {
        List<Task> tasks = taskRepository.findByProjectId(project.getId());
        return tasks.stream().anyMatch(this::isUnfinishedTask);
    }

    private boolean isUnfinishedTask(Task task) {
        if (task == null) {
            return false;
        }
        TaskStatus status = task.getStatus();
        return status != TaskStatus.DONE && status != TaskStatus.CANCELLED;
    }

    private Role findProjectRole(String name) {
        return roleRepository.findByNameAndScope(name, RoleScope.PROJECT)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò dự án"));
    }

    private Role findTeamLeaderRole() {
        return roleRepository.findByNameAndScope(TEAM_LEADER_ROLE, RoleScope.TEAM)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò team leader"));
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void validateCreateRequest(CreateProjectRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên dự án không được để trống");
        }
        if (!StringUtils.hasText(request.getObjective())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mục tiêu không được để trống");
        }
        if (!StringUtils.hasText(request.getScope())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Phạm vi không được để trống");
        }
        if (request.getStartDate() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "startDate không được để trống");
        }
        if (request.getDescription() != null && request.getDescription().length() > 2000) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mô tả tối đa 2000 ký tự");
        }
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " không hợp lệ");
        }
    }

    private List<Long> resolveAccessibleProjectIds(Team team, Long teamId) {
        try {
            teamService.verifyCanManageProject(team);
            return null;
        } catch (BusinessException ex) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            List<Long> managedIds = projectMemberRepository.findManagedProjectIdsByTeamAndUser(
                    teamId,
                    currentUserId,
                    MemberStatus.ACTIVE
            );
            if (!managedIds.isEmpty()) {
                return managedIds;
            }
            return projectMemberRepository.findParticipatingProjectIdsByTeamAndUser(
                    teamId,
                    currentUserId,
                    MemberStatus.ACTIVE
            );
        }
    }

    private Specification<Project> buildSearchSpec(
            Long teamId,
            String keyword,
            ProjectStatus status,
            List<Long> accessibleProjectIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("team").get("id"), teamId));

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), ProjectStatus.ARCHIVED));
            }

            if (accessibleProjectIds != null) {
                if (accessibleProjectIds.isEmpty()) {
                    return cb.disjunction();
                }
                predicates.add(root.get("id").in(accessibleProjectIds));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
