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
