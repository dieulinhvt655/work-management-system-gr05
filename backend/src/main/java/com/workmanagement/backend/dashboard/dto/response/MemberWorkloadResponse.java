package com.workmanagement.backend.dashboard.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberWorkloadResponse {

    private Long memberId;
    private String memberName;
    private long assignedTasks;
    private long inProgressTasks;
    private long doneTasks;

}
