package com.workmanagement.backend.task.repository;

import com.workmanagement.backend.task.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findByProjectId(Long projectId);

    Optional<WorkflowTransition> findByIdAndProjectId(Long id, Long projectId);

    Optional<WorkflowTransition> findByProjectIdAndFromStateIdAndToStateId(
            Long projectId,
            Long fromStateId,
            Long toStateId
    );

}
