package com.workmanagement.backend.team.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.team.dto.request.CreateTeamRequest;
import com.workmanagement.backend.team.dto.request.UpdateTeamRequest;
import com.workmanagement.backend.team.dto.response.TeamResponse;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.mapper.TeamMapper;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.service.WorkspaceService;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private static final String TEAM_LEADER_ROLE = "Team Leader";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMapper teamMapper;
    private final WorkspaceService workspaceService;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    /** UC-2.3 — Tạo nhóm làm việc trong workspace */
    @Transactional
    @PreAuthorize("hasAuthority('team:create')")
    public TeamResponse create(Long workspaceId, CreateTeamRequest request) {
        Workspace workspace = workspaceService.getWorkspace(workspaceId);
        workspaceService.verifyCanManage(workspace);
        ensureWorkspaceActive(workspace);

        Team team = Team.builder()
                .workspace(workspace)
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(CommonStatus.ACTIVE)
                .build();

        team = teamRepository.save(team);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_CREATED,
                ActivityLogAction.TARGET_TEAM,
                team.getId(),
                team.getName()
        );

        return toResponseWithLeader(team);
    }

    /** UC-2.4 — Danh sách nhóm trong workspace */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('team:read')")
    public PageResponse<TeamResponse> findAll(
            Long workspaceId,
            int page,
            int size,
            String keyword,
            CommonStatus status
    ) {
        Workspace workspace = workspaceService.getWorkspace(workspaceId);
        workspaceService.verifyAccess(workspace);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Team> spec = buildSearchSpec(workspaceId, keyword, status);
        Page<Team> result = teamRepository.findAll(spec, pageable);

        return PageResponse.<TeamResponse>builder()
                .items(result.getContent().stream().map(this::toResponseWithLeader).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** UC-2.4 — Chi tiết nhóm */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('team:read')")
    public TeamResponse findById(Long workspaceId, Long teamId) {
        Team team = getTeam(workspaceId, teamId);
        workspaceService.verifyAccess(team.getWorkspace());
        return toResponseWithLeader(team);
    }

    /** UC-2.4 — Cập nhật thông tin nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamResponse update(Long workspaceId, Long teamId, UpdateTeamRequest request) {
        Team team = getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());
        ensureTeamActive(team);

        if (StringUtils.hasText(request.getName())) {
            team.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }

        team = teamRepository.save(team);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_UPDATED,
                ActivityLogAction.TARGET_TEAM,
                team.getId(),
                team.getName()
        );

        return toResponseWithLeader(team);
    }

    /** UC-2.5 — Giải thể nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:delete')")
    public TeamResponse disband(Long workspaceId, Long teamId) {
        Team team = getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());

        if (team.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhóm đã được giải thể");
        }

        team.setStatus(CommonStatus.INACTIVE);
        team = teamRepository.save(team);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_DISBANDED,
                ActivityLogAction.TARGET_TEAM,
                team.getId(),
                team.getName()
        );

        return toResponseWithLeader(team);
    }

    public Team getTeam(Long workspaceId, Long teamId) {
        return teamRepository.findByIdAndWorkspaceId(teamId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "Không tìm thấy nhóm làm việc"));
    }

    void ensureTeamActive(Team team) {
        if (team.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhóm đã giải thể, không thể thao tác");
        }
    }

    /** Team Leader hoặc workspace owner được quản lý dự án (UC-2.8) */
    public void verifyCanManageProject(Team team) {
        try {
            workspaceService.verifyCanManage(team.getWorkspace());
        } catch (BusinessException ex) {
            if (!isActiveTeamLeader(team)) {
                throw new BusinessException(
                        ErrorCode.TEAM_ACCESS_DENIED,
                        "Chỉ Team Leader hoặc workspace owner mới được thao tác"
                );
            }
        }
    }

    public void verifyTeamAccess(Team team) {
        workspaceService.verifyAccess(team.getWorkspace());
    }

    private boolean isActiveTeamLeader(Team team) {
        return teamMemberRepository.existsByTeamIdAndWorkspaceMember_User_IdAndRole_NameAndStatus(
                team.getId(),
                SecurityUtils.getCurrentUserId(),
                TEAM_LEADER_ROLE,
                MemberStatus.ACTIVE
        );
    }

    private void ensureWorkspaceActive(Workspace workspace) {
        if (workspace.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã đóng");
        }
    }

    private TeamResponse toResponseWithLeader(Team team) {
        TeamMember leader = findTeamLeader(team.getId()).orElse(null);
        return teamMapper.toResponse(team, leader);
    }

    private java.util.Optional<TeamMember> findTeamLeader(Long teamId) {
        return roleRepository.findByNameAndScope(TEAM_LEADER_ROLE, com.workmanagement.backend.common.enums.RoleScope.TEAM)
                .flatMap(role -> teamMemberRepository
                        .findByTeamIdAndRole_IdAndStatus(teamId, role.getId(), MemberStatus.ACTIVE)
                        .stream()
                        .findFirst());
    }

    private Specification<Team> buildSearchSpec(Long workspaceId, String keyword, CommonStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("workspace").get("id"), workspaceId));

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
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
