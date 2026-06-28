package com.workmanagement.backend.productbacklog.mapper;

import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogResponse;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import org.springframework.stereotype.Component;

@Component
public class ProductBacklogMapper {

    public ProductBacklogResponse toResponse(ProductBacklog backlog) {
        return ProductBacklogResponse.builder()
                .id(backlog.getId())
                .projectId(backlog.getProject().getId())
                .name(backlog.getName())
                .description(backlog.getDescription())
                .createdAt(backlog.getCreatedAt())
                .updatedAt(backlog.getUpdatedAt())
                .build();
    }

}
