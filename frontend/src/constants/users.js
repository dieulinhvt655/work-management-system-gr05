export const USER_ACCOUNT_STATUS = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  LOCKED: 'LOCKED',
  PENDING: 'PENDING',
}

export const USER_STATUS_LABELS = {
  [USER_ACCOUNT_STATUS.ACTIVE]: 'Hoạt động',
  [USER_ACCOUNT_STATUS.INACTIVE]: 'Không hoạt động',
  [USER_ACCOUNT_STATUS.LOCKED]: 'Đã khóa',
  [USER_ACCOUNT_STATUS.PENDING]: 'Chờ kích hoạt',
}

export const CREATE_USER_STATUS_OPTIONS = [
  { value: USER_ACCOUNT_STATUS.ACTIVE, label: 'Active' },
  { value: USER_ACCOUNT_STATUS.INACTIVE, label: 'Inactive' },
]

/** Display labels for system role keys returned by API. */
export const USER_ROLE_LABELS = {
  SYSTEM_ADMIN: 'System Admin',
  WORKSPACE_OWNER: 'Workspace Owner',
  TEAM_LEADER: 'Team Leader',
  PROJECT_MANAGER: 'Project Manager',
  TEAM_MEMBER: 'Team Member',
}

export const USER_ROLE_OPTIONS = Object.entries(USER_ROLE_LABELS).map(
  ([value, label]) => ({ value, label }),
)
