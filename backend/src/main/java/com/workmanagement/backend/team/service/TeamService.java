package com.workmanagement.backend.team.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.repository.ProjectRepository;
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
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    /** Trạng thái dự án được coi là "chưa hoàn tất"; COMPLETED/ARCHIVED không chặn giải thể nhóm. */
    private static final List<ProjectStatus> UNFINISHED_PROJECT_STATUSES =
            List.of(ProjectStatus.DRAFT, ProjectStatus.ACTIVE);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMapper teamMapper;
    private final WorkspaceService workspaceService;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;
    private final ProjectRepository projectRepository;

    /** UC-2.3 — Tạo nhóm làm việc trong workspace */
    @Transactional
    @PreAuthorize("hasAuthority('team:create')")
    public TeamResponse create(Long workspaceId, CreateTeamRequest request) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên nhóm không được để trống");
        }

        String teamName = request.getName().trim();
        if (teamName.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên nhóm tối đa 255 ký tự");
        }
        if (request.getDescription() != null && request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mô tả tối đa 2000 ký tự");
        }

        Workspace workspace = workspaceService.getWorkspace(workspaceId);
        workspaceService.verifyCanManage(workspace);
        ensureWorkspaceActive(workspace);

        if (teamRepository.existsByWorkspaceIdAndNameIgnoreCase(workspace.getId(), teamName)) {
            throw new BusinessException(ErrorCode.TEAM_NAME_ALREADY_EXISTS, "Tên nhóm đã tồn tại trong workspace");
        }

        Team team = Team.builder()
                .workspace(workspace)
                .name(teamName)
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
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }

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
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }

        Team team = getTeam(workspaceId, teamId);
        workspaceService.verifyAccess(team.getWorkspace());
        return toResponseWithLeader(team);
    }

    /** UC-2.4 — Cập nhật thông tin nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamResponse update(Long workspaceId, Long teamId, UpdateTeamRequest request) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }

        Team team = getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());
        ensureTeamActive(team);

        boolean changed = false;

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (newName.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên nhóm không được để trống");
            }
            if (newName.length() > MAX_NAME_LENGTH) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên nhóm tối đa 255 ký tự");
            }
            if (!newName.equalsIgnoreCase(team.getName())
                    && teamRepository.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, newName)) {
                throw new BusinessException(ErrorCode.TEAM_NAME_ALREADY_EXISTS, "Tên nhóm đã tồn tại trong workspace");
            }
            if (!newName.equals(team.getName())) {
                team.setName(newName);
                changed = true;
            }
        }

        if (request.getDescription() != null) {
            if (request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mô tả tối đa 2000 ký tự");
            }
            if (!request.getDescription().equals(team.getDescription())) {
                team.setDescription(request.getDescription());
                changed = true;
            }
        }

        if (!changed) {
            return toResponseWithLeader(team);
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
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }

        Team team = getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());

        if (team.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhóm đã được giải thể");
        }

        if (teamMemberRepository.existsByTeamIdAndStatus(team.getId(), MemberStatus.ACTIVE)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Nhóm vẫn còn thành viên đang hoạt động, không thể giải thể"
            );
        }

        if (projectRepository.existsByTeamIdAndStatusIn(team.getId(), UNFINISHED_PROJECT_STATUSES)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Nhóm vẫn còn dự án chưa hoàn tất, không thể giải thể"
            );
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

    /** Hỗ trợ UC-2.4 — Tra cứu nhóm theo workspace */
    public Team getTeam(Long workspaceId, Long teamId) {
        return teamRepository.findByIdAndWorkspaceId(teamId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "Không tìm thấy nhóm làm việc"));
    }

    /**
     * Chặn thao tác trên nhóm đã giải thể. Dự án chỉ có {@link CommonStatus#ACTIVE}/{@link CommonStatus#INACTIVE},
     * trong đó INACTIVE chính là trạng thái "đã giải thể" (UC-2.5). Khi có thêm các trạng thái như
     * DISBANDED/DELETED/ARCHIVED thì bổ sung tại đây.
     */
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
            if (!isActiveTeamLeaderInternal(team)) {
                throw new BusinessException(
                        ErrorCode.TEAM_ACCESS_DENIED,
                        "Chỉ Team Leader hoặc workspace owner mới được thao tác"
                );
            }
        }
    }

    /** Hỗ trợ UC-2.4 — Kiểm tra quyền truy cập nhóm qua workspace */
    public void verifyTeamAccess(Team team) {
        workspaceService.verifyAccess(team.getWorkspace());
    }

    /** Hỗ trợ UC-2.7 — Kiểm tra user hiện tại có phải Team Leader đang hoạt động */
    public boolean isActiveTeamLeader(Team team) {
        return isActiveTeamLeaderInternal(team);
    }

    private boolean isActiveTeamLeaderInternal(Team team) {
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
