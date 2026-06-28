package com.workmanagement.backend.workspace.repository;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findByWorkspaceIdAndStatus(Long workspaceId, MemberStatus status);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserIdAndStatus(Long workspaceId, Long userId, MemberStatus status);

}
