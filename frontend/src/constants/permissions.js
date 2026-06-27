export const PERMISSIONS = {
  // Dashboard
  DASHBOARD_WORKSPACE_READ: 'dashboard:workspace:read',
  DASHBOARD_TEAM_READ: 'dashboard:team:read',
  DASHBOARD_PROJECT_READ: 'dashboard:project:read',
  DASHBOARD_MY_READ: 'dashboard:my:read',

  // Workspace — system-wide (System Admin: all workspaces)
  WORKSPACE_ADMIN_READ: 'workspace:admin:read',
  WORKSPACE_ADMIN_CREATE: 'workspace:admin:create',
  WORKSPACE_ADMIN_MANAGE: 'workspace:admin:manage',
  WORKSPACE_ADMIN_ACTIVITY_READ: 'workspace:admin:activity:read',

  // Workspace — scoped to user's managed workspace(s) (Owner)
  WORKSPACE_READ: 'workspace:read',
  WORKSPACE_MANAGE: 'workspace:manage',
  WORKSPACE_ACTIVITY_READ: 'workspace:activity:read',

  // User (system)
  USER_READ: 'user:read',
  USER_MANAGE: 'user:manage',

  // Team
  TEAM_READ: 'team:read',
  TEAM_MANAGE: 'team:manage',

  // Member
  MEMBER_READ: 'member:read',
  MEMBER_MANAGE: 'member:manage',

  // Project
  PROJECT_READ: 'project:read',
  PROJECT_CREATE: 'project:create',
  PROJECT_MANAGE: 'project:manage',

  // My Work
  MYWORK_READ: 'mywork:read',

  // Project detail (tabs — used later)
  BACKLOG_READ: 'backlog:read',
  BACKLOG_MANAGE: 'backlog:manage',
  SPRINT_READ: 'sprint:read',
  SPRINT_MANAGE: 'sprint:manage',
  PROJECT_MEMBER_MANAGE: 'project:member:manage',
  PROJECT_DOC_READ: 'project:doc:read',
  PROJECT_ACTIVITY_READ: 'project:activity:read',

  // RBAC & audit
  ROLE_MANAGE: 'role:manage',
  AUDIT_READ: 'audit:read',
  SETTINGS_MANAGE: 'settings:manage',

  // Profile
  PROFILE_READ: 'profile:read',
}

export const ALL_PERMISSIONS = Object.values(PERMISSIONS)
