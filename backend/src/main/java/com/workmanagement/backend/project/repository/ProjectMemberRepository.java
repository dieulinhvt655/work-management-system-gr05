package com.workmanagement.backend.project.repository;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
