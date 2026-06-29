import { getPermissionsForMockRole } from '../constants/mock/rolePermissions'
import { PERMISSIONS } from '../constants/permissions'
import { MOCK_ROLES } from '../constants/roles'

/** Backend role name → frontend permission profile (mock role keys). */
export const BACKEND_ROLE_TO_MOCK_ROLE = {
  'System Admin': MOCK_ROLES.SYSTEM_ADMIN,
  'Workspace Owner': MOCK_ROLES.WORKSPACE_OWNER,
  'Team Leader': MOCK_ROLES.TEAM_LEADER,
  'Project Manager': MOCK_ROLES.PROJECT_MANAGER,
  'Project Contributor': MOCK_ROLES.TEAM_MEMBER,
  'Team Member': MOCK_ROLES.TEAM_MEMBER,
  'Workspace Member': MOCK_ROLES.WORKSPACE_MEMBER,
}

/** Fallback map when role is unknown — backend permission code → frontend codes. */
const BACKEND_CODE_TO_FRONTEND = {
  'user:read': [PERMISSIONS.USER_READ],
  'user:create': [PERMISSIONS.USER_MANAGE],
  'user:update': [PERMISSIONS.USER_MANAGE],
  'user:lock': [PERMISSIONS.USER_MANAGE],
  'user:assign-role': [PERMISSIONS.USER_MANAGE, PERMISSIONS.ROLE_MANAGE],
  'role:read': [PERMISSIONS.ROLE_MANAGE],
  'role:create': [PERMISSIONS.ROLE_MANAGE],
  'role:update': [PERMISSIONS.ROLE_MANAGE],
  'role:delete': [PERMISSIONS.ROLE_MANAGE],
  'role:assign-permission': [PERMISSIONS.ROLE_MANAGE],
  'permission:read': [PERMISSIONS.ROLE_MANAGE],
  'dashboard:read': [
    PERMISSIONS.DASHBOARD_WORKSPACE_READ,
    PERMISSIONS.DASHBOARD_MY_READ,
    PERMISSIONS.DASHBOARD_TEAM_READ,
    PERMISSIONS.DASHBOARD_PROJECT_READ,
  ],
  'workspace:read': [
    PERMISSIONS.WORKSPACE_READ,
    PERMISSIONS.MEMBER_READ,
    PERMISSIONS.WORKSPACE_ACTIVITY_READ,
  ],
  'workspace:create': [PERMISSIONS.WORKSPACE_ADMIN_CREATE],
  'workspace:update': [
    PERMISSIONS.WORKSPACE_MANAGE,
    PERMISSIONS.WORKSPACE_ADMIN_MANAGE,
    PERMISSIONS.WORKSPACE_READ,
  ],
  'workspace:close': [
    PERMISSIONS.WORKSPACE_ADMIN_MANAGE,
    PERMISSIONS.WORKSPACE_MANAGE,
  ],
  'member:read': [PERMISSIONS.MEMBER_READ],
  'member:manage': [PERMISSIONS.MEMBER_MANAGE],
  'team:read': [PERMISSIONS.TEAM_READ],
  'team:create': [PERMISSIONS.TEAM_MANAGE],
  'team:update': [PERMISSIONS.TEAM_MANAGE],
  'team:delete': [PERMISSIONS.TEAM_MANAGE],
  'project:read': [PERMISSIONS.PROJECT_READ],
  'project:create': [PERMISSIONS.PROJECT_CREATE],
  'project:update': [PERMISSIONS.PROJECT_MANAGE],
  'project:manage-members': [PERMISSIONS.PROJECT_MEMBER_MANAGE],
  'backlog:read': [PERMISSIONS.BACKLOG_READ],
  'backlog:create': [PERMISSIONS.BACKLOG_MANAGE],
  'backlog:update': [PERMISSIONS.BACKLOG_MANAGE],
  'backlog:delete': [PERMISSIONS.BACKLOG_MANAGE],
  'sprint:read': [PERMISSIONS.SPRINT_READ],
  'sprint:create': [PERMISSIONS.SPRINT_MANAGE],
  'sprint:update': [PERMISSIONS.SPRINT_MANAGE],
  'sprint:delete': [PERMISSIONS.SPRINT_MANAGE],
  'task:read': [PERMISSIONS.MYWORK_READ],
  'task:create': [PERMISSIONS.MYWORK_READ],
  'task:update': [PERMISSIONS.MYWORK_READ],
  'task:delete': [PERMISSIONS.MYWORK_READ],
  'task:assign': [PERMISSIONS.MYWORK_READ],
  'comment:read': [PERMISSIONS.MYWORK_READ],
  'comment:create': [PERMISSIONS.MYWORK_READ],
  'comment:update': [PERMISSIONS.MYWORK_READ],
  'comment:delete': [PERMISSIONS.MYWORK_READ],
  'attachment:read': [PERMISSIONS.PROJECT_DOC_READ],
  'attachment:create': [PERMISSIONS.PROJECT_DOC_READ],
  'attachment:delete': [PERMISSIONS.PROJECT_DOC_READ],
  'notification:read': [PERMISSIONS.PROFILE_READ],
}

function mapBackendCodes(backendCodes = []) {
  const resolved = new Set()

  for (const code of backendCodes) {
    const mapped = BACKEND_CODE_TO_FRONTEND[code]
    if (mapped?.length) {
      mapped.forEach((permission) => resolved.add(permission))
    } else {
      resolved.add(code)
    }
  }

  return [...resolved]
}

export function resolveFrontendPermissions(roleName, backendCodes = []) {
  if (backendCodes.length > 0) {
    return mapBackendCodes(backendCodes)
  }

  const mockRole = BACKEND_ROLE_TO_MOCK_ROLE[roleName]
  if (mockRole) {
    return getPermissionsForMockRole(mockRole)
  }

  return mapBackendCodes(backendCodes)
}

export function isSystemAdminRole(roleName, roleScope) {
  return (
    roleName === 'System Admin' ||
    String(roleScope ?? '').toUpperCase() === 'SYSTEM'
  )
}

export function resolveWorkspaceScope(roleName, roleScope, apiUser = {}) {
  if (isSystemAdminRole(roleName, roleScope)) {
    return {
      workspaceId: null,
      managedWorkspaceIds: [],
    }
  }

  return {
    workspaceId: apiUser.workspaceId ?? null,
    managedWorkspaceIds: apiUser.managedWorkspaceIds ?? [],
  }
}
