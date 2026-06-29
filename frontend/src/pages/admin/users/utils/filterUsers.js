export function filterUsers(users = [], filters) {
  const query = filters.search.trim().toLowerCase()

  return users.filter((user) => {
    if (query) {
      const matchesSearch =
        user.fullName?.toLowerCase().includes(query) ||
        user.email?.toLowerCase().includes(query) ||
        user.employeeCode?.toLowerCase().includes(query) ||
        user.username?.toLowerCase().includes(query)

      if (!matchesSearch) return false
    }

    if (filters.role && String(user.roleId) !== String(filters.role)) {
      return false
    }

    if (filters.departmentId && user.departmentId !== filters.departmentId) {
      return false
    }

    if (filters.status && user.status !== filters.status) {
      return false
    }

    return true
  })
}

export function selectWorkspaceGroup(groups = [], workspaceId) {
  if (!workspaceId) return null
  return groups.find((group) => group.workspaceId === workspaceId) ?? null
}

export function getFilteredWorkspaceGroup(groups = [], filters) {
  const group = selectWorkspaceGroup(groups, filters.workspaceId)
  if (!group) return null

  return {
    ...group,
    users: filterUsers(group.users ?? [], filters),
  }
}

export function hasActiveUserFilters(filters) {
  return Boolean(
    filters.search.trim() ||
      filters.role ||
      filters.departmentId ||
      filters.status,
  )
}
