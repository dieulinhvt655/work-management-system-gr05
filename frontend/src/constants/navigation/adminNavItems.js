import {
  Briefcase,
  LayoutDashboard,
  ScrollText,
  Settings,
  Shield,
  UserCog,
} from 'lucide-react'
import { PERMISSIONS } from '../permissions'

/** Admin module navigation — System Admin sidebar structure. */
export const ADMIN_NAV_ITEMS = [
  {
    id: 'admin-dashboard',
    label: 'Admin Dashboard',
    icon: LayoutDashboard,
    to: '/admin/dashboard',
    permission: PERMISSIONS.USER_READ,
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
    id: 'workspace-management',
    label: 'Workspace Management',
    icon: Briefcase,
    children: [
      {
        id: 'workspaces-list',
        label: 'Workspaces',
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
        id: 'workspaces-assign-accounts',
        label: 'Add Accounts to Workspace',
        to: '/admin/workspaces/assign-accounts',
        permission: PERMISSIONS.WORKSPACE_ADMIN_MANAGE,
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
    id: 'roles',
    label: 'Roles & Permissions',
    icon: Shield,
    to: '/roles',
    permission: PERMISSIONS.ROLE_MANAGE,
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
]

export const ADMIN_ROUTE_PERMISSIONS = {
  '/admin/dashboard': PERMISSIONS.USER_READ,
  '/admin/users': PERMISSIONS.USER_READ,
  '/admin/users/create': PERMISSIONS.USER_MANAGE,
  '/admin/users/status': PERMISSIONS.USER_READ,
  '/admin/workspaces': PERMISSIONS.WORKSPACE_ADMIN_READ,
  '/admin/workspaces/create': PERMISSIONS.WORKSPACE_ADMIN_CREATE,
  '/admin/workspaces/assign-accounts': PERMISSIONS.WORKSPACE_ADMIN_MANAGE,
  '/admin/workspaces/activity': PERMISSIONS.WORKSPACE_ADMIN_ACTIVITY_READ,
  '/roles': PERMISSIONS.ROLE_MANAGE,
  '/admin/audit/system': PERMISSIONS.AUDIT_READ,
  '/admin/audit/security': PERMISSIONS.AUDIT_READ,
  '/admin/settings': PERMISSIONS.SETTINGS_MANAGE,
}

export const ADMIN_DEFAULT_ROUTE_PRIORITY = [
  '/admin/dashboard',
  '/admin/users',
  '/admin/workspaces',
  '/roles',
  '/admin/settings',
]
