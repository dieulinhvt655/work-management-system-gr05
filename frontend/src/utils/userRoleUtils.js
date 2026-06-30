export function getUserRoleName(user) {
  if (!user?.role) return ''
  if (typeof user.role === 'string') return user.role.trim()
  return String(user.role.name ?? '').trim()
}

export function isWorkspaceOwnerUser(user) {
  if (!user || user.isSystemAdmin) return false
  if (user.isWorkspaceOwner === true) return true

  const roleName = getUserRoleName(user).toLowerCase()
  const roleScope = String(user.roleScope ?? user.role?.scope ?? '').toUpperCase()

  return (
    roleName === 'workspace owner' ||
    (roleScope === 'WORKSPACE' && roleName.includes('owner'))
  )
}

export function isSystemAdminUser(user) {
  if (!user) return false
  if (user.isSystemAdmin === true) return true

  const roleName = getUserRoleName(user).toLowerCase()
  const roleScope = String(user.roleScope ?? user.role?.scope ?? '').toUpperCase()

  return roleName === 'system admin' || roleScope === 'SYSTEM'
}
