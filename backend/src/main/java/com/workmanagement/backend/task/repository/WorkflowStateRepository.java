package com.workmanagement.backend.task.repository;

import com.workmanagement.backend.task.entity.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {

    List<WorkflowState> findByProjectIdOrderByPositionAsc(Long projectId);

    Optional<WorkflowState> findByIdAndProjectId(Long id, Long projectId);

    Optional<WorkflowState> findByProjectIdAndIsDefaultTrue(Long projectId);

    Optional<WorkflowState> findByProjectIdAndCode(Long projectId, String code);

    boolean existsByProjectId(Long projectId);

}
