import { mapPermissionResponse } from '../utils/roleMappers'
import { unwrapApiResponse } from './apiResponse'
import api, { getAuthRequestConfig } from './axios'

/** @returns {Promise<Array<{ id: number, code: string, name: string, module: string, description: string }>>} */
export async function fetchPermissions() {
  const { data } = await api.get('/permissions', getAuthRequestConfig())
  const items = unwrapApiResponse({ data }) ?? []
  return items.map(mapPermissionResponse)
}

export async function fetchPermissionById(permissionId) {
  const { data } = await api.get(
    `/permissions/${permissionId}`,
    getAuthRequestConfig(),
  )
  return mapPermissionResponse(unwrapApiResponse({ data }))
}

export async function createPermission(payload) {
  const { data } = await api.post(
    '/permissions',
    {
      code: payload.code,
      name: payload.name,
      module: payload.module,
      description: payload.description?.trim() || undefined,
    },
    getAuthRequestConfig(),
  )

  return mapPermissionResponse(unwrapApiResponse({ data }))
}

export async function updatePermission(permissionId, payload) {
  const { data } = await api.put(
    `/permissions/${permissionId}`,
    {
      name: payload.name,
      module: payload.module,
      description: payload.description?.trim() || undefined,
    },
    getAuthRequestConfig(),
  )

  return mapPermissionResponse(unwrapApiResponse({ data }))
}

export async function deletePermission(permissionId) {
  await api.delete(`/permissions/${permissionId}`, getAuthRequestConfig())
}
