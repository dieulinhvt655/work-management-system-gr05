import { Briefcase, FolderKanban, LayoutDashboard } from 'lucide-react'
import { PERMISSIONS } from '../permissions'

/** Team Leader sidebar — workspace info + project management trong team. */
export const TEAM_LEADER_NAV_ITEMS = [
  {
    id: 'team-dashboard',
    label: 'Team Dashboard',
    icon: LayoutDashboard,
    to: '/dashboard/team',
    permission: PERMISSIONS.DASHBOARD_TEAM_READ,
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
    id: 'project-management',
    label: 'Project Management',
    icon: FolderKanban,
    children: [
      {
        id: 'projects-create',
        label: 'Tạo dự án mới',
        to: '/projects/create',
      },
      {
        id: 'projects-list',
        label: 'Danh sách dự án',
        to: '/projects',
        permission: PERMISSIONS.PROJECT_READ,
      },
      {
        id: 'team-members',
        label: 'Thành viên của Team',
        to: '/projects/team-members',
        permission: PERMISSIONS.PROJECT_READ,
      },
    ],
  },
]

export const TEAM_LEADER_ROUTE_PERMISSIONS = {
  '/dashboard/team': PERMISSIONS.DASHBOARD_TEAM_READ,
  '/workspace/info': PERMISSIONS.WORKSPACE_READ,
  '/projects': PERMISSIONS.PROJECT_READ,
  '/projects/team-members': PERMISSIONS.PROJECT_READ,
}

export const TEAM_LEADER_DEFAULT_ROUTE_PRIORITY = [
  '/dashboard/team',
  '/projects',
  '/workspace/info',
]
