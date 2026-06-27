import {
  Briefcase,
  ClipboardList,
  FolderKanban,
  LayoutDashboard,
  ScrollText,
  Settings,
  Shield,
  User,
  UserCog,
  Users,
} from 'lucide-react'
import { PERMISSIONS } from '../permissions'

/** Navigation config — visibility driven by permission, not role. */
export const NAV_ITEMS = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: LayoutDashboard,
    children: [
      {
        id: 'dashboard-workspace',
        label: 'Workspace Dashboard',
        to: '/dashboard/workspace',
        permission: PERMISSIONS.DASHBOARD_WORKSPACE_READ,
      },
      {
        id: 'dashboard-team',
        label: 'Team Dashboard',
        to: '/dashboard/team',
        permission: PERMISSIONS.DASHBOARD_TEAM_READ,
      },
      {
        id: 'dashboard-project',
        label: 'Project Dashboard',
        to: '/dashboard/project',
        permission: PERMISSIONS.DASHBOARD_PROJECT_READ,
      },
      {
        id: 'dashboard-my',
        label: 'My Dashboard',
        to: '/dashboard/my',
        permission: PERMISSIONS.DASHBOARD_MY_READ,
      },
    ],
  },
  {
    id: 'workspaces',
    label: 'Workspaces',
    icon: Briefcase,
    children: [
      {
        id: 'workspaces-list',
        label: 'Danh sách Workspace',
        to: '/admin/workspaces',
        permission: PERMISSIONS.WORKSPACE_ADMIN_READ,
      },
      {
        id: 'workspaces-create',
        label: 'Tạo Workspace',
        to: '/admin/workspaces/create',
        permission: PERMISSIONS.WORKSPACE_ADMIN_CREATE,
      },
      {
        id: 'workspaces-activity',
        label: 'Workspace Activity',
        to: '/admin/workspaces/activity',
        permission: PERMISSIONS.WORKSPACE_ADMIN_ACTIVITY_READ,
      },
    ],
  },
  {
    id: 'workspace',
    label: 'Workspace',
    icon: Briefcase,
    children: [
      {
        id: 'workspace-info',
        label: 'Thông tin Workspace',
        to: '/workspace/info',
        permission: PERMISSIONS.WORKSPACE_READ,
      },
      {
        id: 'workspace-settings',
        label: 'Workspace Settings',
        to: '/workspace/settings',
        permission: PERMISSIONS.WORKSPACE_MANAGE,
      },
      {
        id: 'workspace-activity',
        label: 'Workspace Activity',
        to: '/workspace/activity',
        permission: PERMISSIONS.WORKSPACE_ACTIVITY_READ,
      },
    ],
  },
  {
    id: 'users',
    label: 'User Management',
    icon: UserCog,
    children: [
      {
        id: 'users-list',
        label: 'Danh sách người dùng',
        to: '/admin/users',
        permission: PERMISSIONS.USER_READ,
      },
      {
        id: 'users-create',
        label: 'Tạo tài khoản',
        to: '/admin/users/create',
        permission: PERMISSIONS.USER_MANAGE,
      },
      {
        id: 'users-status',
        label: 'Trạng thái tài khoản',
        to: '/admin/users/status',
        permission: PERMISSIONS.USER_MANAGE,
      },
    ],
  },
  {
    id: 'teams',
    label: 'Teams & Departments',
    icon: Users,
    children: [
      {
        id: 'teams-list',
        label: 'Danh sách Team',
        to: '/teams',
        permission: PERMISSIONS.TEAM_READ,
      },
      {
        id: 'teams-departments',
        label: 'Danh sách Department',
        to: '/teams/departments',
        permission: PERMISSIONS.TEAM_READ,
      },
      {
        id: 'teams-assign',
        label: 'Phân công thành viên',
        to: '/teams/assignments',
        permission: PERMISSIONS.TEAM_MANAGE,
      },
    ],
  },
  {
    id: 'members',
    label: 'Workspace Members',
    icon: Users,
    children: [
      {
        id: 'members-list',
        label: 'Danh sách thành viên',
        to: '/members',
        permission: PERMISSIONS.MEMBER_READ,
      },
      {
        id: 'members-invite',
        label: 'Mời thành viên',
        to: '/members/invite',
        permission: PERMISSIONS.MEMBER_MANAGE,
      },
      {
        id: 'members-permissions',
        label: 'Phân quyền thành viên',
        to: '/members/permissions',
        permission: PERMISSIONS.MEMBER_MANAGE,
      },
    ],
  },
  {
    id: 'roles',
    label: 'Roles & Permissions',
    icon: Shield,
    to: '/roles',
    permission: PERMISSIONS.ROLE_MANAGE,
  },
  {
    id: 'projects',
    label: 'Projects',
    icon: FolderKanban,
    to: '/projects',
    permission: PERMISSIONS.PROJECT_READ,
  },
  {
    id: 'my-work',
    label: 'My Work',
    icon: ClipboardList,
    children: [
      {
        id: 'my-work-tasks',
        label: 'My Tasks',
        to: '/my-work/tasks',
        permission: PERMISSIONS.MYWORK_READ,
      },
      {
        id: 'my-work-assigned',
        label: 'Assigned Tasks',
        to: '/my-work/assigned',
        permission: PERMISSIONS.MYWORK_READ,
      },
      {
        id: 'my-work-activity',
        label: 'My Activity',
        to: '/my-work/activity',
        permission: PERMISSIONS.MYWORK_READ,
      },
    ],
  },
  {
    id: 'audit',
    label: 'Audit Logs',
    icon: ScrollText,
    children: [
      {
        id: 'audit-system',
        label: 'System Logs',
        to: '/admin/audit/system',
        permission: PERMISSIONS.AUDIT_READ,
      },
      {
        id: 'audit-security',
        label: 'Security Logs',
        to: '/admin/audit/security',
        permission: PERMISSIONS.AUDIT_READ,
      },
    ],
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: Settings,
    to: '/admin/settings',
    permission: PERMISSIONS.SETTINGS_MANAGE,
  },
  {
    id: 'profile',
    label: 'Hồ sơ cá nhân',
    icon: User,
    to: '/profile',
    permission: PERMISSIONS.PROFILE_READ,
  },
]

