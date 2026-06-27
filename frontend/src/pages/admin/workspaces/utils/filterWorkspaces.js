const FILTER_ALL = ''

export function filterWorkspaces(workspaces, filters) {
  const search = filters.search.trim().toLowerCase()

  return workspaces.filter((workspace) => {
    if (filters.status && filters.status !== FILTER_ALL) {
      if (workspace.status !== filters.status) return false
    }

    if (filters.ownerId && filters.ownerId !== FILTER_ALL) {
      if (workspace.ownerId !== filters.ownerId) return false
    }

    if (filters.createdDate) {
      const createdDate = workspace.createdAt?.slice(0, 10)
      if (createdDate !== filters.createdDate) return false
    }

    if (!search) return true

    const haystack = [
      workspace.name,
      workspace.code,
      workspace.ownerName,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    return haystack.includes(search)
  })
}

export function hasActiveWorkspaceFilters(filters) {
  return Boolean(
    filters.search.trim() ||
      (filters.status && filters.status !== FILTER_ALL) ||
      (filters.ownerId && filters.ownerId !== FILTER_ALL) ||
      filters.createdDate,
  )
}

export { FILTER_ALL as WORKSPACE_FILTER_ALL }
