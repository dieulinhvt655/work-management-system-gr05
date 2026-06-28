package com.workmanagement.backend.project.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.RoleScope;
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
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.service.TeamService;
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

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String PROJECT_MANAGER_ROLE = "Project Manager";
    private static final String PROJECT_CONTRIBUTOR_ROLE = "Project Contributor";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    /** UC-3.1 — Tạo dự án mới */
    @Transactional
    @PreAuthorize("hasAuthority('project:create')")
    public ProjectResponse create(Long workspaceId, Long teamId, CreateProjectRequest request) {
        Team team = teamService.getTeam(workspaceId, teamId);
        teamService.verifyCanManageProject(team);
        ensureTeamActive(team);

        if (projectRepository.existsByCode(request.getCode().trim())) {
            throw new BusinessException(ErrorCode.PROJECT_CODE_ALREADY_EXISTS, "Mã dự án đã tồn tại");
        }

        TeamMember projectManager = resolveActiveTeamMember(teamId, request.getProjectManagerMemberId());
        validateDateRange(request.getStartDate(), request.getEndDate());

        Project project = Project.builder()
                .team(team)
                .projectManagerMember(projectManager)
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .description(request.getDescription())
                .objective(request.getObjective())
                .scope(request.getScope())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ProjectStatus.DRAFT)
                .build();
        project = projectRepository.save(project);

        Role pmRole = findProjectRole(PROJECT_MANAGER_ROLE);
        projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .teamMember(projectManager)
                .role(pmRole)
                .status(MemberStatus.ACTIVE)
                .build());

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
        Team team = teamService.getTeam(workspaceId, teamId);
        teamService.verifyTeamAccess(team);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Project> spec = buildSearchSpec(teamId, keyword, status);
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
        Project project = getProject(workspaceId, teamId, projectId);
        verifyCanUpdateProject(project);
        ensureProjectEditable(project);

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : project.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : project.getEndDate();
        validateDateRange(startDate, endDate);

        if (StringUtils.hasText(request.getName())) {
            project.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getObjective() != null) {
            project.setObjective(request.getObjective());
        }
        if (request.getScope() != null) {
            project.setScope(request.getScope());
        }
        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
        }

        if (request.getProjectManagerMemberId() != null
                && !request.getProjectManagerMemberId().equals(project.getProjectManagerMember().getId())) {
            reassignProjectManager(project, teamId, request.getProjectManagerMemberId());
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
        Project project = getProject(workspaceId, teamId, projectId);
        teamService.verifyCanManageProject(project.getTeam());
        ensureProjectEditable(project);

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ dự án nháp mới được kích hoạt");
        }

        project.setStatus(ProjectStatus.ACTIVE);
        project = projectRepository.save(project);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_ACTIVATED,
                ActivityLogAction.TARGET_PROJECT,
                project.getId(),
                project.getStatus().getValue(),
                project
        );

        return projectMapper.toResponse(project);
    }

    /** UC-3.11 — Kết thúc dự án (ACTIVE → COMPLETED) */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public ProjectResponse complete(Long workspaceId, Long teamId, Long projectId) {
        Project project = getProject(workspaceId, teamId, projectId);
        verifyIsProjectManager(project);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ dự án đang hoạt động mới được kết thúc");
        }

        project.setStatus(ProjectStatus.COMPLETED);
        project = projectRepository.save(project);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_COMPLETED,
                ActivityLogAction.TARGET_PROJECT,
                project.getId(),
                project.getStatus().getValue(),
                project
        );

        return projectMapper.toResponse(project);
    }

    /** UC-3.12 — Lưu trữ dự án (COMPLETED → ARCHIVED) */
    @Transactional
    @PreAuthorize("hasAuthority('project:update')")
    public ProjectResponse archive(Long workspaceId, Long teamId, Long projectId) {
        Project project = getProject(workspaceId, teamId, projectId);
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
                project.getStatus().getValue(),
                project
        );

        return projectMapper.toResponse(project);
    }

    public Project getProject(Long workspaceId, Long teamId, Long projectId) {
        Team team = teamService.getTeam(workspaceId, teamId);
        return projectRepository.findByIdAndTeamId(projectId, team.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "Không tìm thấy dự án"));
    }

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

    private boolean isActiveProjectManager(Project project) {
        TeamMember pm = project.getProjectManagerMember();
        return pm.getStatus() == MemberStatus.ACTIVE
                && pm.getWorkspaceMember().getUser().getId().equals(SecurityUtils.getCurrentUserId());
    }

    private void reassignProjectManager(Project project, Long teamId, Long newPmTeamMemberId) {
        TeamMember newPm = resolveActiveTeamMember(teamId, newPmTeamMemberId);
        TeamMember oldPm = project.getProjectManagerMember();
        Role pmRole = findProjectRole(PROJECT_MANAGER_ROLE);
        Role contributorRole = findProjectRole(PROJECT_CONTRIBUTOR_ROLE);

        projectMemberRepository.findByProjectIdAndTeamMemberId(project.getId(), oldPm.getId())
                .ifPresent(oldPmMember -> {
                    oldPmMember.setRole(contributorRole);
                    projectMemberRepository.save(oldPmMember);
                });

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

    private Role findProjectRole(String name) {
        return roleRepository.findByNameAndScope(name, RoleScope.PROJECT)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò dự án"));
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private Specification<Project> buildSearchSpec(Long teamId, String keyword, ProjectStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("team").get("id"), teamId));

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
