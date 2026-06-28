package com.workmanagement.backend.project.service;

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
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private TeamService teamService;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProjectService projectService;

    private Team team;
    private Project project;
    private TeamMember pmTeamMember;
    private Role pmRole;
    private Role contributorRole;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        User pmUser = User.builder().id(2L).fullName("PM User").build();
        WorkspaceMember wsMember = WorkspaceMember.builder().id(5L).user(pmUser).build();
        pmTeamMember = TeamMember.builder()
                .id(7L)
                .team(team)
                .workspaceMember(wsMember)
                .status(MemberStatus.ACTIVE)
                .build();
        project = Project.builder()
                .id(30L)
                .team(team)
                .projectManagerMember(pmTeamMember)
                .code("PRJ-001")
                .name("Alpha")
                .status(ProjectStatus.DRAFT)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();
        pmRole = Role.builder().id(1L).name("Project Manager").scope(RoleScope.PROJECT).build();
        contributorRole = Role.builder().id(2L).name("Project Contributor").scope(RoleScope.PROJECT).build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldCreateDraftProject() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setCode("PRJ-001");
        request.setName("Alpha");
        request.setProjectManagerMemberId(7L);
        ProjectResponse response = ProjectResponse.builder().id(30L).name("Alpha").build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.existsByCode("PRJ-001")).thenReturn(false);
        when(teamMemberRepository.findByIdAndTeamId(7L, 20L)).thenReturn(Optional.of(pmTeamMember));
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(roleRepository.findByNameAndScope("Project Manager", RoleScope.PROJECT)).thenReturn(Optional.of(pmRole));
        when(projectMapper.toResponse(project)).thenReturn(response);

        ProjectResponse result = projectService.create(10L, 20L, request);

        assertThat(result.getName()).isEqualTo("Alpha");
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldReturnPagedProjects() {
        ProjectResponse response = ProjectResponse.builder().id(30L).name("Alpha").build();

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        doNothing().when(teamService).verifyTeamAccess(team);
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(project)));
        when(projectMapper.toResponse(project)).thenReturn(response);

        PageResponse<ProjectResponse> result = projectService.findAll(10L, 20L, 0, 20, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("Alpha");
    }

    @Test
    void update_shouldUpdateProjectInfo() {
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("Beta");

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).name("Beta").build()
        );

        ProjectResponse result = projectService.update(10L, 20L, 30L, request);

        assertThat(result.getName()).isEqualTo("Beta");
        assertThat(project.getName()).isEqualTo("Beta");
    }

    @Test
    void update_shouldRejectWhenNotTeamLeaderOrPm() {
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("Beta");

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doThrow(new BusinessException(ErrorCode.TEAM_ACCESS_DENIED, "denied"))
                .when(teamService).verifyCanManageProject(team);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);

        assertThatThrownBy(() -> projectService.update(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void activate_shouldChangeDraftToActive() {
        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).status(ProjectStatus.ACTIVE).build()
        );

        projectService.activate(10L, 20L, 30L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void activate_shouldRejectNonDraftProject() {
        project.setStatus(ProjectStatus.ACTIVE);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);

        assertThatThrownBy(() -> projectService.activate(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void complete_shouldChangeActiveToCompleted() {
        project.setStatus(ProjectStatus.ACTIVE);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).status(ProjectStatus.COMPLETED).build()
        );

        projectService.complete(10L, 20L, 30L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    }

    @Test
    void complete_shouldRejectNonPm() {
        project.setStatus(ProjectStatus.ACTIVE);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.complete(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void archive_shouldChangeCompletedToArchived() {
        project.setStatus(ProjectStatus.COMPLETED);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(
                ProjectResponse.builder().id(30L).status(ProjectStatus.ARCHIVED).build()
        );

        projectService.archive(10L, 20L, 30L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void archive_shouldRejectNonCompletedProject() {
        project.setStatus(ProjectStatus.ACTIVE);

        when(teamService.getTeam(10L, 20L)).thenReturn(team);
        when(projectRepository.findByIdAndTeamId(30L, 20L)).thenReturn(Optional.of(project));
        doNothing().when(teamService).verifyCanManageProject(team);

        assertThatThrownBy(() -> projectService.archive(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

}
