package com.workmanagement.backend.sprint.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class BurndownPointResponse {

    private LocalDate date;
    private long remainingTasks;
    private long completedTasks;

}
