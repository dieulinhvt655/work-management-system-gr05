package com.workmanagement.backend.team.repository;

import com.workmanagement.backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    Optional<Team> findByIdAndWorkspaceId(Long id, Long workspaceId);

    java.util.List<Team> findByWorkspaceId(Long workspaceId);

    boolean existsByWorkspaceIdAndNameIgnoreCase(Long workspaceId, String name);

}
