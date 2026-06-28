package com.workmanagement.backend.team.dto.response;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamMemberResponse {

    private Long id;
    private Long teamId;
    private Long workspaceMemberId;
    private UserResponse user;
    private RoleResponse role;
    private MemberStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime removedAt;

}
