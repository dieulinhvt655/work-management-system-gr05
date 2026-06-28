package com.workmanagement.backend.workspace.dto.response;

import com.workmanagement.backend.common.enums.CommonStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WorkspaceResponse {

    private Long id;
    private String name;
    private String description;
    private CommonStatus status;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
