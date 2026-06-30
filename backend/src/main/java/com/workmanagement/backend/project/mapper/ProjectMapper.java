package com.workmanagement.backend.project.mapper;

import com.workmanagement.backend.project.dto.response.ProjectResponse;
import com.workmanagement.backend.project.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .teamId(project.getTeam().getId())
                .workspaceId(project.getTeam().getWorkspace().getId())
                .code(project.getCode())
                .name(project.getName())
                .description(project.getDescription())
                .objective(project.getObjective())
                .scope(project.getScope())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .status(project.getStatus())
                .projectManagerMemberId(
                        project.getProjectManagerMember() != null ? project.getProjectManagerMember().getId() : null
                )
                .projectManagerName(
                        project.getProjectManagerMember() != null
                                && project.getProjectManagerMember().getWorkspaceMember() != null
                                && project.getProjectManagerMember().getWorkspaceMember().getUser() != null
                                ? project.getProjectManagerMember().getWorkspaceMember().getUser().getFullName()
                                : null
                )
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

}
