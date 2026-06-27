import { USE_MOCK_AUTH } from '../constants/config'
import api from './axios'
import {
  mockCreateUser,
  mockFetchDepartments,
  mockFetchUserById,
  mockFetchUsersGroupedByWorkspace,
  mockUpdateUser,
  mockUpdateUserRole,
  mockUpdateUserStatus,
} from './mockUsersStore'

function unwrap(response) {
  return response.data?.data ?? response.data
}

/** @returns {Promise<Array<{ id: string, name: string }>>} */
export async function fetchDepartments() {
  if (USE_MOCK_AUTH) {
    return mockFetchDepartments()
  }

  try {
    const { data } = await api.get('/admin/departments')
    return unwrap({ data }) ?? []
  } catch {
    return []
  }
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
  if (USE_MOCK_AUTH) {
    return mockFetchUsersGroupedByWorkspace()
  }

  try {
    const { data } = await api.get('/admin/users/grouped-by-workspace')
    return unwrap({ data }) ?? []
  } catch {
    return []
  }
}

export async function fetchUserById(userId) {
  if (USE_MOCK_AUTH) {
    return mockFetchUserById(userId)
  }

  const { data } = await api.get(`/admin/users/${userId}`)
  return unwrap({ data })
}

export async function updateUser(userId, payload) {
  if (USE_MOCK_AUTH) {
    return mockUpdateUser(userId, payload)
  }

  const { data } = await api.put(`/admin/users/${userId}`, payload)
  return unwrap({ data })
}

export async function updateUserRole(userId, role) {
  if (USE_MOCK_AUTH) {
    return mockUpdateUserRole(userId, role)
  }

  const { data } = await api.patch(`/admin/users/${userId}/role`, { role })
  return unwrap({ data })
}

export async function updateUserStatus(userId, status) {
  if (USE_MOCK_AUTH) {
    return mockUpdateUserStatus(userId, status)
  }

  const { data } = await api.patch(`/admin/users/${userId}/status`, { status })
  return unwrap({ data })
}

export async function createUser(payload) {
  if (USE_MOCK_AUTH) {
    return mockCreateUser(payload)
  }

  const { data } = await api.post('/admin/users', payload)
  return unwrap({ data })
}
