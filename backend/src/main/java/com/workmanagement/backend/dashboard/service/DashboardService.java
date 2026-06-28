package com.workmanagement.backend.dashboard.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.dashboard.dto.request.DashboardFilterRequest;
import com.workmanagement.backend.dashboard.dto.response.DashboardSummaryResponse;
import com.workmanagement.backend.dashboard.dto.response.MemberWorkloadResponse;
import com.workmanagement.backend.dashboard.dto.response.PersonalDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.ProjectDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.SprintDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.TeamSummaryItemResponse;
import com.workmanagement.backend.dashboard.mapper.DashboardMapper;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogItemRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.repository.ProjectRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.sprint.entity.Sprint;
import com.workmanagement.backend.sprint.repository.SprintRepository;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.team.service.TeamService;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkspaceService workspaceService;
    private final TeamService teamService;
    private final ProjectService projectService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final ProductBacklogItemRepository productBacklogItemRepository;
    private final DashboardMapper dashboardMapper;

    /** UC-7.1 — Dashboard tổng quan workspace */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('dashboard:read')")
    public DashboardSummaryResponse getWorkspaceDashboard(Long workspaceId) {
        Workspace workspace = workspaceService.getWorkspace(workspaceId);
        workspaceService.verifyCanManage(workspace);

        List<Team> teams = teamRepository.findByWorkspaceId(workspaceId).stream()
                .filter(team -> team.getStatus() == CommonStatus.ACTIVE)
                .toList();
        List<Project> projects = teams.stream()
                .flatMap(team -> projectRepository.findByTeamId(team.getId()).stream())
                .toList();

        long activeProjects = projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .count();
        long activeSprints = projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .filter(project -> sprintRepository.existsByProjectIdAndStatus(project.getId(), SprintStatus.ACTIVE))
                .count();

        List<TeamSummaryItemResponse> teamSummaries = teams.stream()
                .map(team -> toTeamSummary(team))
                .toList();

        return DashboardSummaryResponse.builder()
                .scopeId(workspace.getId())
                .scopeName(workspace.getName())
                .scopeType("workspace")
                .totalTeams(teams.size())
                .totalProjects(projects.size())
                .totalMembers(workspaceMemberRepository
                        .findByWorkspaceIdAndStatus(workspaceId, MemberStatus.ACTIVE)
                        .size())
                .activeProjects(activeProjects)
                .activeSprints(activeSprints)
                .projectsByStatus(countProjectsByStatus(projects))
                .teams(teamSummaries)
                .build();
    }

    /** UC-7.2 — Dashboard nhóm làm việc */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('dashboard:read')")
    public DashboardSummaryResponse getTeamDashboard(Long workspaceId, Long teamId) {
        Team team = teamService.getTeam(workspaceId, teamId);
        verifyTeamLeader(team);

        List<Project> projects = projectRepository.findByTeamId(teamId);
        long activeProjects = projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .count();
        long activeSprints = projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .filter(project -> sprintRepository.existsByProjectIdAndStatus(project.getId(), SprintStatus.ACTIVE))
                .count();

        return DashboardSummaryResponse.builder()
                .scopeId(team.getId())
                .scopeName(team.getName())
                .scopeType("team")
                .totalTeams(1)
                .totalProjects(projects.size())
                .totalMembers(teamMemberRepository.findByTeamIdAndStatus(teamId, MemberStatus.ACTIVE).size())
                .activeProjects(activeProjects)
                .activeSprints(activeSprints)
                .projectsByStatus(countProjectsByStatus(projects))
                .teams(List.of(toTeamSummary(team)))
                .build();
    }

    /** UC-7.3 — Dashboard dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('dashboard:read')")
    public ProjectDashboardResponse getProjectDashboard(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyProjectManager(project);

        List<Task> projectTasks = resolveProjectTasks(project);
        SprintDashboardResponse activeSprint = sprintRepository
                .findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)
                .map(sprint -> dashboardMapper.toSprintDashboard(
                        sprint,
                        taskRepository.findBySprintIdOrderByCreatedAtDesc(sprint.getId())
                ))
                .orElse(null);

        List<MemberWorkloadResponse> workload = projectMemberRepository
                .findByProjectIdAndStatus(projectId, MemberStatus.ACTIVE)
                .stream()
                .map(member -> toMemberWorkload(member, projectTasks))
                .filter(item -> item.getAssignedTasks() > 0)
                .toList();

        return ProjectDashboardResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .status(project.getStatus())
                .totalPbis(productBacklogItemRepository.countByProjectId(projectId))
                .readyPbis(productBacklogItemRepository.countByProjectIdAndStatus(projectId, PbiStatus.READY))
                .inSprintPbis(productBacklogItemRepository.countByProjectIdAndStatus(projectId, PbiStatus.IN_SPRINT))
                .completedPbis(productBacklogItemRepository.countByProjectIdAndStatus(projectId, PbiStatus.COMPLETED))
                .activeSprint(activeSprint)
                .taskBreakdown(dashboardMapper.toTaskBreakdown(projectTasks))
                .memberWorkload(workload)
                .build();
    }

    /** UC-7.4 — Dashboard cá nhân trong dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('dashboard:read')")
    public PersonalDashboardResponse getPersonalDashboard(
            Long workspaceId,
            Long teamId,
            Long projectId,
            DashboardFilterRequest filter
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyActiveProjectMember(project);

        int upcomingLimit = filter != null && filter.getUpcomingLimit() != null
                ? Math.min(Math.max(filter.getUpcomingLimit(), 1), 20)
                : 5;

        Long userId = SecurityUtils.getCurrentUserId();
        List<Task> assignedTasks = taskRepository.findAssignedByProjectIdAndUserId(projectId, userId);

        return PersonalDashboardResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .totalAssignedTasks(assignedTasks.size())
                .taskBreakdown(dashboardMapper.toTaskBreakdown(assignedTasks))
                .upcomingTasks(dashboardMapper.toUpcomingTasks(assignedTasks, upcomingLimit))
                .inProgressTasks(dashboardMapper.toInProgressTasks(assignedTasks))
                .build();
    }

    private TeamSummaryItemResponse toTeamSummary(Team team) {
        List<Project> projects = projectRepository.findByTeamId(team.getId());
        long activeProjects = projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .count();

        return TeamSummaryItemResponse.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .projectCount(projects.size())
                .activeProjects(activeProjects)
                .build();
    }

    private Map<ProjectStatus, Long> countProjectsByStatus(List<Project> projects) {
        Map<ProjectStatus, Long> counts = new EnumMap<>(ProjectStatus.class);
        Arrays.stream(ProjectStatus.values()).forEach(status -> counts.put(status, 0L));
        projects.forEach(project -> counts.merge(project.getStatus(), 1L, Long::sum));
        return counts;
    }

    private List<Task> resolveProjectTasks(Project project) {
        Optional<Sprint> activeSprint = sprintRepository
                .findFirstByProjectIdAndStatus(project.getId(), SprintStatus.ACTIVE);
        if (activeSprint.isPresent()) {
            return taskRepository.findBySprintIdOrderByCreatedAtDesc(activeSprint.get().getId());
        }
        return taskRepository.findByProjectId(project.getId());
    }

    private MemberWorkloadResponse toMemberWorkload(ProjectMember member, List<Task> projectTasks) {
        List<Task> memberTasks = projectTasks.stream()
                .filter(task -> task.getAssigneeMember() != null)
                .filter(task -> member.getId().equals(task.getAssigneeMember().getId()))
                .toList();
        return dashboardMapper.toMemberWorkload(member, memberTasks);
    }

    private void verifyTeamLeader(Team team) {
        if (!teamService.isActiveTeamLeader(team)) {
            throw new BusinessException(
                    ErrorCode.DASHBOARD_ACCESS_DENIED,
                    "Chỉ Team Leader mới được xem dashboard nhóm"
            );
        }
    }

    private void verifyProjectManager(Project project) {
        if (!projectService.isActiveProjectManager(project)) {
            throw new BusinessException(
                    ErrorCode.DASHBOARD_ACCESS_DENIED,
                    "Chỉ Project Manager mới được xem dashboard dự án"
            );
        }
    }

    private void verifyActiveProjectMember(Project project) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isMember = projectMemberRepository
                .findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                        project.getId(),
                        userId,
                        MemberStatus.ACTIVE
                )
                .isPresent();
        if (!isMember) {
            throw new BusinessException(
                    ErrorCode.DASHBOARD_ACCESS_DENIED,
                    "Chỉ thành viên dự án mới được xem dashboard cá nhân"
            );
        }
    }

}
