package com.workmanagement.backend.project.repository;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectIdAndStatus(Long projectId, MemberStatus status);

    Optional<ProjectMember> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndTeamMemberId(Long projectId, Long teamMemberId);

    Optional<ProjectMember> findByProjectIdAndTeamMemberId(Long projectId, Long teamMemberId);

    Optional<ProjectMember> findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
            Long projectId,
            Long userId,
            MemberStatus status
    );

    @Query("""
            select distinct pm.project.id
            from ProjectMember pm
            where pm.project.team.id = :teamId
              and pm.teamMember.workspaceMember.user.id = :userId
              and pm.status = :status
              and pm.role.name = 'Project Manager'
            """)
    List<Long> findManagedProjectIdsByTeamAndUser(
            @Param("teamId") Long teamId,
            @Param("userId") Long userId,
            @Param("status") MemberStatus status
    );

    @Query("""
            select distinct pm.project.id
            from ProjectMember pm
            where pm.project.team.id = :teamId
              and pm.teamMember.workspaceMember.user.id = :userId
              and pm.status = :status
            """)
    List<Long> findParticipatingProjectIdsByTeamAndUser(
            @Param("teamId") Long teamId,
            @Param("userId") Long userId,
            @Param("status") MemberStatus status
    );

}
