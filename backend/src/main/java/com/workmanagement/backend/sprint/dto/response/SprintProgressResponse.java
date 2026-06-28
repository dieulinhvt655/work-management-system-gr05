package com.workmanagement.backend.sprint.dto.response;

import com.workmanagement.backend.common.enums.SprintStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SprintProgressResponse {

    private Long sprintId;
    private String sprintName;
    private SprintStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalTasks;
    private long todoTasks;
    private long inProgressTasks;
    private long reviewTasks;
    private long doneTasks;
    private long cancelledTasks;
    private int completionPercent;
    private List<BurndownPointResponse> burndown;

}
