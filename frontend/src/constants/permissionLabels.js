import { PERMISSIONS } from './permissions'

export const PERMISSION_LABELS = {
  [PERMISSIONS.DASHBOARD_WORKSPACE_READ]: 'Xem dashboard workspace',
  [PERMISSIONS.DASHBOARD_TEAM_READ]: 'Xem dashboard team',
  [PERMISSIONS.DASHBOARD_PROJECT_READ]: 'Xem dashboard dự án',
  [PERMISSIONS.DASHBOARD_MY_READ]: 'Xem dashboard cá nhân',
  [PERMISSIONS.WORKSPACE_ADMIN_READ]: 'Xem workspace (hệ thống)',
  [PERMISSIONS.WORKSPACE_ADMIN_CREATE]: 'Tạo workspace',
  [PERMISSIONS.WORKSPACE_ADMIN_MANAGE]: 'Quản lý workspace (hệ thống)',
  [PERMISSIONS.WORKSPACE_ADMIN_ACTIVITY_READ]: 'Xem hoạt động workspace (hệ thống)',
  [PERMISSIONS.WORKSPACE_READ]: 'Xem workspace',
  [PERMISSIONS.WORKSPACE_MANAGE]: 'Quản lý workspace',
  [PERMISSIONS.WORKSPACE_ACTIVITY_READ]: 'Xem hoạt động workspace',
  [PERMISSIONS.USER_READ]: 'Xem người dùng',
  [PERMISSIONS.USER_MANAGE]: 'Quản lý người dùng',
  [PERMISSIONS.TEAM_READ]: 'Xem team',
  [PERMISSIONS.TEAM_MANAGE]: 'Quản lý team',
  [PERMISSIONS.MEMBER_READ]: 'Xem thành viên',
  [PERMISSIONS.MEMBER_MANAGE]: 'Quản lý thành viên',
  [PERMISSIONS.PROJECT_READ]: 'Xem dự án',
  [PERMISSIONS.PROJECT_CREATE]: 'Tạo dự án',
  [PERMISSIONS.PROJECT_MANAGE]: 'Quản lý dự án',
  [PERMISSIONS.MYWORK_READ]: 'Xem công việc của tôi',
  [PERMISSIONS.BACKLOG_READ]: 'Xem backlog',
  [PERMISSIONS.BACKLOG_MANAGE]: 'Quản lý backlog',
  [PERMISSIONS.SPRINT_READ]: 'Xem sprint',
  [PERMISSIONS.SPRINT_MANAGE]: 'Quản lý sprint',
  [PERMISSIONS.PROJECT_MEMBER_MANAGE]: 'Quản lý thành viên dự án',
  [PERMISSIONS.PROJECT_DOC_READ]: 'Xem tài liệu dự án',
  [PERMISSIONS.PROJECT_ACTIVITY_READ]: 'Xem hoạt động dự án',
  [PERMISSIONS.ROLE_MANAGE]: 'Quản lý vai trò',
  [PERMISSIONS.AUDIT_READ]: 'Xem audit log',
  [PERMISSIONS.SETTINGS_MANAGE]: 'Quản lý cài đặt',
  [PERMISSIONS.PROFILE_READ]: 'Xem hồ sơ',
}

export function getPermissionLabel(permission) {
  return PERMISSION_LABELS[permission] ?? permission
}
