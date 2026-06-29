import {
  Briefcase,
  LayoutDashboard,
  Shield,
  UserCog,
  UserSquare2,
  Users,
} from 'lucide-react'
import { PERMISSIONS } from '../permissions'

/** Workspace Owner sidebar — scoped to managed workspace. */
export const WORKSPACE_OWNER_NAV_ITEMS = [
  {
    id: 'workspace-dashboard',
    label: 'Workspace Dashboard',
    icon: LayoutDashboard,
    to: '/dashboard/workspace',
    permission: PERMISSIONS.DASHBOARD_WORKSPACE_READ,
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
    ],
  },
  {
    id: 'teams',
    label: 'Teams',
    icon: Users,
    children: [
      {
        id: 'teams-list',
        label: 'Danh sách phòng ban',
        to: '/teams',
        permission: PERMISSIONS.TEAM_READ,
      },
      {
        id: 'teams-create',
        label: 'Tạo phòng ban',
        to: '/teams/create',
        permission: PERMISSIONS.TEAM_MANAGE,
      },
      {
        id: 'teams-assign-members',
        label: 'Phân công nhân viên',
        to: '/teams/assign-members',
        permission: PERMISSIONS.TEAM_MANAGE,
      },
    ],
  },
  {
    id: 'members',
    label: 'Members',
    icon: UserSquare2,
    to: '/members',
    permission: PERMISSIONS.MEMBER_READ,
  },
  {
    id: 'account-management',
    label: 'Account Management',
    icon: UserCog,
    children: [
      {
        id: 'accounts-list',
        label: 'Danh sách Account',
        to: '/admin/users',
        permission: PERMISSIONS.USER_READ,
      },
      {
        id: 'accounts-create',
        label: 'Tạo Account',
        to: '/admin/users/create',
        permission: PERMISSIONS.USER_MANAGE,
      },
      {
        id: 'accounts-status',
        label: 'Trạng thái Account',
        to: '/admin/users/status',
        permission: PERMISSIONS.USER_READ,
      },
    ],
  },
  {
    id: 'workspace-roles',
    label: 'Roles & Permissions',
    icon: Shield,
    to: '/workspace/roles',
    permission: PERMISSIONS.ROLE_MANAGE,
  },
]

export const WORKSPACE_OWNER_ROUTE_PERMISSIONS = {
  '/dashboard/workspace': PERMISSIONS.DASHBOARD_WORKSPACE_READ,
  '/workspace/info': PERMISSIONS.WORKSPACE_READ,
  '/workspace/roles': PERMISSIONS.ROLE_MANAGE,
  '/teams': PERMISSIONS.TEAM_READ,
  '/teams/create': PERMISSIONS.TEAM_MANAGE,
  '/teams/assign-members': PERMISSIONS.TEAM_MANAGE,
  '/members': PERMISSIONS.MEMBER_READ,
  '/admin/users': PERMISSIONS.USER_READ,
  '/admin/users/create': PERMISSIONS.USER_MANAGE,
  '/admin/users/status': PERMISSIONS.USER_READ,
}

export const WORKSPACE_OWNER_DEFAULT_ROUTE_PRIORITY = [
  '/dashboard/workspace',
  '/workspace/info',
  '/teams',
  '/members',
  '/admin/users',
  '/workspace/roles',
]
