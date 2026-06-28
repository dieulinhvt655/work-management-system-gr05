package com.workmanagement.backend.productbacklog.repository;

import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductBacklogRepository extends JpaRepository<ProductBacklog, Long> {

    Optional<ProductBacklog> findByProjectId(Long projectId);

}
