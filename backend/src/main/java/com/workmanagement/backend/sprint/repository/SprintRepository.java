package com.workmanagement.backend.sprint.repository;

import com.workmanagement.backend.sprint.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    Optional<Sprint> findByIdAndProjectId(Long id, Long projectId);

}
