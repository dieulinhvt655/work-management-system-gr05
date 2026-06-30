package com.workmanagement.backend.team.repository;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamIdAndStatus(Long teamId, MemberStatus status);

    boolean existsByTeamIdAndStatus(Long teamId, MemberStatus status);

    Optional<TeamMember> findByIdAndTeamId(Long id, Long teamId);

    boolean existsByTeamIdAndWorkspaceMemberId(Long teamId, Long workspaceMemberId);

    boolean existsByTeamIdAndWorkspaceMemberIdAndStatus(
            Long teamId, Long workspaceMemberId, MemberStatus status);

    Optional<TeamMember> findFirstByTeamIdAndWorkspaceMemberIdAndStatus(
            Long teamId, Long workspaceMemberId, MemberStatus status);

    Optional<TeamMember> findByTeamIdAndWorkspaceMember_User_IdAndStatus(
            Long teamId, Long userId, MemberStatus status);

    List<TeamMember> findByTeamIdAndRole_IdAndStatus(Long teamId, Long roleId, MemberStatus status);

    boolean existsByTeamIdAndWorkspaceMember_User_IdAndRole_NameAndStatus(
            Long teamId, Long userId, String roleName, MemberStatus status);

    List<TeamMember> findByTeam_Workspace_Id(Long workspaceId);

}
