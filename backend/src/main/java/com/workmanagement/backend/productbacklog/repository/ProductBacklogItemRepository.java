package com.workmanagement.backend.productbacklog.repository;

import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductBacklogItemRepository extends JpaRepository<ProductBacklogItem, Long>, JpaSpecificationExecutor<ProductBacklogItem> {

    Optional<ProductBacklogItem> findByIdAndBacklog_Project_Id(Long id, Long projectId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE pbi_id = :pbiId", nativeQuery = true)
    long countTasksByPbiId(@Param("pbiId") Long pbiId);

    List<ProductBacklogItem> findBySprintIdOrderByCreatedAtDesc(Long sprintId);

    long countBySprintId(Long sprintId);

    @Query("SELECT COUNT(p) FROM ProductBacklogItem p WHERE p.backlog.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(p) FROM ProductBacklogItem p WHERE p.backlog.project.id = :projectId AND p.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") PbiStatus status);

}
