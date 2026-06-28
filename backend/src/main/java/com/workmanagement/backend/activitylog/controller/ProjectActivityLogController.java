package com.workmanagement.backend.activitylog.controller;

import com.workmanagement.backend.activitylog.dto.response.ActivityLogResponse;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/activity-logs")
@RequiredArgsConstructor
public class ProjectActivityLogController {

    private final ActivityLogService activityLogService;

    /** UC-3.10 — Lịch sử hoạt động dự án */
    @GetMapping
    public ApiResponse<PageResponse<ActivityLogResponse>> findByProject(
            @PathVariable Long workspaceId,
            @PathVariable Long teamId,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.success(activityLogService.findByProject(
                workspaceId, teamId, projectId, page, size, action, targetType, from, to
        ));
    }

}
