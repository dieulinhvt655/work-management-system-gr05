export const TEAM_STATUS = {
  ACTIVE: 'active',
  INACTIVE: 'inactive',
}

export const TEAM_STATUS_LABELS = {
  [TEAM_STATUS.ACTIVE]: 'Đang hoạt động',
  [TEAM_STATUS.INACTIVE]: 'Đã giải thể',
}

export const TEAM_TYPE = {
  DEPARTMENT: 'department',
  TEAM: 'team',
}

export const TEAM_TYPE_LABELS = {
  [TEAM_TYPE.DEPARTMENT]: 'Phòng ban',
  [TEAM_TYPE.TEAM]: 'Nhóm',
}

export const TEAM_TYPE_FILTER_LABELS = {
  [TEAM_TYPE.DEPARTMENT]: 'Phòng ban',
  [TEAM_TYPE.TEAM]: 'Nhóm làm việc',
}

export const LEADER_FILTER = {
  WITH: 'with_leader',
  WITHOUT: 'without_leader',
}

export const LEADER_FILTER_LABELS = {
  [LEADER_FILTER.WITH]: 'Có trưởng nhóm',
  [LEADER_FILTER.WITHOUT]: 'Chưa có trưởng nhóm',
}

export const CREATE_TEAM_STATUS_OPTIONS = [
  { value: TEAM_STATUS.ACTIVE, label: 'Hoạt động (Active)' },
  { value: TEAM_STATUS.INACTIVE, label: 'Không hoạt động (Inactive)' },
]
