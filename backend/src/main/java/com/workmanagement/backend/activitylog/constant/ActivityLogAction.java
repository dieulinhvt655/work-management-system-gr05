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
    public static final String PROJECT_MEMBER_ADDED = "PROJECT_MEMBER_ADDED";
    public static final String PROJECT_MEMBER_UPDATED = "PROJECT_MEMBER_UPDATED";

    public static final String TARGET_WORKSPACE = "WORKSPACE";
    public static final String TARGET_WORKSPACE_MEMBER = "WORKSPACE_MEMBER";
    public static final String TARGET_TEAM = "TEAM";
    public static final String TARGET_TEAM_MEMBER = "TEAM_MEMBER";
    public static final String TARGET_PROJECT = "PROJECT";
    public static final String TARGET_PROJECT_MEMBER = "PROJECT_MEMBER";

}
