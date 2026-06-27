import { USE_MOCK_AUTH } from '../constants/config'
import api from './axios'
import {
  mockCreateWorkspace,
  mockFetchWorkspaceById,
  mockFetchWorkspaceOwners,
  mockFetchWorkspaces,
  mockUpdateWorkspace,
  mockUpdateWorkspaceStatus,
} from './mockWorkspacesStore'

function unwrap(response) {
  return response.data?.data ?? response.data
}

export async function fetchWorkspaces() {
  if (USE_MOCK_AUTH) {
    return mockFetchWorkspaces()
  }

  try {
    const { data } = await api.get('/admin/workspaces')
    return unwrap({ data }) ?? []
  } catch {
    return []
  }
}

export async function fetchWorkspaceById(workspaceId) {
  if (USE_MOCK_AUTH) {
    return mockFetchWorkspaceById(workspaceId)
  }

  const { data } = await api.get(`/admin/workspaces/${workspaceId}`)
  return unwrap({ data })
}

export async function fetchWorkspaceOwners() {
  if (USE_MOCK_AUTH) {
    return mockFetchWorkspaceOwners()
  }

  try {
    const { data } = await api.get('/admin/workspaces/owners')
    return unwrap({ data }) ?? []
  } catch {
    return []
  }
}

export async function createWorkspace(payload) {
  if (USE_MOCK_AUTH) {
    return mockCreateWorkspace(payload)
  }

  const { data } = await api.post('/admin/workspaces', payload)
  return unwrap({ data })
}

export async function updateWorkspace(workspaceId, payload) {
  if (USE_MOCK_AUTH) {
    return mockUpdateWorkspace(workspaceId, payload)
  }

  const { data } = await api.put(`/admin/workspaces/${workspaceId}`, payload)
  return unwrap({ data })
}

export async function updateWorkspaceStatus(workspaceId, status) {
  if (USE_MOCK_AUTH) {
    return mockUpdateWorkspaceStatus(workspaceId, status)
  }

  const { data } = await api.patch(`/admin/workspaces/${workspaceId}/status`, {
    status,
  })
  return unwrap({ data })
}
