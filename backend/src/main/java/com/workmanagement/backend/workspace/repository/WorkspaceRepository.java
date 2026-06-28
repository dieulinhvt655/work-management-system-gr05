package com.workmanagement.backend.workspace.repository;

import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long>, JpaSpecificationExecutor<Workspace> {
}
