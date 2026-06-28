package com.workmanagement.backend.project.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
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

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectService projectService;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepository;
    private final RoleService roleService;
    private final ActivityLogService activityLogService;

    /** UC-2.8 — Danh sách thành viên dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public List<ProjectMemberResponse> findAll(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        return projectMemberRepository.findByProjectIdAndStatus(projectId, MemberStatus.ACTIVE)
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
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        Team team = project.getTeam();
        teamService.verifyCanManageProject(team);
        ensureProjectOpen(project);

        if (projectMemberRepository.existsByProjectIdAndTeamMemberId(projectId, request.getTeamMemberId())) {
            throw new BusinessException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS, "Thành viên đã có trong dự án");
        }

        TeamMember teamMember = teamMemberRepository.findByIdAndTeamId(request.getTeamMemberId(), teamId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TEAM_MEMBER_NOT_FOUND,
                        "Thành viên phải thuộc nhóm và đang active"
                ));

        Role role = roleService.getRole(request.getRoleId());
        validateProjectRole(role);

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .teamMember(teamMember)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .build();
        member = projectMemberRepository.save(member);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_MEMBER_ADDED,
                ActivityLogAction.TARGET_PROJECT_MEMBER,
                member.getId(),
                teamMember.getWorkspaceMember().getUser().getFullName(),
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
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        teamService.verifyCanManageProject(project.getTeam());
        ensureProjectOpen(project);

        ProjectMember member = getProjectMember(projectId, memberId);
        Role role = roleService.getRole(request.getRoleId());
        validateProjectRole(role);

        if (isProjectManagerRole(member.getRole())
                && request.getStatus() == MemberStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Không thể vô hiệu hoá Project Manager, hãy chỉ định PM khác trước"
            );
        }

        member.setRole(role);

        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
            if (request.getStatus() == MemberStatus.INACTIVE) {
                member.setRemovedAt(LocalDateTime.now());
            } else {
                member.setRemovedAt(null);
            }
        }

        member = projectMemberRepository.save(member);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PROJECT_MEMBER_UPDATED,
                ActivityLogAction.TARGET_PROJECT_MEMBER,
                member.getId(),
                role.getName(),
                project
        );

        return projectMemberMapper.toResponse(member);
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
    }

    private boolean isProjectManagerRole(Role role) {
        return PROJECT_MANAGER_ROLE.equals(role.getName());
    }

    private void ensureProjectOpen(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
    }

}
