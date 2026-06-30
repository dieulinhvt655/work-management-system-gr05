package com.workmanagement.backend.workspace.repository;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findByWorkspaceIdAndStatus(Long workspaceId, MemberStatus status);

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    List<WorkspaceMember> findByUserIdAndStatus(Long userId, MemberStatus status);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByWorkspaceIdAndUserIdAndStatus(Long workspaceId, Long userId, MemberStatus status);

    boolean existsByWorkspaceIdInAndUserIdAndStatus(
            Collection<Long> workspaceIds, Long userId, MemberStatus status);

    @Query("""
            select wm.workspace.id from WorkspaceMember wm
            where wm.user.id = :userId
              and wm.status = :status
              and wm.role.name = 'Workspace Owner'
            """)
    List<Long> findOwnedWorkspaceIds(
            @Param("userId") Long userId,
            @Param("status") MemberStatus status);

}
