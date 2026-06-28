package com.workmanagement.backend.productbacklog.dto.response;

import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.PbiType;
import com.workmanagement.backend.common.enums.PriorityLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductBacklogItemResponse {

    private Long id;
    private Long backlogId;
    private Long projectId;
    private Long sprintId;
    private String title;
    private String description;
    private PbiType type;
    private PriorityLevel priority;
    private PbiStatus status;
    private LocalDate desiredDueDate;
    private Long proposerMemberId;
    private String proposerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
