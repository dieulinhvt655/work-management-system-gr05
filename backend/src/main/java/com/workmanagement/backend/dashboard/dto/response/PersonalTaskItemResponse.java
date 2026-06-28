package com.workmanagement.backend.dashboard.dto.response;

import com.workmanagement.backend.common.enums.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PersonalTaskItemResponse {

    private Long taskId;
    private String title;
    private TaskStatus status;
    private LocalDate deadline;
    private Integer progress;
    private Long sprintId;

}
