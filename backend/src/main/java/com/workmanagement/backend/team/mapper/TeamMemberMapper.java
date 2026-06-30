package com.workmanagement.backend.team.mapper;

import com.workmanagement.backend.security.mapper.RoleMapper;
import com.workmanagement.backend.team.dto.response.TeamMemberResponse;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.mapper.UserMapper;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamMemberMapper {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public TeamMemberResponse toResponse(TeamMember member) {
        WorkspaceMember workspaceMember = member.getWorkspaceMember();
        User user = workspaceMember != null ? workspaceMember.getUser() : null;

        return TeamMemberResponse.builder()
                .id(member.getId())
                .teamId(member.getTeam() != null ? member.getTeam().getId() : null)
                .workspaceMemberId(workspaceMember != null ? workspaceMember.getId() : null)
                .user(user != null ? userMapper.toResponse(user) : null)
                .role(member.getRole() != null ? roleMapper.toResponse(member.getRole(), List.of()) : null)
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .removedAt(member.getRemovedAt())
                .build();
    }

}
