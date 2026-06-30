package com.workmanagement.backend.project.repository;

import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    Optional<Project> findByIdAndTeamId(Long id, Long teamId);

    boolean existsByCode(String code);

    boolean existsByTeamIdAndNameIgnoreCase(Long teamId, String name);

    List<Project> findByTeamId(Long teamId);

    boolean existsByTeamIdAndStatusIn(Long teamId, Collection<ProjectStatus> statuses);

}
