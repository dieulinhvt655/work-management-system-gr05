package com.workmanagement.backend.dashboard.mapper;

import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.dashboard.dto.response.PersonalTaskItemResponse;
import com.workmanagement.backend.dashboard.dto.response.TaskStatusBreakdownResponse;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.sprint.entity.Sprint;
import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardMapperTest {

    private final DashboardMapper dashboardMapper = new DashboardMapper();

    @Test
    void toTaskBreakdown_shouldCountStatuses() {
        List<Task> tasks = List.of(
                Task.builder().status(TaskStatus.TO_DO).build(),
                Task.builder().status(TaskStatus.IN_PROGRESS).build(),
                Task.builder().status(TaskStatus.DONE).build(),
                Task.builder().status(TaskStatus.DONE).build()
        );

        TaskStatusBreakdownResponse response = dashboardMapper.toTaskBreakdown(tasks);

        assertThat(response.getTodo()).isEqualTo(1);
        assertThat(response.getInProgress()).isEqualTo(1);
        assertThat(response.getDone()).isEqualTo(2);
    }

    @Test
    void toSprintDashboard_shouldCalculateCompletionPercent() {
        Sprint sprint = Sprint.builder()
                .id(80L)
                .name("Sprint 1")
                .status(SprintStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .build();
        List<Task> tasks = List.of(
                Task.builder().status(TaskStatus.DONE).build(),
                Task.builder().status(TaskStatus.IN_PROGRESS).build()
        );

        var response = dashboardMapper.toSprintDashboard(sprint, tasks);

        assertThat(response.getCompletionPercent()).isEqualTo(50);
        assertThat(response.getTotalTasks()).isEqualTo(2);
    }

    @Test
    void toUpcomingTasks_shouldSortByDeadline() {
        Task later = Task.builder()
                .id(2L)
                .title("Later")
                .status(TaskStatus.TO_DO)
                .deadline(LocalDate.now().plusDays(5))
                .build();
        Task sooner = Task.builder()
                .id(1L)
                .title("Sooner")
                .status(TaskStatus.TO_DO)
                .deadline(LocalDate.now().plusDays(1))
                .build();

        List<PersonalTaskItemResponse> items = dashboardMapper.toUpcomingTasks(List.of(later, sooner), 5);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getTaskId()).isEqualTo(1L);
    }

    @Test
    void toMemberWorkload_shouldMapMemberName() {
        User user = User.builder().id(2L).fullName("Bob").build();
        WorkspaceMember workspaceMember = WorkspaceMember.builder().user(user).build();
        TeamMember teamMember = TeamMember.builder().workspaceMember(workspaceMember).build();
        ProjectMember member = ProjectMember.builder().id(12L).teamMember(teamMember).build();
        List<Task> tasks = List.of(
                Task.builder().status(TaskStatus.IN_PROGRESS).build(),
                Task.builder().status(TaskStatus.DONE).build()
        );

        var response = dashboardMapper.toMemberWorkload(member, tasks);

        assertThat(response.getMemberName()).isEqualTo("Bob");
        assertThat(response.getAssignedTasks()).isEqualTo(2);
        assertThat(response.getDoneTasks()).isEqualTo(1);
    }

}
