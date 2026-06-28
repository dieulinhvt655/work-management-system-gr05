package com.workmanagement.backend.project.mapper;

import com.workmanagement.backend.project.dto.response.ProjectMemberResponse;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.security.mapper.RoleMapper;
import com.workmanagement.backend.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectMemberMapper {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public ProjectMemberResponse toResponse(ProjectMember member) {
        return ProjectMemberResponse.builder()
                .id(member.getId())
                .projectId(member.getProject().getId())
                .teamMemberId(member.getTeamMember().getId())
                .user(userMapper.toResponse(member.getTeamMember().getWorkspaceMember().getUser()))
                .role(roleMapper.toResponse(member.getRole(), List.of()))
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .removedAt(member.getRemovedAt())
                .build();
    }

}
