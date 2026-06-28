package com.workmanagement.backend.task.mapper;

import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.task.dto.response.TaskResponse;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.entity.WorkflowState;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .pbiId(task.getPbi().getId())
                .pbiTitle(task.getPbi().getTitle())
                .sprintId(task.getSprintId())
                .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .progress(task.getProgress())
                .startDate(task.getStartDate())
                .deadline(task.getDeadline())
                .completedAt(task.getCompletedAt())
                .assigneeMemberId(memberId(task.getAssigneeMember()))
                .assigneeName(memberName(task.getAssigneeMember()))
                .reporterMemberId(memberId(task.getReporterMember()))
                .reporterName(memberName(task.getReporterMember()))
                .reviewerMemberId(memberId(task.getReviewerMember()))
                .reviewerName(memberName(task.getReviewerMember()))
                .workflowStateId(workflowStateId(task.getWorkflowState()))
                .workflowStateName(workflowStateName(task.getWorkflowState()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private Long memberId(ProjectMember member) {
        return member != null ? member.getId() : null;
    }

    private String memberName(ProjectMember member) {
        if (member == null) {
            return null;
        }
        return member.getTeamMember().getWorkspaceMember().getUser().getFullName();
    }

    private Long workflowStateId(WorkflowState state) {
        return state != null ? state.getId() : null;
    }

    private String workflowStateName(WorkflowState state) {
        return state != null ? state.getName() : null;
    }

}
