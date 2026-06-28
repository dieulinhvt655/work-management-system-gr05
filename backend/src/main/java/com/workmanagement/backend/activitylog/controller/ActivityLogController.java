package com.workmanagement.backend.activitylog.controller;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.response.ApiResponse;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.activitylog.dto.response.ActivityLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    /** UC-2.9 — Theo dõi hoạt động tổ chức */
    @GetMapping
    public ApiResponse<PageResponse<ActivityLogResponse>> findByWorkspace(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.success(activityLogService.findByWorkspace(
                workspaceId, page, size, action, targetType, from, to
        ));
    }

}
