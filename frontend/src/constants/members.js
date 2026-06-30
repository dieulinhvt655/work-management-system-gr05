export const MEMBER_ORG_STATUS = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
}

export const MEMBER_ORG_STATUS_LABELS = {
  [MEMBER_ORG_STATUS.ACTIVE]: 'active',
  [MEMBER_ORG_STATUS.INACTIVE]: 'inactive (khóa)',
}

export const MEMBER_ORG_STATUS_OPTIONS = Object.entries(
  MEMBER_ORG_STATUS_LABELS,
).map(([value, label]) => ({ value, label }))
