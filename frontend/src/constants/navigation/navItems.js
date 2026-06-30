import {
  Briefcase,
  ClipboardList,
  FolderKanban,
  Users,
} from 'lucide-react'
import { PERMISSIONS } from '../permissions'
import {
  ADMIN_DEFAULT_ROUTE_PRIORITY,
  ADMIN_NAV_ITEMS,
  ADMIN_ROUTE_PERMISSIONS,
} from './adminNavItems'
import {
  WORKSPACE_OWNER_DEFAULT_ROUTE_PRIORITY,
  WORKSPACE_OWNER_NAV_ITEMS,
  WORKSPACE_OWNER_ROUTE_PERMISSIONS,
} from './workspaceOwnerNavItems'
import {
  TEAM_LEADER_DEFAULT_ROUTE_PRIORITY,
  TEAM_LEADER_NAV_ITEMS,
  TEAM_LEADER_ROUTE_PERMISSIONS,
} from './teamLeaderNavItems'
import { isTeamLeaderUser, isWorkspaceOwnerUser } from '../../utils/userRoleUtils'

/** Pick sidebar items by session role scope. */
export function getNavItemsForUser(user) {
  if (user?.isSystemAdmin) {
    return ADMIN_NAV_ITEMS
  }

  if (isWorkspaceOwnerUser(user)) {
    return WORKSPACE_OWNER_NAV_ITEMS
  }

  if (isTeamLeaderUser(user)) {
    return TEAM_LEADER_NAV_ITEMS
  }

  return OPERATIONAL_NAV_ITEMS
}

export function getRoutePermissionsForUser(user) {
  if (user?.isSystemAdmin) {
    return ROUTE_PERMISSIONS_BASE
  }

  if (isWorkspaceOwnerUser(user)) {
    return {
      ...WORKSPACE_OWNER_ROUTE_PERMISSIONS,
      '/profile': PERMISSIONS.PROFILE_READ,
    }
  }

  if (isTeamLeaderUser(user)) {
    return {
      ...TEAM_LEADER_ROUTE_PERMISSIONS,
      '/profile': PERMISSIONS.PROFILE_READ,
    }
  }

  return ROUTE_PERMISSIONS_BASE
}

export function getDefaultRoutePriorityForUser(user) {
  if (user?.isSystemAdmin) {
    return DEFAULT_ROUTE_PRIORITY_BASE
  }

  if (isWorkspaceOwnerUser(user)) {
    return WORKSPACE_OWNER_DEFAULT_ROUTE_PRIORITY
  }

  if (isTeamLeaderUser(user)) {
    return TEAM_LEADER_DEFAULT_ROUTE_PRIORITY
  }

  return DEFAULT_ROUTE_PRIORITY_BASE.filter((path) => !path.startsWith('/admin'))
}

/** Operational navigation (non-admin modules). */
const OPERATIONAL_NAV_ITEMS = [
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
    ],
  },
  {
    id: 'project-management',
    label: 'Project Management',
    icon: FolderKanban,
    children: [
      {
        id: 'projects-create',
        label: 'Create Project',
        to: '/projects/create',
        permission: PERMISSIONS.PROJECT_CREATE,
      },
      {
        id: 'projects-list',
        label: 'Projects',
        to: '/projects',
        permission: PERMISSIONS.PROJECT_READ,
      },
      {
        id: 'projects-activity',
        label: 'Project Activity',
        to: '/projects/activity',
        permission: PERMISSIONS.PROJECTS_ACTIVITY_READ,
      },
    ],
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
        id: 'my-work-activity',
        label: 'My Activity',
        to: '/my-work/activity',
        permission: PERMISSIONS.MYWORK_READ,
      },
    ],
  },
  {
    id: 'my-team',
    label: 'My Team',
    icon: Users,
    children: [
      {
        id: 'my-team-members-list',
        label: 'Team Members',
        to: '/my-team/members',
        permission: PERMISSIONS.MY_TEAM_MEMBERS_READ,
      },
    ],
  },
]

/** Full sidebar — admin module first, then operational items (legacy export). */
export const NAV_ITEMS = [...ADMIN_NAV_ITEMS, ...OPERATIONAL_NAV_ITEMS]

/** Route → permission map for PermissionRoute guards. */
export const ROUTE_PERMISSIONS = {
  ...ADMIN_ROUTE_PERMISSIONS,
  ...WORKSPACE_OWNER_ROUTE_PERMISSIONS,
  '/dashboard/team': PERMISSIONS.DASHBOARD_TEAM_READ,
  '/dashboard/workspace': PERMISSIONS.DASHBOARD_WORKSPACE_READ,
  '/dashboard/my': PERMISSIONS.DASHBOARD_MY_READ,
  '/workspace/info': PERMISSIONS.WORKSPACE_READ,
  '/workspace/roles': PERMISSIONS.ROLE_MANAGE,
  '/workspace/settings': PERMISSIONS.WORKSPACE_MANAGE,
  '/workspace/activity': PERMISSIONS.WORKSPACE_ACTIVITY_READ,
  '/teams': PERMISSIONS.TEAM_READ,
  '/teams/create': PERMISSIONS.TEAM_MANAGE,
  '/teams/assign-members': PERMISSIONS.TEAM_MANAGE,
  '/teams/departments': PERMISSIONS.TEAM_READ,
  '/teams/assignments': PERMISSIONS.TEAM_MANAGE,
  '/my-team': PERMISSIONS.MY_TEAM_READ,
  '/my-team/members': PERMISSIONS.MY_TEAM_MEMBERS_READ,
  '/members': PERMISSIONS.MEMBER_READ,
  '/members/invite': PERMISSIONS.MEMBER_MANAGE,
  '/members/permissions': PERMISSIONS.MEMBER_MANAGE,
  '/projects': PERMISSIONS.PROJECT_READ,
  '/projects/create': PERMISSIONS.PROJECT_CREATE,
  '/projects/activity': PERMISSIONS.PROJECTS_ACTIVITY_READ,
  '/my-work/tasks': PERMISSIONS.MYWORK_READ,
  '/my-work/activity': PERMISSIONS.MYWORK_READ,
  '/my-work/assigned': PERMISSIONS.MYWORK_READ,
  '/profile': PERMISSIONS.PROFILE_READ,
}

const ROUTE_PERMISSIONS_BASE = ROUTE_PERMISSIONS

export const DEFAULT_ROUTE_PRIORITY = [
  ...ADMIN_DEFAULT_ROUTE_PRIORITY,
  '/dashboard/workspace',
  '/dashboard/my',
  '/projects',
  '/my-work/tasks',
  '/workspace/info',
]

const DEFAULT_ROUTE_PRIORITY_BASE = DEFAULT_ROUTE_PRIORITY
