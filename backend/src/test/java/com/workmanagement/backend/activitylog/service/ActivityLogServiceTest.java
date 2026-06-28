package com.workmanagement.backend.activitylog.service;

import com.workmanagement.backend.activitylog.dto.response.ActivityLogResponse;
import com.workmanagement.backend.activitylog.entity.ActivityLog;
import com.workmanagement.backend.activitylog.mapper.ActivityLogMapper;
import com.workmanagement.backend.activitylog.repository.ActivityLogRepository;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.repository.TeamMemberRepository;
import com.workmanagement.backend.team.repository.TeamRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.workspace.repository.WorkspaceMemberRepository;
import com.workmanagement.backend.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private ActivityLogMapper activityLogMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private ActivityLogService activityLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(activityLogService, "workspaceService", workspaceService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByWorkspace_shouldReturnPagedLogs() {
        Workspace workspace = Workspace.builder().id(10L).build();
        User actor = User.builder().id(1L).fullName("Admin").build();
        ActivityLog log = ActivityLog.builder()
                .id(1L)
                .actor(actor)
                .action("TEAM_CREATED")
                .targetType("TEAM")
                .targetId(20L)
                .build();
        ActivityLogResponse response = ActivityLogResponse.builder().id(1L).action("TEAM_CREATED").build();

        when(workspaceService.getWorkspace(10L)).thenReturn(workspace);
        doNothing().when(workspaceService).verifyAccess(workspace);
        when(teamRepository.findByWorkspaceId(10L)).thenReturn(List.of(Team.builder().id(20L).build()));
        when(workspaceMemberRepository.findByWorkspaceId(10L)).thenReturn(List.of());
        when(teamMemberRepository.findByTeam_Workspace_Id(10L)).thenReturn(List.of());
        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(activityLogMapper.toResponse(log)).thenReturn(response);

        PageResponse<ActivityLogResponse> result = activityLogService.findByWorkspace(
                10L, 0, 20, null, null, null, null
        );

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getAction()).isEqualTo("TEAM_CREATED");
    }

}
