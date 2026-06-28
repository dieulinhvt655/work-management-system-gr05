package com.workmanagement.backend.activitylog.repository;

import com.workmanagement.backend.activitylog.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

}
