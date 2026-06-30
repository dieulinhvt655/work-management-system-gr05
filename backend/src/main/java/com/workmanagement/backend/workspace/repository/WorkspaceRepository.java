package com.workmanagement.backend.workspace.repository;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long>, JpaSpecificationExecutor<Workspace> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select w.id from Workspace w where w.owner.id = :userId")
    List<Long> findIdsByOwnerId(@Param("userId") Long userId);

    boolean existsByIdInAndOwner_Id(Collection<Long> ids, Long ownerId);
}
