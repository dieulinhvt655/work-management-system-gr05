package com.workmanagement.backend.team.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
import com.workmanagement.backend.team.dto.request.TransferTeamMemberRequest;
import com.workmanagement.backend.team.dto.request.UpdateTeamMemberRequest;
import com.workmanagement.backend.team.dto.response.TeamMemberResponse;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.mapper.TeamMemberMapper;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private static final String TEAM_LEADER_ROLE = "Team Leader";
    private static final String TEAM_MEMBER_ROLE = "Team Member";

    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamService teamService;
    private final WorkspaceService workspaceService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    /** UC-2.4 — Danh sách thành viên nhóm */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('team:read')")
    public List<TeamMemberResponse> findAll(Long workspaceId, Long teamId) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }

        Team team = teamService.getTeam(workspaceId, teamId);
        workspaceService.verifyAccess(team.getWorkspace());

        return teamMemberRepository.findByTeamIdAndStatus(teamId, MemberStatus.ACTIVE)
                .stream()
                .map(teamMemberMapper::toResponse)
                .toList();
    }

    /** UC-2.4 — Thêm thành viên workspace vào nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamMemberResponse add(Long workspaceId, Long teamId, AddTeamMemberRequest request) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        if (request.getWorkspaceMemberId() == null || request.getWorkspaceMemberId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceMemberId không hợp lệ");
        }
        if (request.getRoleId() == null || request.getRoleId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "roleId không hợp lệ");
        }

        Team team = teamService.getTeam(workspaceId, teamId);
        boolean workspaceManager = isWorkspaceManager(team);
        if (!workspaceManager && !teamService.isActiveTeamLeader(team)) {
            throw new BusinessException(
                    ErrorCode.TEAM_ACCESS_DENIED,
                    "Chỉ Workspace Owner/System Admin hoặc Team Leader của nhóm mới được thêm thành viên"
            );
        }
        ensureWorkspaceActive(team.getWorkspace());
        teamService.ensureTeamActive(team);

        WorkspaceMember workspaceMember = workspaceMemberRepository.findById(request.getWorkspaceMemberId())
                .filter(m -> m.getWorkspace().getId().equals(workspaceId))
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
                        "Không tìm thấy thành viên workspace hoặc không hoạt động"
                ));

        if (workspaceMember.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Tài khoản người dùng không hoạt động");
        }

        Role role = roleService.getRole(request.getRoleId());
        validateTeamRole(role);

        boolean assigningLeader = isTeamLeaderRole(role);
        if (assigningLeader && !workspaceManager) {
            throw new BusinessException(
                    ErrorCode.TEAM_ACCESS_DENIED,
                    "Chỉ Workspace Owner/System Admin mới được gán Team Leader"
            );
        }

        if (teamMemberRepository.existsByTeamIdAndWorkspaceMemberIdAndStatus(
                teamId, request.getWorkspaceMemberId(), MemberStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.TEAM_MEMBER_ALREADY_EXISTS, "Thành viên đã có trong nhóm");
        }

        if (assigningLeader) {
            demoteExistingLeaders(teamId);
        }

        WorkspaceMember addedBy = findCurrentWorkspaceMember(workspaceId);

        // Tái sử dụng bản ghi cũ đã INACTIVE (nếu có) thay vì báo trùng, tránh tạo trùng lặp dữ liệu.
        TeamMember member = teamMemberRepository
                .findFirstByTeamIdAndWorkspaceMemberIdAndStatus(
                        teamId, request.getWorkspaceMemberId(), MemberStatus.INACTIVE)
                .orElseGet(TeamMember::new);
        member.setTeam(team);
        member.setWorkspaceMember(workspaceMember);
        member.setRole(role);
        member.setAddedByWorkspaceMember(addedBy);
        member.setStatus(MemberStatus.ACTIVE);
        member.setRemovedAt(null);

        member = teamMemberRepository.save(member);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_MEMBER_ADDED,
                ActivityLogAction.TARGET_TEAM_MEMBER,
                member.getId(),
                workspaceMember.getUser().getFullName()
        );

        return teamMemberMapper.toResponse(member);
    }

    /** UC-2.4 — Cập nhật vai trò / trạng thái thành viên nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamMemberResponse update(
            Long workspaceId,
            Long teamId,
            Long memberId,
            UpdateTeamMemberRequest request
    ) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }
        if (memberId == null || memberId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Member id không hợp lệ");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        if (request.getRoleId() != null && request.getRoleId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "roleId không hợp lệ");
        }
        if (request.getRoleId() == null && request.getStatus() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không có thông tin cập nhật");
        }

        Team team = teamService.getTeam(workspaceId, teamId);
        boolean workspaceManager = isWorkspaceManager(team);
        if (!workspaceManager && !teamService.isActiveTeamLeader(team)) {
            throw new BusinessException(
                    ErrorCode.TEAM_ACCESS_DENIED,
                    "Chỉ Workspace Owner/System Admin hoặc Team Leader của nhóm mới được cập nhật thành viên"
            );
        }
        ensureWorkspaceActive(team.getWorkspace());
        teamService.ensureTeamActive(team);

        TeamMember member = getTeamMember(teamId, memberId);
        boolean currentlyLeader = isTeamLeaderRole(member.getRole());
        boolean changed = false;

        if (request.getRoleId() != null) {
            Role role = roleService.getRole(request.getRoleId());
            validateTeamRole(role);

            boolean assigningLeader = isTeamLeaderRole(role);
            if (assigningLeader && !workspaceManager) {
                throw new BusinessException(
                        ErrorCode.TEAM_ACCESS_DENIED,
                        "Chỉ Workspace Owner/System Admin mới được gán Team Leader"
                );
            }
            if (!role.getId().equals(member.getRole().getId())) {
                if (assigningLeader) {
                    demoteExistingLeadersExcept(teamId, memberId);
                }
                member.setRole(role);
                changed = true;
            }
        }

        if (request.getStatus() != null && request.getStatus() != member.getStatus()) {
            boolean leaderInvolved = currentlyLeader || isTeamLeaderRole(member.getRole());
            if (request.getStatus() == MemberStatus.INACTIVE && leaderInvolved) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Không thể vô hiệu hoá Team Leader, hãy gán leader khác trước"
                );
            }
            if (request.getStatus() == MemberStatus.ACTIVE
                    && member.getWorkspaceMember().getUser().getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException(
                        ErrorCode.USER_INACTIVE,
                        "Tài khoản người dùng không hoạt động, không thể kích hoạt lại trong nhóm"
                );
            }
            member.setStatus(request.getStatus());
            member.setRemovedAt(request.getStatus() == MemberStatus.INACTIVE ? LocalDateTime.now() : null);
            changed = true;
        }

        if (!changed) {
            return teamMemberMapper.toResponse(member);
        }

        member = teamMemberRepository.save(member);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_MEMBER_UPDATED,
                ActivityLogAction.TARGET_TEAM_MEMBER,
                member.getId(),
                member.getRole().getName()
        );

        return teamMemberMapper.toResponse(member);
    }

    /** UC-2.7 — Gán Team Leader cho nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamMemberResponse assignLeader(Long workspaceId, Long teamId, Long memberId) {
        validateTeamLeaderRequest(workspaceId, teamId, memberId);
        Team team = teamService.getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());
        teamService.ensureTeamActive(team);

        TeamMember member = getTeamMember(teamId, memberId);
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thành viên không hoạt động");
        }
        if (isTeamLeaderRole(member.getRole())) {
            return teamMemberMapper.toResponse(member);
        }

        Role leaderRole = findTeamLeaderRole();
        demoteExistingLeaders(teamId);
        member.setRole(leaderRole);

        member = teamMemberRepository.save(member);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_LEADER_ASSIGNED,
                ActivityLogAction.TARGET_TEAM_MEMBER,
                member.getId(),
                member.getWorkspaceMember().getUser().getFullName()
        );

        return teamMemberMapper.toResponse(member);
    }

    /** UC-2.7 — Thu hồi Team Leader */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamMemberResponse revokeLeader(Long workspaceId, Long teamId, Long memberId) {
        validateTeamLeaderRequest(workspaceId, teamId, memberId);
        Team team = teamService.getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());
        teamService.ensureTeamActive(team);

        TeamMember member = getTeamMember(teamId, memberId);
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thành viên không hoạt động");
        }
        if (!isTeamLeaderRole(member.getRole())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thành viên không phải Team Leader");
        }

        member.setRole(findTeamMemberRole());
        member = teamMemberRepository.save(member);

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_LEADER_REVOKED,
                ActivityLogAction.TARGET_TEAM_MEMBER,
                member.getId(),
                member.getWorkspaceMember().getUser().getFullName()
        );

        return teamMemberMapper.toResponse(member);
    }

    /** UC-2.6 — Điều chuyển nhân sự giữa các team */
    @Transactional
    @PreAuthorize("hasAuthority('workspace:update')")
    public TeamMemberResponse transfer(
            Long workspaceId,
            Long teamId,
            Long memberId,
            TransferTeamMemberRequest request
    ) {
        validateTeamMemberTransferRequest(workspaceId, teamId, memberId, request);

        Team sourceTeam = teamService.getTeam(workspaceId, teamId);
        Team targetTeam = teamService.getTeam(workspaceId, request.getTargetTeamId());
        if (sourceTeam.getId().equals(targetTeam.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team nguồn và team đích phải khác nhau");
        }

        workspaceService.verifyCanManage(sourceTeam.getWorkspace());
        ensureWorkspaceActive(sourceTeam.getWorkspace());
        teamService.ensureTeamActive(sourceTeam);
        teamService.ensureTeamActive(targetTeam);

        TeamMember sourceMember = getTeamMember(teamId, memberId);
        if (sourceMember.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thành viên không hoạt động");
        }
        if (isTeamLeaderRole(sourceMember.getRole())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Không thể điều chuyển Team Leader, hãy gán leader khác trước"
            );
        }

        WorkspaceMember workspaceMember = sourceMember.getWorkspaceMember();
        if (workspaceMember == null || workspaceMember.getUser() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thông tin thành viên không hợp lệ");
        }
        if (workspaceMember.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Tài khoản người dùng không hoạt động");
        }

        TeamMember targetMember = teamMemberRepository
                .findFirstByTeamIdAndWorkspaceMemberIdAndStatus(
                        targetTeam.getId(),
                        workspaceMember.getId(),
                        MemberStatus.ACTIVE
                )
                .orElseGet(() -> teamMemberRepository
                        .findFirstByTeamIdAndWorkspaceMemberIdAndStatus(
                                targetTeam.getId(),
                                workspaceMember.getId(),
                                MemberStatus.INACTIVE
                        )
                        .orElseGet(TeamMember::new));

        Role teamMemberRole = findTeamMemberRole();
        WorkspaceMember addedBy = findCurrentWorkspaceMember(workspaceId);

        sourceMember.setStatus(MemberStatus.INACTIVE);
        sourceMember.setRemovedAt(LocalDateTime.now());
        teamMemberRepository.save(sourceMember);

        if (targetMember.getId() == null || targetMember.getStatus() != MemberStatus.ACTIVE) {
            targetMember.setTeam(targetTeam);
            targetMember.setWorkspaceMember(workspaceMember);
            targetMember.setRole(teamMemberRole);
            targetMember.setAddedByWorkspaceMember(addedBy);
            targetMember.setStatus(MemberStatus.ACTIVE);
            targetMember.setRemovedAt(null);
            targetMember = teamMemberRepository.save(targetMember);
        }

        activityLogService.recordOrgEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.TEAM_MEMBER_TRANSFERRED,
                ActivityLogAction.TARGET_TEAM_MEMBER,
                targetMember.getId(),
                sourceTeam.getName() + " -> " + targetTeam.getName() + " : " + workspaceMember.getUser().getFullName()
        );

        return teamMemberMapper.toResponse(targetMember);
    }

    private TeamMember getTeamMember(Long teamId, Long memberId) {
        return teamMemberRepository.findByIdAndTeamId(memberId, teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND, "Không tìm thấy thành viên nhóm"));
    }

    private WorkspaceMember findCurrentWorkspaceMember(Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElse(null);
    }

    /** Actor là Workspace Owner / System Admin (quản lý mọi team trong workspace). */
    private boolean isWorkspaceManager(Team team) {
        try {
            workspaceService.verifyCanManage(team.getWorkspace());
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void validateTeamRole(Role role) {
        if (role.getScope() != RoleScope.TEAM) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ được gán vai trò TEAM scope");
        }
    }

    private boolean isTeamLeaderRole(Role role) {
        return TEAM_LEADER_ROLE.equals(role.getName());
    }

    private Role findTeamLeaderRole() {
        return roleRepository.findByNameAndScope(TEAM_LEADER_ROLE, RoleScope.TEAM)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò Team Leader"));
    }

    private Role findTeamMemberRole() {
        return roleRepository.findByNameAndScope(TEAM_MEMBER_ROLE, RoleScope.TEAM)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò Team Member"));
    }

    private void demoteExistingLeaders(Long teamId) {
        Role leaderRole = findTeamLeaderRole();
        Role memberRole = findTeamMemberRole();
        teamMemberRepository.findByTeamIdAndRole_IdAndStatus(teamId, leaderRole.getId(), MemberStatus.ACTIVE)
                .forEach(leader -> leader.setRole(memberRole));
    }

    private void demoteExistingLeadersExcept(Long teamId, Long exceptMemberId) {
        Role leaderRole = findTeamLeaderRole();
        Role memberRole = findTeamMemberRole();
        teamMemberRepository.findByTeamIdAndRole_IdAndStatus(teamId, leaderRole.getId(), MemberStatus.ACTIVE)
                .stream()
                .filter(m -> !m.getId().equals(exceptMemberId))
                .forEach(leader -> leader.setRole(memberRole));
    }

    private void ensureWorkspaceActive(Workspace workspace) {
        if (workspace.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã đóng");
        }
    }

    private void validateTeamLeaderRequest(Long workspaceId, Long teamId, Long memberId) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace id không hợp lệ");
        }
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Team id không hợp lệ");
        }
        if (memberId == null || memberId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Member id không hợp lệ");
        }
    }

    private void validateTeamMemberTransferRequest(
            Long workspaceId,
            Long teamId,
            Long memberId,
            TransferTeamMemberRequest request
    ) {
        validateTeamLeaderRequest(workspaceId, teamId, memberId);
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        if (request.getTargetTeamId() == null || request.getTargetTeamId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "targetTeamId không hợp lệ");
        }
    }

}
