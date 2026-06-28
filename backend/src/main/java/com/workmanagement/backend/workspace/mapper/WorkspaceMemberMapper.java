package com.workmanagement.backend.workspace.mapper;

import com.workmanagement.backend.security.mapper.RoleMapper;
import com.workmanagement.backend.user.mapper.UserMapper;
import com.workmanagement.backend.workspace.dto.response.WorkspaceMemberResponse;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberMapper {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public WorkspaceMemberResponse toResponse(WorkspaceMember member) {
        return WorkspaceMemberResponse.builder()
                .id(member.getId())
                .workspaceId(member.getWorkspace().getId())
                .user(userMapper.toResponse(member.getUser()))
                .role(roleMapper.toResponse(member.getRole(), List.of()))
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .removedAt(member.getRemovedAt())
                .build();
    }

}
