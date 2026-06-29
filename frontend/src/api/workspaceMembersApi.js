import { unwrapApiResponse } from './apiResponse'
import api from './axios'
import { mapWorkspaceMemberResponse } from '../utils/memberMappers'
import { addWorkspaceMember } from './organizationMembersApi'

export async function fetchWorkspaceMemberUserIds(workspaceId) {
  const { data } = await api.get(`/workspaces/${workspaceId}/members`)
  const members = unwrapApiResponse({ data }) ?? []

  return new Set(
    members
      .map((member) => member.user?.id ?? member.userId)
      .filter(Boolean)
      .map(String),
  )
}

export async function assignAccountsToWorkspace(workspaceId, { userIds, roleId }) {
  const results = await Promise.allSettled(
    userIds.map((userId) =>
      addWorkspaceMember(workspaceId, { userId, roleId }),
    ),
  )

  return results.map((result, index) => ({
    userId: userIds[index],
    success: result.status === 'fulfilled',
    member: result.status === 'fulfilled' ? result.value : null,
    error: result.status === 'rejected' ? result.reason : null,
  }))
}

export { addWorkspaceMember, mapWorkspaceMemberResponse }
