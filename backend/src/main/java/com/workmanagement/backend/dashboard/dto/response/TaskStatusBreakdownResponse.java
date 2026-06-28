package com.workmanagement.backend.dashboard.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskStatusBreakdownResponse {

    private long todo;
    private long inProgress;
    private long review;
    private long done;
    private long reopened;
    private long cancelled;

}
