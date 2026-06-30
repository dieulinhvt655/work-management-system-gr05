package com.workmanagement.backend.project.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.dto.request.AddProjectMemberRequest;
import com.workmanagement.backend.project.dto.request.UpdateProjectMemberRequest;
import com.workmanagement.backend.project.dto.response.ProjectMemberResponse;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.mapper.ProjectMemberMapper;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.repository.RoleRepository;
import com.workmanagement.backend.security.service.RoleService;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private static final String PROJECT_MANAGER_ROLE = "Project Manager";
    private static final String PROJECT_CONTRIBUTOR_ROLE = "Project Contributor";

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectService projectService;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepository;
    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    /** UC-2.8 — Danh sách thành viên dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public List<ProjectMemberResponse> findAll(Long workspaceId, Long teamId, Long projectId) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");

        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        return projectMemberRepository.findByProjectIdAndStatus(project.getId(), MemberStatus.ACTIVE)
                .stream()
                .map(projectMemberMapper::toResponse)
                .toList();
    }

    /** UC-2.8 — Phân bổ thành viên nhóm vào dự án */
    @Transactional
    @PreAuthorize("hasAuthority('project:manage-members')")
    public ProjectMemberResponse add(
            Long workspaceId,
            Long teamId,
            Long projectId,
            AddProjectMemberRequest request
    ) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");
        validateAddRequest(request);

        Project project = projectService.getProject(workspaceId, teamId, projectId);
        Team team = project.getTeam();
        ensureProjectMemberManagementAllowed(project);
        verifyCanManageProjectMembers(project);

        TeamMember teamMember = teamMemberRepository.findByIdAndTeamId(request.getTeamMemberId(), teamId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TEAM_MEMBER_NOT_FOUND,
                        "Thành viên phải thuộc nhóm và đang active"
                ));

        Role role = resolveProjectRole(request.getRoleId());
        validateProjectRole(role);

        ProjectMember member = projectMemberRepository.findByProjectIdAndTeamMemberId(project.getId(), teamMember.getId())
                .orElseGet(() -> ProjectMember.builder()
                        .project(project)
                        .teamMember(teamMember)
                        .build());
        if (member.getStatus() == MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS, "Thành viên đã có trong dự án");
        }

        boolean existingRecord = member.getId() != null;
        applyRoleChange(project, member, teamMember, role);
        member.setStatus(MemberStatus.ACTIVE);
        member.setRemovedAt(null);
        member = projectMemberRepository.save(member);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                existingRecord ? ActivityLogAction.PROJECT_MEMBER_UPDATED : ActivityLogAction.PROJECT_MEMBER_ADDED,
                ActivityLogAction.TARGET_PROJECT_MEMBER,
                member.getId(),
                describeMember(member),
                project
        );

        return projectMemberMapper.toResponse(member);
    }

    /** UC-2.8 — Cập nhật vai trò / trạng thái thành viên dự án */
    @Transactional
    @PreAuthorize("hasAuthority('project:manage-members')")
    public ProjectMemberResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long memberId,
            UpdateProjectMemberRequest request
    ) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");
        validateId(memberId, "Member id");
        validateUpdateRequest(request);

        Project project = projectService.getProject(workspaceId, teamId, projectId);
        ensureProjectMemberManagementAllowed(project);
        verifyCanManageProjectMembers(project);

        ProjectMember member = getProjectMember(projectId, memberId);
        Role role = resolveProjectRole(request.getRoleId());
        validateProjectRole(role);

        boolean changed = false;
        boolean currentMemberIsPm = isProjectManagerMember(project, member);
        boolean targetRoleIsPm = isProjectManagerRole(role);

        if (currentMemberIsPm && !targetRoleIsPm) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Không thể thay đổi vai trò Project Manager khi chưa chỉ định PM khác"
                );
        }

        if (targetRoleIsPm) {
            if (!currentMemberIsPm || !sameRole(member.getRole(), role)) {
                applyRoleChange(project, member, member.getTeamMember(), role);
                changed = true;
            }
        } else if (!sameRole(member.getRole(), role)) {
            member.setRole(role);
            changed = true;
        }

        if (request.getStatus() != null) {
            if (request.getStatus() == MemberStatus.INACTIVE && (currentMemberIsPm || targetRoleIsPm)) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Không thể vô hiệu hoá Project Manager, hãy chỉ định PM khác trước"
                );
            }
            if (request.getStatus() != member.getStatus()) {
                changed = true;
            }
            member.setStatus(request.getStatus());
            if (request.getStatus() == MemberStatus.INACTIVE) {
                member.setRemovedAt(LocalDateTime.now());
            } else {
                member.setRemovedAt(null);
            }
        }

        if (!changed) {
            return projectMemberMapper.toResponse(member);
        }

        member = projectMemberRepository.save(member);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_MEMBER_UPDATED,
                ActivityLogAction.TARGET_PROJECT_MEMBER,
                member.getId(),
                describeMember(member),
                project
        );

        return projectMemberMapper.toResponse(member);
    }

    /** UC-2.8 — Gỡ thành viên khỏi dự án */
    @Transactional
    @PreAuthorize("hasAuthority('project:manage-members')")
    public void remove(Long workspaceId, Long teamId, Long projectId, Long memberId) {
        validateId(workspaceId, "Workspace id");
        validateId(teamId, "Team id");
        validateId(projectId, "Project id");
        validateId(memberId, "Member id");

        Project project = projectService.getProject(workspaceId, teamId, projectId);
        ensureProjectMemberManagementAllowed(project);
        verifyCanManageProjectMembers(project);

        ProjectMember member = getProjectMember(projectId, memberId);
        if (isProjectManagerMember(project, member) || isProjectManagerRole(member.getRole())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Không thể xóa Project Manager, hãy chỉ định PM khác trước"
            );
        }

        if (member.getStatus() == MemberStatus.INACTIVE) {
            return;
        }

        member.setStatus(MemberStatus.INACTIVE);
        member.setRemovedAt(LocalDateTime.now());
        member = projectMemberRepository.save(member);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_MEMBER_REMOVED,
                ActivityLogAction.TARGET_PROJECT_MEMBER,
                member.getId(),
                describeMember(member),
                project
        );
    }

    private ProjectMember getProjectMember(Long projectId, Long memberId) {
        return projectMemberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PROJECT_MEMBER_NOT_FOUND,
                        "Không tìm thấy thành viên dự án"
                ));
    }

    private void validateProjectRole(Role role) {
        if (role.getScope() != RoleScope.PROJECT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ được gán vai trò PROJECT scope");
        }
        if (!PROJECT_MANAGER_ROLE.equals(role.getName()) && !PROJECT_CONTRIBUTOR_ROLE.equals(role.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ hỗ trợ Project Manager hoặc Project Contributor");
        }
    }

    private boolean isProjectManagerRole(Role role) {
        return PROJECT_MANAGER_ROLE.equals(role.getName());
    }

    private boolean sameRole(Role currentRole, Role newRole) {
        return currentRole != null && newRole != null && currentRole.getId().equals(newRole.getId());
    }

    private void applyRoleChange(Project project, ProjectMember member, TeamMember teamMember, Role role) {
        if (isProjectManagerRole(role)) {
            promoteProjectManager(project, member, role, teamMember);
            return;
        }
        member.setRole(role);
    }

    private boolean isProjectManagerMember(Project project, ProjectMember member) {
        return project.getProjectManagerMember() != null
                && member.getTeamMember() != null
                && project.getProjectManagerMember().getId().equals(member.getTeamMember().getId());
    }

    private void promoteProjectManager(Project project, ProjectMember member, Role role, TeamMember newPm) {
        Role contributorRole = getProjectContributorRole();
        TeamMember oldPm = project.getProjectManagerMember();

        if (oldPm != null && (newPm == null || !oldPm.getId().equals(newPm.getId()))) {
            projectMemberRepository.findByProjectIdAndTeamMemberId(project.getId(), oldPm.getId())
                    .ifPresent(oldPmMember -> {
                        oldPmMember.setRole(contributorRole);
                        oldPmMember.setStatus(MemberStatus.ACTIVE);
                        oldPmMember.setRemovedAt(null);
                        projectMemberRepository.save(oldPmMember);
                    });
        }

        member.setRole(role);
        member.setStatus(MemberStatus.ACTIVE);
        member.setRemovedAt(null);
        project.setProjectManagerMember(newPm);
    }

    private Role resolveProjectRole(Long roleId) {
        return roleService.getRole(roleId);
    }

    private Role getProjectContributorRole() {
        return roleRepository.findByNameAndScope(PROJECT_CONTRIBUTOR_ROLE, RoleScope.PROJECT)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò Project Contributor"));
    }

    private void ensureProjectMemberManagementAllowed(Project project) {
        if (project.getTeam().getWorkspace().getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Workspace đã ngừng hoạt động");
        }
        if (project.getTeam().getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhóm đã giải thể");
        }
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
    }

    private void verifyCanManageProjectMembers(Project project) {
        try {
            teamService.verifyCanManageProject(project.getTeam());
            return;
        } catch (BusinessException ex) {
            if (projectService.isActiveProjectManager(project)) {
                return;
            }
        }

        throw new BusinessException(
                ErrorCode.PROJECT_ACCESS_DENIED,
                "Chỉ Team Leader hoặc Project Manager mới được quản lý thành viên dự án"
        );
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " không hợp lệ");
        }
    }

    private void validateAddRequest(AddProjectMemberRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        validateId(request.getTeamMemberId(), "teamMemberId");
        validateId(request.getRoleId(), "roleId");
    }

    private void validateUpdateRequest(UpdateProjectMemberRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request không được để trống");
        }
        validateId(request.getRoleId(), "roleId");
    }

    private String describeMember(ProjectMember member) {
        if (member == null || member.getTeamMember() == null
                || member.getTeamMember().getWorkspaceMember() == null
                || member.getTeamMember().getWorkspaceMember().getUser() == null) {
            return null;
        }
        String fullName = member.getTeamMember().getWorkspaceMember().getUser().getFullName();
        String roleName = member.getRole() != null ? member.getRole().getName() : null;
        if (roleName == null) {
            return fullName;
        }
        return fullName + " (" + roleName + ")";
    }

}
