package com.workmanagement.backend.activitylog.constant;

public final class ActivityLogAction {

    private ActivityLogAction() {
    }

    public static final String WORKSPACE_CREATED = "WORKSPACE_CREATED";
    public static final String WORKSPACE_UPDATED = "WORKSPACE_UPDATED";
    public static final String WORKSPACE_CLOSED = "WORKSPACE_CLOSED";
    public static final String WORKSPACE_MEMBER_ADDED = "WORKSPACE_MEMBER_ADDED";
    public static final String WORKSPACE_MEMBER_UPDATED = "WORKSPACE_MEMBER_UPDATED";
    public static final String TEAM_CREATED = "TEAM_CREATED";
    public static final String TEAM_UPDATED = "TEAM_UPDATED";
    public static final String TEAM_DISBANDED = "TEAM_DISBANDED";
    public static final String TEAM_MEMBER_ADDED = "TEAM_MEMBER_ADDED";
    public static final String TEAM_MEMBER_UPDATED = "TEAM_MEMBER_UPDATED";
    public static final String TEAM_LEADER_ASSIGNED = "TEAM_LEADER_ASSIGNED";
    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_UPDATED = "PROJECT_UPDATED";
    public static final String PROJECT_ACTIVATED = "PROJECT_ACTIVATED";
    public static final String PROJECT_COMPLETED = "PROJECT_COMPLETED";
    public static final String PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
    public static final String PROJECT_MEMBER_ADDED = "PROJECT_MEMBER_ADDED";
    public static final String PROJECT_MEMBER_UPDATED = "PROJECT_MEMBER_UPDATED";
    public static final String ATTACHMENT_UPLOADED = "ATTACHMENT_UPLOADED";
    public static final String ATTACHMENT_UPDATED = "ATTACHMENT_UPDATED";
    public static final String ATTACHMENT_DELETED = "ATTACHMENT_DELETED";
    public static final String PBI_CREATED = "PBI_CREATED";
    public static final String PBI_UPDATED = "PBI_UPDATED";
    public static final String PBI_DELETED = "PBI_DELETED";
    public static final String PBI_MARKED_READY = "PBI_MARKED_READY";
    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_UPDATED = "TASK_UPDATED";
    public static final String TASK_DELETED = "TASK_DELETED";
    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TASK_ASSIGNMENT_CONFIRMED = "TASK_ASSIGNMENT_CONFIRMED";
    public static final String TASK_STARTED = "TASK_STARTED";
    public static final String TASK_PROGRESS_UPDATED = "TASK_PROGRESS_UPDATED";
    public static final String TASK_APPROVED = "TASK_APPROVED";
    public static final String TASK_REJECTED = "TASK_REJECTED";
    public static final String TASK_ACTIVATED_IN_SPRINT = "TASK_ACTIVATED_IN_SPRINT";
    public static final String SPRINT_CREATED = "SPRINT_CREATED";
    public static final String SPRINT_UPDATED = "SPRINT_UPDATED";
    public static final String SPRINT_STARTED = "SPRINT_STARTED";
    public static final String SPRINT_COMPLETED = "SPRINT_COMPLETED";
    public static final String SPRINT_CANCELLED = "SPRINT_CANCELLED";
    public static final String SPRINT_PBI_ADDED = "SPRINT_PBI_ADDED";
    public static final String SPRINT_PBI_REMOVED = "SPRINT_PBI_REMOVED";

    public static final String TARGET_WORKSPACE = "WORKSPACE";
    public static final String TARGET_WORKSPACE_MEMBER = "WORKSPACE_MEMBER";
    public static final String TARGET_TEAM = "TEAM";
    public static final String TARGET_TEAM_MEMBER = "TEAM_MEMBER";
    public static final String TARGET_PROJECT = "PROJECT";
    public static final String TARGET_PROJECT_MEMBER = "PROJECT_MEMBER";
    public static final String TARGET_ATTACHMENT = "ATTACHMENT";
    public static final String TARGET_PBI = "PBI";
    public static final String TARGET_TASK = "TASK";
    public static final String TARGET_SPRINT = "SPRINT";

}
