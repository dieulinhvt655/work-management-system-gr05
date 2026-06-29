import { mapWorkspaceResponse } from '../utils/workspaceMappers'
import { unwrapApiResponse } from './apiResponse'
import api from './axios'
import { fetchUsers } from './usersApi'

async function fetchAllWorkspaces(params = {}) {
  const size = 100
  let page = 0
  let totalPages = 1
  const items = []

  while (page < totalPages) {
    const { data } = await api.get('/workspaces', {
      params: {
        page,
        size,
        keyword: params.keyword?.trim() || undefined,
        status: params.status || undefined,
      },
    })
    const payload = unwrapApiResponse({ data })
    items.push(...(payload.items ?? []))
    totalPages = payload.totalPages ?? 1
    page += 1
  }

  return items
}

async function fetchWorkspaceMemberCount(workspaceId) {
  try {
    const { data } = await api.get(`/workspaces/${workspaceId}/members`)
    const members = unwrapApiResponse({ data }) ?? []
    return members.length
  } catch {
    return 0
  }
}

export async function fetchWorkspaces(params = {}) {
  const items = await fetchAllWorkspaces(params)

  const workspaces = await Promise.all(
    items.map(async (item) => {
      const memberCount = await fetchWorkspaceMemberCount(item.id)
      return mapWorkspaceResponse(item, { memberCount })
    }),
  )

  return workspaces
}

export async function fetchWorkspaceById(workspaceId) {
  const { data } = await api.get(`/workspaces/${workspaceId}`)
  const workspace = unwrapApiResponse({ data })
  const memberCount = await fetchWorkspaceMemberCount(workspaceId)
  return mapWorkspaceResponse(workspace, { memberCount })
}

/** Users có vai trò Workspace Owner — dùng cho form tạo workspace. */
export async function fetchWorkspaceOwners() {
  const users = await fetchUsers()
  return users
    .filter(
      (user) =>
        user.roleName === 'Workspace Owner' || user.role === 'WORKSPACE_OWNER',
    )
    .map((user) => ({
      id: user.id,
      fullName: user.fullName,
      email: user.email,
    }))
}

export async function createWorkspace(payload) {
  const { data } = await api.post('/workspaces', {
    name: payload.name,
    description: payload.description?.trim() || undefined,
  })

  return mapWorkspaceResponse(unwrapApiResponse({ data }))
}

export async function updateWorkspace(workspaceId, payload) {
  const { data } = await api.put(`/workspaces/${workspaceId}`, {
    name: payload.name,
    description: payload.description?.trim() || undefined,
  })

  return mapWorkspaceResponse(unwrapApiResponse({ data }))
}

export async function updateWorkspaceStatus(workspaceId) {
  const { data } = await api.patch(`/workspaces/${workspaceId}/close`)
  return mapWorkspaceResponse(unwrapApiResponse({ data }))
}

export async function fetchWorkspaceActivityLogs(workspaceId, params = {}) {
  const { data } = await api.get(`/workspaces/${workspaceId}/activity-logs`, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 10,
    },
  })
  return unwrapApiResponse({ data })
}
