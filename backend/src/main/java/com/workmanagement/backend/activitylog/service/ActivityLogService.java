package com.workmanagement.backend.activitylog.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.dto.response.ActivityLogResponse;
import com.workmanagement.backend.activitylog.entity.ActivityLog;
import com.workmanagement.backend.activitylog.mapper.ActivityLogMapper;
import com.workmanagement.backend.activitylog.repository.ActivityLogRepository;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.repository.ProjectRepository;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.service.WorkspaceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogMapper activityLogMapper;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;

    @Lazy
    @Autowired
    private WorkspaceService workspaceService;

    /** UC-2.9 — Tra cứu hoạt động tổ chức (workspace) */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('workspace:read') and hasAuthority('workspace:activity-read')")
    public PageResponse<ActivityLogResponse> findByWorkspace(
            Long workspaceId,
            int page,
            int size,
            String action,
            String targetType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        workspaceService.verifyAccess(workspaceService.getWorkspace(workspaceId));

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Long> teamIds = teamRepository.findByWorkspaceId(workspaceId).stream()
                .map(team -> team.getId())
                .toList();
        List<Long> workspaceMemberIds = workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                .map(member -> member.getId())
                .toList();
        List<Long> teamMemberIds = teamMemberRepository.findByTeam_Workspace_Id(workspaceId).stream()
                .map(member -> member.getId())
                .toList();

        Specification<ActivityLog> spec = workspaceActivitySpec(
                workspaceId, teamIds, workspaceMemberIds, teamMemberIds, action, targetType, from, to
        );
        Page<ActivityLog> result = activityLogRepository.findAll(spec, pageable);

        return PageResponse.<ActivityLogResponse>builder()
                .items(result.getContent().stream().map(activityLogMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** UC-3.10 — Lịch sử hoạt động dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public PageResponse<ActivityLogResponse> findByProject(
            Long workspaceId,
            Long teamId,
            Long projectId,
            int page,
            int size,
            String action,
            String targetType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Project project = projectRepository.findByIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "Không tìm thấy dự án"));
        if (!project.getTeam().getWorkspace().getId().equals(workspaceId)) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "Không tìm thấy dự án");
        }
        workspaceService.verifyAccess(workspaceService.getWorkspace(workspaceId));

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<ActivityLog> spec = projectActivitySpec(projectId, action, targetType, from, to);
        Page<ActivityLog> result = activityLogRepository.findAll(spec, pageable);

        return PageResponse.<ActivityLogResponse>builder()
                .items(result.getContent().stream().map(activityLogMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** Hỗ trợ UC-2.9, UC-3.10 — Ghi nhận hoạt động vào nhật ký */
    @Transactional
    public void record(
            Long actorUserId,
            String action,
            String targetType,
            Long targetId,
            String oldValue,
            String newValue,
            Project project
    ) {
        User actor = userRepository.findById(actorUserId).orElse(null);
        if (actor == null) {
            return;
        }

        activityLogRepository.save(ActivityLog.builder()
                .actor(actor)
                .project(project)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    /** Hỗ trợ UC-2.9 — Ghi nhật ký hoạt động tổ chức (workspace/team) */
    @Transactional
    public void recordOrgEvent(Long actorUserId, String action, String targetType, Long targetId, String newValue) {
        record(actorUserId, action, targetType, targetId, null, newValue, null);
    }

    /** Hỗ trợ UC-3.10 — Ghi nhật ký hoạt động dự án */
    @Transactional
    public void recordProjectEvent(
            Long actorUserId,
            String action,
            String targetType,
            Long targetId,
            String newValue,
            Project project
    ) {
        record(actorUserId, action, targetType, targetId, null, newValue, project);
    }

    private Specification<ActivityLog> workspaceActivitySpec(
            Long workspaceId,
            List<Long> teamIds,
            List<Long> workspaceMemberIds,
            List<Long> teamMemberIds,
            String action,
            String targetType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            List<Predicate> scopePredicates = new ArrayList<>();
            scopePredicates.add(cb.and(
                    cb.equal(root.get("targetType"), ActivityLogAction.TARGET_WORKSPACE),
                    cb.equal(root.get("targetId"), workspaceId)
            ));

            if (!teamIds.isEmpty()) {
                scopePredicates.add(cb.and(
                        cb.equal(root.get("targetType"), ActivityLogAction.TARGET_TEAM),
                        root.get("targetId").in(teamIds)
                ));
            }

            if (!workspaceMemberIds.isEmpty()) {
                scopePredicates.add(cb.and(
                        cb.equal(root.get("targetType"), ActivityLogAction.TARGET_WORKSPACE_MEMBER),
                        root.get("targetId").in(workspaceMemberIds)
                ));
            }

            if (!teamMemberIds.isEmpty()) {
                scopePredicates.add(cb.and(
                        cb.equal(root.get("targetType"), ActivityLogAction.TARGET_TEAM_MEMBER),
                        root.get("targetId").in(teamMemberIds)
                ));
            }

            scopePredicates.add(cb.and(
                    cb.isNotNull(root.get("project")),
                    cb.equal(root.get("project").get("team").get("workspace").get("id"), workspaceId)
            ));

            predicates.add(cb.or(scopePredicates.toArray(Predicate[]::new)));

            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (StringUtils.hasText(targetType)) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<ActivityLog> projectActivitySpec(
            Long projectId,
            String action,
            String targetType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (StringUtils.hasText(targetType)) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
