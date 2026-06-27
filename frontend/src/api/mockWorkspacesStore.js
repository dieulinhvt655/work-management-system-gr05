import {
  MOCK_WORKSPACE_OWNERS,
  MOCK_WORKSPACES,
} from '../constants/mock/workspacesData'
import { WORKSPACE_STATUS } from '../constants/workspaces'

function cloneWorkspaces() {
  return MOCK_WORKSPACES.map((workspace) => ({ ...workspace }))
}

let workspaces = cloneWorkspaces()

function delay(ms = 180) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function findWorkspaceIndex(workspaceId) {
  return workspaces.findIndex((workspace) => workspace.id === workspaceId)
}

function findOwner(ownerId) {
  return MOCK_WORKSPACE_OWNERS.find((owner) => owner.id === ownerId)
}

function normalizeCode(code) {
  return code.trim().toLowerCase()
}

export async function mockFetchWorkspaces() {
  await delay()
  return workspaces.map((workspace) => ({ ...workspace }))
}

export async function mockFetchWorkspaceOwners() {
  await delay()
  return MOCK_WORKSPACE_OWNERS.map((owner) => ({ ...owner }))
}

export async function mockFetchWorkspaceById(workspaceId) {
  await delay()
  const workspace = workspaces.find((entry) => entry.id === workspaceId)
  if (!workspace) {
    throw new Error('Không tìm thấy workspace')
  }
  return { ...workspace }
}

export async function mockCreateWorkspace(payload) {
  await delay()

  const name = payload.name.trim()
  const code = normalizeCode(payload.code)

  if (workspaces.some((workspace) => workspace.name.toLowerCase() === name.toLowerCase())) {
    throw new Error('Tên Workspace hoặc mã Workspace đã tồn tại')
  }

  if (workspaces.some((workspace) => normalizeCode(workspace.code) === code)) {
    throw new Error('Tên Workspace hoặc mã Workspace đã tồn tại')
  }

  const owner = findOwner(payload.ownerId)
  if (!owner) {
    throw new Error('Không tìm thấy Workspace Owner')
  }

  const nextWorkspace = {
    id: `ws-${Date.now()}`,
    name,
    code,
    logoUrl: payload.logoUrl?.trim() || '',
    description: payload.description?.trim() || '',
    contactEmail: payload.contactEmail.trim(),
    contactPhone: payload.contactPhone?.trim() || '',
    address: payload.address?.trim() || '',
    ownerId: owner.id,
    ownerName: owner.fullName,
    departmentCount: 0,
    memberCount: 0,
    status: payload.status ?? WORKSPACE_STATUS.ACTIVE,
    createdAt: new Date().toISOString(),
  }

  workspaces.push(nextWorkspace)
  return { ...nextWorkspace }
}

export async function mockUpdateWorkspace(workspaceId, payload) {
  await delay()
  const index = findWorkspaceIndex(workspaceId)
  if (index < 0) {
    throw new Error('Không tìm thấy workspace')
  }

  const current = workspaces[index]
  const name = payload.name?.trim() ?? current.name
  const code = payload.code ? normalizeCode(payload.code) : current.code

  const nameConflict = workspaces.some(
    (workspace) =>
      workspace.id !== workspaceId &&
      workspace.name.toLowerCase() === name.toLowerCase(),
  )
  const codeConflict = workspaces.some(
    (workspace) =>
      workspace.id !== workspaceId && normalizeCode(workspace.code) === code,
  )

  if (nameConflict || codeConflict) {
    throw new Error('Tên Workspace hoặc mã Workspace đã tồn tại')
  }

  let ownerName = current.ownerName
  let ownerId = current.ownerId

  if (payload.ownerId && payload.ownerId !== current.ownerId) {
    const owner = findOwner(payload.ownerId)
    if (!owner) {
      throw new Error('Không tìm thấy Workspace Owner')
    }
    ownerId = owner.id
    ownerName = owner.fullName
  }

  const next = {
    ...current,
    ...payload,
    name,
    code,
    ownerId,
    ownerName,
    logoUrl: payload.logoUrl?.trim() ?? current.logoUrl,
    description: payload.description?.trim() ?? current.description,
    contactEmail: payload.contactEmail?.trim() ?? current.contactEmail,
    contactPhone: payload.contactPhone?.trim() ?? current.contactPhone,
    address: payload.address?.trim() ?? current.address,
  }

  workspaces[index] = next
  return { ...next }
}

export async function mockUpdateWorkspaceStatus(workspaceId, status) {
  await delay()
  const index = findWorkspaceIndex(workspaceId)
  if (index < 0) {
    throw new Error('Không tìm thấy workspace')
  }

  workspaces[index] = { ...workspaces[index], status }
  return { ...workspaces[index] }
}

export function resetMockWorkspacesStore() {
  workspaces = cloneWorkspaces()
}
