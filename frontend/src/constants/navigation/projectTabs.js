import { PERMISSIONS } from '../permissions'

export const PROJECT_TABS = [
  {
    id: 'overview',
    label: 'Tổng quan',
    path: 'overview',
    permission: PERMISSIONS.PROJECT_READ,
  },
  {
    id: 'members',
    label: 'Thành viên',
    path: 'members',
    permission: PERMISSIONS.PROJECT_READ,
  },
  {
    id: 'backlog',
    label: 'Product Backlog',
    path: 'backlog',
    permission: PERMISSIONS.BACKLOG_READ,
  },
  {
    id: 'docs',
    label: 'Tài liệu',
    path: 'docs',
    permission: PERMISSIONS.PROJECT_DOC_READ,
  },
  {
    id: 'activity',
    label: 'Lịch sử hoạt động',
    path: 'activity',
    permission: PERMISSIONS.PROJECT_ACTIVITY_READ,
  },
]

export function getProjectTabPermission(pathSegment) {
  return PROJECT_TABS.find((tab) => tab.path === pathSegment)?.permission
}

export function getDefaultProjectTabPath(can) {
  for (const tab of PROJECT_TABS) {
    if (can(tab.permission)) {
      return tab.path
    }
  }
  return null
}
