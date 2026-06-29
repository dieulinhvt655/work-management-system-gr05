/** Map backend PermissionResponse → frontend permission model. */
export function mapPermissionResponse(permission) {
  if (!permission) return null

  return {
    id: Number(permission.id),
    code: permission.code,
    name: permission.name,
    module: permission.module ?? 'other',
    description: permission.description ?? '',
  }
}

/** Map backend RoleResponse → frontend role model. */
export function mapRoleResponse(role) {
  if (!role) return null

  const permissions = (role.permissions ?? [])
    .map(mapPermissionResponse)
    .filter(Boolean)

  return {
    id: Number(role.id),
    name: role.name,
    description: role.description ?? '',
    scope: role.scope ?? 'SYSTEM',
    permissions,
    permissionIds: permissions.map((permission) => permission.id),
  }
}

/** Group permissions by module for checkbox UI. */
export function groupPermissionsByModule(permissions = []) {
  const groups = new Map()

  for (const permission of permissions) {
    const moduleKey = permission.module || 'other'
    if (!groups.has(moduleKey)) {
      groups.set(moduleKey, [])
    }
    groups.get(moduleKey).push(permission)
  }

  return [...groups.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([module, items]) => ({
      module,
      permissions: items.sort((a, b) => a.name.localeCompare(b.name)),
    }))
}
