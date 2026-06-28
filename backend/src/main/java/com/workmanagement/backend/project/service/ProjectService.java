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
import com.workmanagement.backend.project.dto.request.CreateProjectRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String PROJECT_MANAGER_ROLE = "Project Manager";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    /** Tạo dự án (hỗ trợ UC-2.8 — cần project trước khi phân bổ member) */
    @Transactional
    @PreAuthorize("hasAuthority('project:create')")
    public ProjectResponse create(Long workspaceId, Long teamId, CreateProjectRequest request) {
        Team team = teamService.getTeam(workspaceId, teamId);
        teamService.verifyCanManageProject(team);
        ensureTeamActive(team);

        if (projectRepository.existsByCode(request.getCode().trim())) {
            throw new BusinessException(ErrorCode.PROJECT_CODE_ALREADY_EXISTS, "Mã dự án đã tồn tại");
        }

        TeamMember projectManager = teamMemberRepository.findByIdAndTeamId(
                        request.getProjectManagerMemberId(), teamId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TEAM_MEMBER_NOT_FOUND,
                        "Project Manager phải là thành viên active của nhóm"
                ));

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

    /** Chi tiết dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('project:read')")
    public ProjectResponse findById(Long workspaceId, Long teamId, Long projectId) {
        Project project = getProject(workspaceId, teamId, projectId);
        verifyProjectAccess(project);
        return projectMapper.toResponse(project);
    }

    public Project getProject(Long workspaceId, Long teamId, Long projectId) {
        Team team = teamService.getTeam(workspaceId, teamId);
        return projectRepository.findByIdAndTeamId(projectId, team.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "Không tìm thấy dự án"));
    }

    void verifyProjectAccess(Project project) {
        teamService.verifyTeamAccess(project.getTeam());
    }

    private void ensureTeamActive(Team team) {
        if (team.getStatus() == CommonStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nhóm đã giải thể");
        }
    }

    private Role findProjectRole(String name) {
        return roleRepository.findByNameAndScope(name, RoleScope.PROJECT)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Không tìm thấy vai trò dự án"));
    }

    private void validateDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

}
