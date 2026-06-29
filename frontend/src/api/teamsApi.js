import { TEAM_STATUS } from '../constants/teams'
import { buildTeamSummary, mapTeamResponse } from '../utils/teamMappers'
import { mapTeamMemberResponse } from '../utils/teamMemberMappers'
import { fetchAllPages, unwrapApiResponse } from './apiResponse'
import api from './axios'
import { resolveRoleIdByKey } from './rolesApi'
import { fetchWorkspaces } from './workspacesApi'

async function resolveWorkspaceIds(workspaceId) {
  if (workspaceId) {
    return [String(workspaceId)]
  }

  const workspaces = await fetchWorkspaces()
  return workspaces.map((workspace) => workspace.id)
}

async function fetchAllTeamMemberRecords(workspaceId, teamId) {
  return fetchAllPages(async ({ page, size }) => {
    const { data } = await api.get(
      `/workspaces/${workspaceId}/teams/${teamId}/members`,
      { params: { page, size } },
    )
    return unwrapApiResponse({ data })
  })
}

export async function fetchTeamMembers(workspaceId, teamId) {
  const members = await fetchAllTeamMemberRecords(workspaceId, teamId)
  return members.map((member) => mapTeamMemberResponse(member)).filter(Boolean)
}

async function fetchTeamMembersWithLeaderFlag(workspaceId, teamId, teamLeaderId) {
  const members = await fetchAllTeamMemberRecords(workspaceId, teamId)
  const leaderId = teamLeaderId != null ? Number(teamLeaderId) : null

  return members
    .map((member) =>
      mapTeamMemberResponse(member, {
        isLeader: leaderId != null && Number(member.id) === leaderId,
      }),
    )
    .filter(Boolean)
}

async function fetchTeamMemberCount(workspaceId, teamId) {
  try {
    const members = await fetchTeamMembers(workspaceId, teamId)
    return members.length
  } catch {
    return 0
  }
}

async function fetchAllTeamsForWorkspace(workspaceId, params = {}) {
  const size = 100
  let page = 0
  let totalPages = 1
  const items = []

  while (page < totalPages) {
    const { data } = await api.get(`/workspaces/${workspaceId}/teams`, {
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

  const teams = await Promise.all(
    items.map(async (item) => {
      let members = []

      try {
        members = await fetchTeamMembersWithLeaderFlag(
          workspaceId,
          item.id,
          item.teamLeaderId,
        )
      } catch {
        members = []
      }

      return mapTeamResponse(item, {
        memberCount: members.length,
        members,
      })
    }),
  )

  return teams
}

export async function fetchTeams(workspaceId, params = {}) {
  const workspaceIds = await resolveWorkspaceIds(workspaceId)

  if (workspaceIds.length === 0) {
    return []
  }

  const groups = await Promise.all(
    workspaceIds.map((id) => fetchAllTeamsForWorkspace(id, params)),
  )

  return groups.flat()
}

export async function fetchTeamById(workspaceId, teamId) {
  const { data } = await api.get(`/workspaces/${workspaceId}/teams/${teamId}`)
  const team = unwrapApiResponse({ data })
  const members = await fetchTeamMembersWithLeaderFlag(
    workspaceId,
    teamId,
    team.teamLeaderId,
  )
  return mapTeamResponse(team, { memberCount: members.length, members })
}

export async function createTeam(workspaceId, payload) {
  const { data } = await api.post(`/workspaces/${workspaceId}/teams`, {
    name: payload.name,
    description: payload.description?.trim() || undefined,
  })

  return mapTeamResponse(unwrapApiResponse({ data }))
}

export async function updateTeam(workspaceId, teamId, payload) {
  const { data } = await api.put(`/workspaces/${workspaceId}/teams/${teamId}`, {
    name: payload.name,
    description: payload.description?.trim() || undefined,
  })

  return mapTeamResponse(unwrapApiResponse({ data }))
}

export async function disbandTeam(workspaceId, teamId) {
  const { data } = await api.patch(
    `/workspaces/${workspaceId}/teams/${teamId}/disband`,
  )

  return mapTeamResponse(unwrapApiResponse({ data }))
}

export async function addTeamMember(workspaceId, teamId, payload) {
  const { data } = await api.post(
    `/workspaces/${workspaceId}/teams/${teamId}/members`,
    {
      workspaceMemberId: Number(payload.workspaceMemberId),
      roleId: Number(payload.roleId),
    },
  )

  return mapTeamMemberResponse(unwrapApiResponse({ data }))
}

export async function updateTeamMember(workspaceId, teamId, memberId, payload) {
  const { data } = await api.patch(
    `/workspaces/${workspaceId}/teams/${teamId}/members/${memberId}`,
    {
      roleId: Number(payload.roleId),
      status: payload.status,
    },
  )

  return mapTeamMemberResponse(unwrapApiResponse({ data }))
}

export async function assignTeamLeader(workspaceId, teamId, memberId) {
  const { data } = await api.patch(
    `/workspaces/${workspaceId}/teams/${teamId}/members/${memberId}/assign-leader`,
  )

  return mapTeamMemberResponse(unwrapApiResponse({ data }))
}

/** Thêm thành viên workspace vào team và gán làm Team Leader. */
export async function assignTeamLeaderFromWorkspaceMember(
  workspaceId,
  teamId,
  { workspaceMemberId, teamLeaderRoleId, existingMembers = [] },
) {
  const existing = existingMembers.find(
    (member) => member.workspaceMemberId === String(workspaceMemberId),
  )

  let teamMember = existing

  if (!teamMember) {
    teamMember = await addTeamMember(workspaceId, teamId, {
      workspaceMemberId,
      roleId: teamLeaderRoleId,
    })
  } else if (Number(existing.roleId) !== Number(teamLeaderRoleId)) {
    teamMember = await updateTeamMember(workspaceId, teamId, teamMember.id, {
      roleId: teamLeaderRoleId,
    })
  }

  await assignTeamLeader(workspaceId, teamId, teamMember.id)
  return teamMember
}

export async function fetchTeamSummary(workspaceId) {
  const teams = await fetchTeams(workspaceId)
  return buildTeamSummary(teams)
}

/** Backend chưa có API activity log — trả về rỗng. */
export async function fetchTeamActivities() {
  return []
}

/**
 * Phân công nhiều workspace member vào một phòng ban.
 * @returns {Promise<Array<{ workspaceMemberId: string, success: boolean, message?: string }>>}
 */
export async function assignMembersToTeam(
  workspaceId,
  teamId,
  workspaceMemberIds,
) {
  const teamMemberRoleId = await resolveRoleIdByKey('Team Member')

  return Promise.all(
    workspaceMemberIds.map(async (workspaceMemberId) => {
      try {
        await addTeamMember(workspaceId, teamId, {
          workspaceMemberId,
          roleId: teamMemberRoleId,
        })
        return { workspaceMemberId: String(workspaceMemberId), success: true }
      } catch (error) {
        return {
          workspaceMemberId: String(workspaceMemberId),
          success: false,
          message: error?.message ?? 'Không thể phân công',
        }
      }
    }),
  )
}

/** Teams đang hoạt động — dùng cho dropdown gán team cho member. */
export async function fetchAssignableTeams(workspaceId) {
  const teams = await fetchTeams(workspaceId)

  return teams
    .filter((team) => team.status === TEAM_STATUS.ACTIVE)
    .map((team) => ({
      id: team.id,
      name: team.name,
      workspaceId: team.workspaceId,
    }))
}
