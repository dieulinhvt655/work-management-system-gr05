package com.workmanagement.backend.security.repository;

import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.RolePermission;
import com.workmanagement.backend.security.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRole_Id(Long roleId);

    void deleteByRole_Id(Long roleId);

    @Query("SELECT rp.permission FROM RolePermission rp WHERE rp.role.id = :roleId")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    @Query("""
            select rp.permission.code from RolePermission rp
            where rp.role.id = (select u.role.id from User u where u.id = :userId)
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

}
