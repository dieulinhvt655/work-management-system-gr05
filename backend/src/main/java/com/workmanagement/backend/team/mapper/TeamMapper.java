package com.workmanagement.backend.team.mapper;

import com.workmanagement.backend.team.dto.response.TeamResponse;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        return toResponse(team, null);
    }

    public TeamResponse toResponse(Team team, TeamMember leader) {
        TeamResponse.TeamResponseBuilder builder = TeamResponse.builder()
                .id(team.getId())
                .workspaceId(team.getWorkspace().getId())
                .name(team.getName())
                .description(team.getDescription())
                .status(team.getStatus())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt());

        if (leader != null && leader.getWorkspaceMember() != null && leader.getWorkspaceMember().getUser() != null) {
            builder.teamLeaderId(leader.getId())
                    .teamLeaderName(leader.getWorkspaceMember().getUser().getFullName());
        }

        return builder.build();
    }

}
