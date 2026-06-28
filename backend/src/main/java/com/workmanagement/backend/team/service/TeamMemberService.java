package com.workmanagement.backend.team.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
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

    /** Danh sách thành viên nhóm */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('team:read')")
    public List<TeamMemberResponse> findAll(Long workspaceId, Long teamId) {
        Team team = teamService.getTeam(workspaceId, teamId);
        workspaceService.verifyAccess(team.getWorkspace());

        return teamMemberRepository.findByTeamIdAndStatus(teamId, MemberStatus.ACTIVE)
                .stream()
                .map(teamMemberMapper::toResponse)
                .toList();
    }

    /** Thêm thành viên vào nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamMemberResponse add(Long workspaceId, Long teamId, AddTeamMemberRequest request) {
        Team team = teamService.getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());
        teamService.ensureTeamActive(team);

        if (teamMemberRepository.existsByTeamIdAndWorkspaceMemberId(teamId, request.getWorkspaceMemberId())) {
            throw new BusinessException(ErrorCode.TEAM_MEMBER_ALREADY_EXISTS, "Thành viên đã có trong nhóm");
        }

        WorkspaceMember workspaceMember = workspaceMemberRepository.findById(request.getWorkspaceMemberId())
                .filter(m -> m.getWorkspace().getId().equals(workspaceId))
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
                        "Không tìm thấy thành viên workspace hoặc không hoạt động"
                ));

        Role role = roleService.getRole(request.getRoleId());
        validateTeamRole(role);

        if (isTeamLeaderRole(role)) {
            demoteExistingLeaders(teamId);
        }

        WorkspaceMember addedBy = findCurrentWorkspaceMember(workspaceId);

        TeamMember member = TeamMember.builder()
                .team(team)
                .workspaceMember(workspaceMember)
                .role(role)
                .addedByWorkspaceMember(addedBy)
                .status(MemberStatus.ACTIVE)
                .build();

        return teamMemberMapper.toResponse(teamMemberRepository.save(member));
    }

    /** Cập nhật vai trò / trạng thái thành viên nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamMemberResponse update(
            Long workspaceId,
            Long teamId,
            Long memberId,
            UpdateTeamMemberRequest request
    ) {
        Team team = teamService.getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());
        teamService.ensureTeamActive(team);

        TeamMember member = getTeamMember(teamId, memberId);
        Role role = roleService.getRole(request.getRoleId());
        validateTeamRole(role);

        if (isTeamLeaderRole(role)) {
            demoteExistingLeadersExcept(teamId, memberId);
        }

        member.setRole(role);

        if (request.getStatus() != null) {
            if (request.getStatus() == MemberStatus.INACTIVE && isTeamLeaderRole(member.getRole())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể vô hiệu hoá Team Leader, hãy gán leader khác trước");
            }
            member.setStatus(request.getStatus());
            if (request.getStatus() == MemberStatus.INACTIVE) {
                member.setRemovedAt(LocalDateTime.now());
            } else {
                member.setRemovedAt(null);
            }
        }

        return teamMemberMapper.toResponse(teamMemberRepository.save(member));
    }

    /** UC-2.7 — Gán Team Leader cho nhóm */
    @Transactional
    @PreAuthorize("hasAuthority('team:update')")
    public TeamMemberResponse assignLeader(Long workspaceId, Long teamId, Long memberId) {
        Team team = teamService.getTeam(workspaceId, teamId);
        workspaceService.verifyCanManage(team.getWorkspace());
        ensureWorkspaceActive(team.getWorkspace());
        teamService.ensureTeamActive(team);

        TeamMember member = getTeamMember(teamId, memberId);
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thành viên không hoạt động");
        }

        Role leaderRole = findTeamLeaderRole();
        demoteExistingLeaders(teamId);
        member.setRole(leaderRole);

        return teamMemberMapper.toResponse(teamMemberRepository.save(member));
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

}
