package com.workmanagement.backend.dashboard.mapper;

import com.workmanagement.backend.common.enums.TaskStatus;
import com.workmanagement.backend.dashboard.dto.response.MemberWorkloadResponse;
import com.workmanagement.backend.dashboard.dto.response.PersonalTaskItemResponse;
import com.workmanagement.backend.dashboard.dto.response.SprintDashboardResponse;
import com.workmanagement.backend.dashboard.dto.response.TaskStatusBreakdownResponse;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.sprint.entity.Sprint;
import com.workmanagement.backend.task.entity.Task;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DashboardMapper {

    public TaskStatusBreakdownResponse toTaskBreakdown(List<Task> tasks) {
        return TaskStatusBreakdownResponse.builder()
                .todo(countByStatus(tasks, TaskStatus.TO_DO))
                .inProgress(countByStatus(tasks, TaskStatus.IN_PROGRESS))
                .review(countByStatus(tasks, TaskStatus.REVIEW))
                .done(countByStatus(tasks, TaskStatus.DONE))
                .reopened(countByStatus(tasks, TaskStatus.REOPENED))
                .cancelled(countByStatus(tasks, TaskStatus.CANCELLED))
                .build();
    }

    public SprintDashboardResponse toSprintDashboard(Sprint sprint, List<Task> tasks) {
        long cancelled = countByStatus(tasks, TaskStatus.CANCELLED);
        long done = countByStatus(tasks, TaskStatus.DONE);
        long totalActive = tasks.size() - cancelled;
        int completionPercent = totalActive == 0 ? 0 : (int) Math.round((done * 100.0) / totalActive);

        return SprintDashboardResponse.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .status(sprint.getStatus())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .totalTasks(tasks.size())
                .doneTasks(done)
                .completionPercent(completionPercent)
                .taskBreakdown(toTaskBreakdown(tasks))
                .build();
    }

    public MemberWorkloadResponse toMemberWorkload(ProjectMember member, List<Task> tasks) {
        return MemberWorkloadResponse.builder()
                .memberId(member.getId())
                .memberName(member.getTeamMember().getWorkspaceMember().getUser().getFullName())
                .assignedTasks(tasks.size())
                .inProgressTasks(countByStatus(tasks, TaskStatus.IN_PROGRESS)
                        + countByStatus(tasks, TaskStatus.REVIEW)
                        + countByStatus(tasks, TaskStatus.REOPENED))
                .doneTasks(countByStatus(tasks, TaskStatus.DONE))
                .build();
    }

    public PersonalTaskItemResponse toPersonalTaskItem(Task task) {
        return PersonalTaskItemResponse.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .deadline(task.getDeadline())
                .progress(task.getProgress())
                .sprintId(task.getSprintId())
                .build();
    }

    public List<PersonalTaskItemResponse> toUpcomingTasks(List<Task> tasks, int limit) {
        return tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED)
                .filter(task -> task.getDeadline() != null)
                .sorted(Comparator.comparing(Task::getDeadline))
                .limit(limit)
                .map(this::toPersonalTaskItem)
                .toList();
    }

    public List<PersonalTaskItemResponse> toInProgressTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS
                        || task.getStatus() == TaskStatus.REVIEW
                        || task.getStatus() == TaskStatus.REOPENED)
                .sorted(Comparator.comparing(Task::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toPersonalTaskItem)
                .toList();
    }

    private long countByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream().filter(task -> task.getStatus() == status).count();
    }

}
