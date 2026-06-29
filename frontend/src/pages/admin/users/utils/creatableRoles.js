/** Role names assignable when creating a system account (System Admin). */
export const SYSTEM_CREATABLE_ROLE_NAMES = [
  'System Admin',
  'Workspace Owner',
  'Workspace Member',
  'Team Leader',
  'Team Member',
  'Project Manager',
  'Project Contributor',
]

/** Workspace Owner may only assign workspace-scoped roles. */
export const WORKSPACE_OWNER_CREATABLE_ROLE_NAMES = [
  'Workspace Member',
  'Workspace Owner',
]

export function filterCreatableRoles(roles = [], { isWorkspaceOwner = false } = {}) {
  const allowedNames = isWorkspaceOwner
    ? WORKSPACE_OWNER_CREATABLE_ROLE_NAMES
    : SYSTEM_CREATABLE_ROLE_NAMES

  const allowed = new Set(allowedNames)

  return roles.filter((role) => allowed.has(role.name))
}
