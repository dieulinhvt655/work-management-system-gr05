export const USER_ACCOUNT_STATUS = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  DELETED: 'DELETED',
}

export const USER_STATUS_LABELS = {
  [USER_ACCOUNT_STATUS.ACTIVE]: 'Hoạt động',
  [USER_ACCOUNT_STATUS.INACTIVE]: 'Inactive (khóa)',
  [USER_ACCOUNT_STATUS.DELETED]: 'Đã xóa',
}

export const CREATE_USER_STATUS_OPTIONS = [
  { value: USER_ACCOUNT_STATUS.ACTIVE, label: 'Active' },
  { value: USER_ACCOUNT_STATUS.INACTIVE, label: 'Inactive' },
]

/** Default role when creating a new account. */
export const DEFAULT_CREATE_USER_ROLE = 'WORKSPACE_MEMBER'

/** Display labels for system role keys returned by API. */
export const USER_ROLE_LABELS = {
  SYSTEM_ADMIN: 'System Admin',
  WORKSPACE_OWNER: 'Workspace Owner',
  WORKSPACE_MEMBER: 'Workspace Member',
  TEAM_LEADER: 'Team Leader',
  PROJECT_MANAGER: 'Project Manager',
  TEAM_MEMBER: 'Project Contributor',
}

export const USER_ROLE_OPTIONS = Object.entries(USER_ROLE_LABELS).map(
  ([value, label]) => ({ value, label }),
)
