import {
  buildMemberSummary,
  mapWorkspaceMemberResponse,
} from '../utils/memberMappers'
import { fetchAllPages, unwrapApiResponse } from './apiResponse'
import api from './axios'
import { fetchAssignableTeams as fetchTeamsForAssignment } from './teamsApi'
import { resolveRoleIdByKey } from './rolesApi'
import { fetchWorkspaces } from './workspacesApi'

async function resolveWorkspaceIds(workspaceId) {
  if (workspaceId) {
    return [String(workspaceId)]
  }

  const workspaces = await fetchWorkspaces()
  return workspaces.map((workspace) => workspace.id)
}

async function fetchMembersForWorkspace(workspaceId) {
  const members = await fetchAllPages(async ({ page, size }) => {
    const { data } = await api.get(`/workspaces/${workspaceId}/members`, {
      params: { page, size },
    })
    return unwrapApiResponse({ data })
  })

  return members.map(mapWorkspaceMemberResponse).filter(Boolean)
}

async function fetchAllMembers(workspaceId) {
  const workspaceIds = await resolveWorkspaceIds(workspaceId)

  if (workspaceIds.length === 0) {
    return []
  }

  const groups = await Promise.all(
    workspaceIds.map((id) => fetchMembersForWorkspace(id)),
  )

  return groups.flat()
}

export async function fetchOrganizationMembers(workspaceId) {
  return fetchAllMembers(workspaceId)
}

export async function fetchAssignableTeams(workspaceId) {
  return fetchTeamsForAssignment(workspaceId)
}

export async function fetchOrganizationMemberSummary(workspaceId) {
  const members = await fetchAllMembers(workspaceId)
  return buildMemberSummary(members)
}

export async function fetchOrganizationMemberById(memberId) {
  const members = await fetchAllMembers()
  const member = members.find((entry) => entry.id === String(memberId))

  if (!member) {
    throw new Error('Không tìm thấy thành viên.')
  }

  return member
}

/** Backend chưa có API lịch sử tổ chức. */
export async function fetchMemberOrgHistory() {
  return []
}

export async function updateMemberOrganization(memberId, payload) {
  const member = await fetchOrganizationMemberById(memberId)

  const body = {
    roleId:
      member.roleId ??
      (await resolveRoleIdByKey(member.roleName ?? 'Workspace Member')),
    status: payload.organizationStatus ?? member.organizationStatus,
  }

  const { data } = await api.patch(
    `/workspaces/${member.workspaceId}/members/${memberId}`,
    body,
  )

  return mapWorkspaceMemberResponse(unwrapApiResponse({ data }))
}

export async function addWorkspaceMember(workspaceId, { userId, roleId }) {
  const { data } = await api.post(`/workspaces/${workspaceId}/members`, {
    userId: Number(userId),
    roleId: Number(roleId),
  })

  return mapWorkspaceMemberResponse(unwrapApiResponse({ data }))
}