/** Route → permission map for PermissionRoute guards. */
export const ROUTE_PERMISSIONS = {
  '/dashboard/workspace': PERMISSIONS.DASHBOARD_WORKSPACE_READ,
  '/dashboard/team': PERMISSIONS.DASHBOARD_TEAM_READ,
  '/dashboard/project': PERMISSIONS.DASHBOARD_PROJECT_READ,
  '/dashboard/my': PERMISSIONS.DASHBOARD_MY_READ,
  '/admin/workspaces': PERMISSIONS.WORKSPACE_ADMIN_READ,
  '/admin/workspaces/create': PERMISSIONS.WORKSPACE_ADMIN_CREATE,
  '/admin/workspaces/activity': PERMISSIONS.WORKSPACE_ADMIN_ACTIVITY_READ,
  '/workspace/info': PERMISSIONS.WORKSPACE_READ,
  '/workspace/settings': PERMISSIONS.WORKSPACE_MANAGE,
  '/workspace/activity': PERMISSIONS.WORKSPACE_ACTIVITY_READ,
  '/admin/users': PERMISSIONS.USER_READ,
  '/admin/users/create': PERMISSIONS.USER_MANAGE,
  '/admin/users/status': PERMISSIONS.USER_MANAGE,
  '/teams': PERMISSIONS.TEAM_READ,
  '/teams/departments': PERMISSIONS.TEAM_READ,
  '/teams/assignments': PERMISSIONS.TEAM_MANAGE,
  '/members': PERMISSIONS.MEMBER_READ,
  '/members/invite': PERMISSIONS.MEMBER_MANAGE,
  '/members/permissions': PERMISSIONS.MEMBER_MANAGE,
  '/roles': PERMISSIONS.ROLE_MANAGE,
  '/projects': PERMISSIONS.PROJECT_READ,
  '/my-work/tasks': PERMISSIONS.MYWORK_READ,
  '/my-work/assigned': PERMISSIONS.MYWORK_READ,
  '/my-work/activity': PERMISSIONS.MYWORK_READ,
  '/admin/audit/system': PERMISSIONS.AUDIT_READ,
  '/admin/audit/security': PERMISSIONS.AUDIT_READ,
  '/admin/settings': PERMISSIONS.SETTINGS_MANAGE,
  '/profile': PERMISSIONS.PROFILE_READ,
}

export const DEFAULT_ROUTE_PRIORITY = [
  '/dashboard/my',
  '/dashboard/project',
  '/dashboard/team',
  '/dashboard/workspace',
  '/projects',
  '/my-work/tasks',
  '/workspace/info',
  '/admin/workspaces',
  '/profile',
]
