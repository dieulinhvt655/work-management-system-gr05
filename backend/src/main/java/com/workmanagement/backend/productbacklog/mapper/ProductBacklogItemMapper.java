package com.workmanagement.backend.productbacklog.mapper;

import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
import com.workmanagement.backend.project.entity.ProjectMember;
import org.springframework.stereotype.Component;

@Component
public class ProductBacklogItemMapper {

    public ProductBacklogItemResponse toResponse(ProductBacklogItem item) {
        ProjectMember proposer = item.getProposerMember();
        String proposerName = null;
        Long proposerMemberId = null;
        if (proposer != null) {
            proposerMemberId = proposer.getId();
            proposerName = proposer.getTeamMember().getWorkspaceMember().getUser().getFullName();
        }

        return ProductBacklogItemResponse.builder()
                .id(item.getId())
                .backlogId(item.getBacklog().getId())
                .projectId(item.getBacklog().getProject().getId())
                .sprintId(item.getSprintId())
                .title(item.getTitle())
                .description(item.getDescription())
                .type(item.getType())
                .priority(item.getPriority())
                .status(item.getStatus())
                .desiredDueDate(item.getDesiredDueDate())
                .proposerMemberId(proposerMemberId)
                .proposerName(proposerName)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

}
