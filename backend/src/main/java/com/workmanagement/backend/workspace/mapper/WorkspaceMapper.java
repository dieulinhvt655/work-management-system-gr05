package com.workmanagement.backend.workspace.mapper;

import com.workmanagement.backend.workspace.dto.response.WorkspaceResponse;
import com.workmanagement.backend.workspace.entity.Workspace;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceMapper {

    public WorkspaceResponse toResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .status(workspace.getStatus())
                .ownerId(workspace.getOwner().getId())
                .ownerName(workspace.getOwner().getFullName())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

}
