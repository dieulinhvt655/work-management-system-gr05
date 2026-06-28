package com.workmanagement.backend.security.repository;

import com.workmanagement.backend.security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByCode(String code);

    List<Permission> findByModule(String module);

}
