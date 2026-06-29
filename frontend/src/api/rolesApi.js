import { AUTH_USER_KEY } from '../constants/auth'
import { USER_ROLE_LABELS } from '../constants/users'
import { mapRoleResponse } from '../utils/roleMappers'
import { unwrapApiResponse } from './apiResponse'
import api, { getAuthRequestConfig } from './axios'

let rolesCache = null

export function clearRolesCache() {
  rolesCache = null
}

function normalizeRolesPayload(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.items)) return payload.items
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

function shouldFallbackRolesFetch(error) {
  const status = error?.response?.status
  return (
    status === 401 ||
    status === 403 ||
    status === 404 ||
    error?.code === 'ECONNABORTED' ||
    error?.code === 'ERR_NETWORK'
  )
}

/** Build role list from existing users when GET /roles is unavailable. */
async function deriveRolesFromUsersDirectory() {
  const roleByName = new Map()

  const addRole = (name, id, scope = 'WORKSPACE') => {
    if (!name || id == null) return
    roleByName.set(name, {
      id: Number(id),
      name,
      description: '',
      scope: scope ?? 'WORKSPACE',
      permissions: [],
      permissionIds: [],
    })
  }

  try {
    const stored = localStorage.getItem(AUTH_USER_KEY)
    if (stored) {
      const sessionUser = JSON.parse(stored)
      const roleName =
        typeof sessionUser.role === 'string'
          ? sessionUser.role
          : sessionUser.role?.name
      addRole(roleName, sessionUser.roleId, sessionUser.roleScope)
    }
  } catch {
    // Ignore invalid session cache.
  }

  const users = await import('./usersApi').then(({ fetchUsers }) => fetchUsers())
  for (const user of users) {
    addRole(user.roleName, user.roleId, user.roleScope)
  }

  return [...roleByName.values()]
}

function buildRolePayload(payload) {
  return {
    name: payload.name?.trim(),
    description: payload.description?.trim() || undefined,
    scope: payload.scope,
    permissionIds: payload.permissionIds?.map(Number) ?? [],
  }
}

/** @returns {Promise<Array<import('../utils/roleMappers').mapRoleResponse>>} */
export async function fetchRoles({ force = false, timeout } = {}) {
  if (rolesCache && !force) {
    return rolesCache
  }

  try {
    const { data } = await api.get(
      '/roles',
      getAuthRequestConfig(timeout ? { timeout } : {}),
    )
    rolesCache = normalizeRolesPayload(unwrapApiResponse({ data }))
      .map(mapRoleResponse)
      .filter(Boolean)

    if (rolesCache.length === 0) {
      const derived = await deriveRolesFromUsersDirectory()
      if (derived.length > 0) {
        rolesCache = derived
      }
    }

    return rolesCache
  } catch (error) {
    if (!shouldFallbackRolesFetch(error)) {
      throw error
    }

    const derived = await deriveRolesFromUsersDirectory()
    if (derived.length === 0) {
      throw error
    }

    rolesCache = derived
    return rolesCache
  }
}

/** Roles for account creation — shorter timeout + user-directory fallback. */
export async function fetchCreatableRolesForAccountCreation({
  force = false,
} = {}) {
  return fetchRoles({ force, timeout: 15_000 })
}

export async function fetchRoleById(roleId) {
  const { data } = await api.get(`/roles/${roleId}`, getAuthRequestConfig())
  return mapRoleResponse(unwrapApiResponse({ data }))
}

export async function createRole(payload) {
  const { data } = await api.post(
    '/roles',
    buildRolePayload(payload),
    getAuthRequestConfig(),
  )
  clearRolesCache()
  return mapRoleResponse(unwrapApiResponse({ data }))
}

export async function updateRole(roleId, payload) {
  const { data } = await api.put(
    `/roles/${roleId}`,
    buildRolePayload(payload),
    getAuthRequestConfig(),
  )
  clearRolesCache()
  return mapRoleResponse(unwrapApiResponse({ data }))
}

export async function deleteRole(roleId) {
  await api.delete(`/roles/${roleId}`, getAuthRequestConfig())
  clearRolesCache()
}

export async function assignRolePermissions(roleId, permissionIds) {
  const { data } = await api.put(
    `/roles/${roleId}/permissions`,
    { permissionIds: permissionIds.map(Number) },
    getAuthRequestConfig(),
  )
  clearRolesCache()
  return mapRoleResponse(unwrapApiResponse({ data }))
}

/** Resolve frontend role key, role name, or numeric id → backend role id. */
export async function resolveRoleIdByKey(roleKeyOrId) {
  const raw = String(roleKeyOrId ?? '').trim()

  if (!raw) {
    throw new Error('Vai trò không hợp lệ.')
  }

  if (/^\d+$/.test(raw)) {
    return Number(raw)
  }

  const roleName = USER_ROLE_LABELS[roleKeyOrId] ?? roleKeyOrId
  const roles = await fetchRoles()

  const aliases = new Set([roleName, roleKeyOrId])
  if (roleKeyOrId === 'Team Member' || roleKeyOrId === 'TEAM_MEMBER') {
    aliases.add('Team Member')
    aliases.add('Project Contributor')
  }
  if (roleKeyOrId === 'TEAM_LEADER' || roleKeyOrId === 'Team Leader') {
    aliases.add('Team Leader')
  }

  const role = roles.find((item) => aliases.has(item.name))

  if (!role) {
    throw new Error(
      `Không tìm thấy vai trò "${[...aliases].join('" hoặc "')}" trên hệ thống.`,
    )
  }

  return role.id
}
