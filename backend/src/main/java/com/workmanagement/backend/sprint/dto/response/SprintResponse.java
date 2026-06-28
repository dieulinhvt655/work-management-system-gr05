package com.workmanagement.backend.sprint.dto.response;

import com.workmanagement.backend.common.enums.SprintStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SprintResponse {

    private Long id;
    private Long projectId;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatus status;
    private String summary;
    private long pbiCount;
    private long taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

}
