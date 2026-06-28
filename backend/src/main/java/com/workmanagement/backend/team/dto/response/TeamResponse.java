package com.workmanagement.backend.team.dto.response;

import com.workmanagement.backend.common.enums.CommonStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamResponse {

    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private CommonStatus status;
    private Long teamLeaderId;
    private String teamLeaderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
