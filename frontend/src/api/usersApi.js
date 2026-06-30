import {
  mapFrontendStatusToBackend,
  mapUserResponse,
} from '../utils/userMappers'
import { generateInitialPassword } from '../utils/generateInitialPassword'
import { unwrapApiResponse } from './apiResponse'
import api, { getAuthRequestConfig } from './axios'
import { fetchRoles, resolveRoleIdByKey } from './rolesApi'

const DEFAULT_USERS_GROUP = {
  workspaceId: 'system',
  workspaceName: 'Toàn hệ thống',
  workspaceCode: 'ALL',
}

function deriveUsername(email, fallback = 'user') {
  const localPart = email?.split('@')[0]?.trim()
  if (localPart && localPart.length >= 3) {
    return localPart.slice(0, 100)
  }
  return fallback.slice(0, 100)
}

async function fetchAllUsers(params = {}) {
  const size = 100
  let page = 0
  let totalPages = 1
  const items = []

  while (page < totalPages) {
    const { data } = await api.get(
      '/users',
      getAuthRequestConfig({
        params: {
          page,
          size,
          keyword: params.keyword?.trim() || undefined,
          status: params.status || undefined,
        },
      }),
    )
    const payload = unwrapApiResponse({ data })
    items.push(...(payload.items ?? []))
    totalPages = payload.totalPages ?? 1
    page += 1
  }

  return items
}

/** Lấy tất cả tài khoản đã tạo (paginate hết các trang). */
export async function fetchUsers(params = {}) {
  const items = await fetchAllUsers(params)
  return items.map(mapUserResponse)
}

/** Backend chưa có API phòng ban. */
export async function fetchDepartments() {
  return []
}

/**
 * @returns {Promise<Array<{
 *   workspaceId: string,
 *   workspaceName: string,
 *   workspaceCode?: string,
 *   users: Array
 * }>>}
 */
export async function fetchUsersGroupedByWorkspace() {
  const users = await fetchUsers()

  return [
    {
      ...DEFAULT_USERS_GROUP,
      users,
    },
  ]
}

export async function fetchUserById(userId) {
  const { data } = await api.get(
    `/users/${userId}`,
    getAuthRequestConfig(),
  )
  return mapUserResponse(unwrapApiResponse({ data }))
}

export async function updateUser(userId, payload) {
  const body = {
    fullName: payload.fullName,
    email: payload.email,
    phone: payload.phone?.trim() || null,
  }

  if (payload.username?.trim()) {
    body.username = payload.username.trim()
  }

  const { data } = await api.put(
    `/users/${userId}`,
    body,
    getAuthRequestConfig(),
  )
  return mapUserResponse(unwrapApiResponse({ data }))
}

export async function updateUserRole(userId, roleKeyOrId) {
  const roleId = await resolveRoleIdByKey(roleKeyOrId)
  const { data } = await api.patch(
    `/users/${userId}/role`,
    { roleId },
    getAuthRequestConfig(),
  )
  const payload = unwrapApiResponse({ data })

  if (payload?.role) {
    return mapUserResponse({ id: userId, ...payload, role: payload.role })
  }

  return fetchUserById(userId)
}

export async function updateUserStatus(userId, status) {
  const { data } = await api.patch(
    `/users/${userId}/status`,
    { status: mapFrontendStatusToBackend(status) },
    getAuthRequestConfig(),
  )
  return mapUserResponse(unwrapApiResponse({ data }))
}

export async function createUser(payload) {
  const roleId = payload.roleId ?? (await resolveRoleIdByKey(payload.role))
  const username =
    payload.username?.trim() || deriveUsername(payload.email)

  const body = {
    fullName: payload.fullName,
    email: payload.email,
    username,
    password: generateInitialPassword(username),
    phone: payload.phone?.trim() || null,
    roleId,
    status: mapFrontendStatusToBackend(payload.status ?? 'ACTIVE'),
  }

  // Workspace Owner: Tự động gán account vào workspace hiện tại
  if (payload.workspaceId) {
    body.workspaceId = payload.workspaceId
    body.workspaceRole = payload.role // Workspace role = role được chọn
  }

  const { data } = await api.post(
    '/users',
    body,
    getAuthRequestConfig(),
  )

  return mapUserResponse(unwrapApiResponse({ data }))
}

export { fetchRoles }
