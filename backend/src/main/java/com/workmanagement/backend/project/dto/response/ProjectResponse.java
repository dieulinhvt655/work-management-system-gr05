package com.workmanagement.backend.project.dto.response;

import com.workmanagement.backend.common.enums.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectResponse {

    private Long id;
    private Long teamId;
    private Long workspaceId;
    private String code;
    private String name;
    private String description;
    private String objective;
    private String scope;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProjectStatus status;
    private Long projectManagerMemberId;
    private String projectManagerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
