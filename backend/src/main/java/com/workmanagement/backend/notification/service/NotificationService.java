package com.workmanagement.backend.notification.service;

import com.workmanagement.backend.project.entity.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NotificationService {

    public void notifyProjectActivated(Project project, List<Long> recipientUserIds) {
        if (project == null || recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }
        log.info(
                "Project activation notification prepared for projectId={}, projectName={}, recipients={}",
                project.getId(),
                project.getName(),
                recipientUserIds
        );
    }

    public void notifyProjectCompleted(Project project, List<Long> recipientUserIds) {
        if (project == null || recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }
        log.info(
                "Project completion notification prepared for projectId={}, projectName={}, recipients={}",
                project.getId(),
                project.getName(),
                recipientUserIds
        );
    }
}
