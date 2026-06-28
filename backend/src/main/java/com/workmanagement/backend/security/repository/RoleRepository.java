package com.workmanagement.backend.security.repository;

import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByScope(RoleScope scope);

    Optional<Role> findByNameAndScope(String name, RoleScope scope);

    boolean existsByNameAndScope(String name, RoleScope scope);

}
