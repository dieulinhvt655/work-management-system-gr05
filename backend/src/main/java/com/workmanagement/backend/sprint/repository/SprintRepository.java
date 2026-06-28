package com.workmanagement.backend.sprint.repository;

import com.workmanagement.backend.common.enums.SprintStatus;
import com.workmanagement.backend.sprint.entity.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    Optional<Sprint> findByIdAndProjectId(Long id, Long projectId);

    Page<Sprint> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    Page<Sprint> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, SprintStatus status, Pageable pageable);

    List<Sprint> findByProjectIdAndStatusInOrderByCreatedAtDesc(Long projectId, List<SprintStatus> statuses);

    boolean existsByProjectIdAndStatus(Long projectId, SprintStatus status);

    boolean existsByProjectIdAndStatusAndIdNot(Long projectId, SprintStatus status, Long id);

    Optional<Sprint> findFirstByProjectIdAndStatus(Long projectId, SprintStatus status);

}
