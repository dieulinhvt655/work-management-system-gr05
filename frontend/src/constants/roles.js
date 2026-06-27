/** Mock login only — UI must not branch on role, use permissions instead. */
export const MOCK_ROLES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  WORKSPACE_OWNER: 'WORKSPACE_OWNER',
  TEAM_LEADER: 'TEAM_LEADER',
  PROJECT_MANAGER: 'PROJECT_MANAGER',
  TEAM_MEMBER: 'TEAM_MEMBER',
}

export const MOCK_ROLE_LABELS = {
  [MOCK_ROLES.SYSTEM_ADMIN]: 'System Admin',
  [MOCK_ROLES.WORKSPACE_OWNER]: 'Workspace Owner',
  [MOCK_ROLES.TEAM_LEADER]: 'Team Leader',
  [MOCK_ROLES.PROJECT_MANAGER]: 'Project Manager',
  [MOCK_ROLES.TEAM_MEMBER]: 'Team Member',
}
