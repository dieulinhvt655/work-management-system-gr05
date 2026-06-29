/** Mock login only — UI must not branch on role, use permissions instead. */
export const MOCK_ROLES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  WORKSPACE_OWNER: 'WORKSPACE_OWNER',
  WORKSPACE_MEMBER: 'WORKSPACE_MEMBER',
  TEAM_LEADER: 'TEAM_LEADER',
  PROJECT_MANAGER: 'PROJECT_MANAGER',
  TEAM_MEMBER: 'TEAM_MEMBER',
}

export const MOCK_ROLE_LABELS = {
  [MOCK_ROLES.SYSTEM_ADMIN]: 'System Admin',
  [MOCK_ROLES.WORKSPACE_OWNER]: 'Workspace Owner',
  [MOCK_ROLES.WORKSPACE_MEMBER]: 'Workspace Member',
  [MOCK_ROLES.TEAM_LEADER]: 'Team Leader',
  [MOCK_ROLES.PROJECT_MANAGER]: 'Project Manager',
  [MOCK_ROLES.TEAM_MEMBER]: 'Project Contributor',
}

/** Backend role scope values. */
export const ROLE_SCOPE = {
  SYSTEM: 'SYSTEM',
  WORKSPACE: 'WORKSPACE',
  TEAM: 'TEAM',
  PROJECT: 'PROJECT',
}

export const ROLE_SCOPE_LABELS = {
  [ROLE_SCOPE.SYSTEM]: 'Hệ thống',
  [ROLE_SCOPE.WORKSPACE]: 'Workspace',
  [ROLE_SCOPE.TEAM]: 'Team',
  [ROLE_SCOPE.PROJECT]: 'Project',
}

export const ROLE_SCOPE_OPTIONS = Object.entries(ROLE_SCOPE_LABELS).map(
  ([value, label]) => ({ value, label }),
)
