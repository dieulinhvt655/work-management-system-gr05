import { attachTeamAssignmentsToMembers } from '../pages/teams/utils/buildAssignmentCandidates'
import { mapWorkspaceMemberResponse } from '../utils/memberMappers'
import { fetchAllPages, unwrapApiResponse } from './apiResponse'
import api from './axios'
import { fetchTeams } from './teamsApi'
import { fetchWorkspaces } from './workspacesApi'

const MAX_WORKSPACE_PROBE = 50
const PROBE_BATCH_SIZE = 10

const EMPTY_ORGANIZATION = {
  workspaceId: null,
  workspaceIds: [],
  workspaceMemberId: null,
  teamId: null,
  teamName: 'Chưa phân phòng ban',
  departmentName: null,
  teamAssignments: [],
}

async function fetchWorkspaceMembers(workspaceId) {
  return fetchAllPages(async ({ page, size }) => {
    const { data } = await api.get(`/workspaces/${workspaceId}/members`, {
      params: { page, size },
    })
    return unwrapApiResponse({ data })
  })
}

async function findMembershipInWorkspace(workspaceId, userId) {
  try {
    const members = await fetchWorkspaceMembers(workspaceId)
    const match = members.find(
      (member) => String(member.user?.id ?? member.userId) === String(userId),
    )

    if (!match) {
      return null
    }

    return {
      workspaceId: String(workspaceId),
      member: match,
    }
  } catch {
    return null
  }
}

async function probeWorkspaceMembership(userId) {
  const candidateIds = Array.from({ length: MAX_WORKSPACE_PROBE }, (_, index) => index + 1)

  for (let start = 0; start < candidateIds.length; start += PROBE_BATCH_SIZE) {
    const batch = candidateIds.slice(start, start + PROBE_BATCH_SIZE)
    const results = await Promise.all(
      batch.map((workspaceId) => findMembershipInWorkspace(workspaceId, userId)),
    )
    const found = results.find(Boolean)

    if (found) {
      return found
    }
  }

  return null
}

async function resolveMembershipWorkspaceIds(userId) {
  try {
    const workspaces = await fetchWorkspaces()

    if (workspaces.length > 0) {
      return workspaces.map((workspace) => String(workspace.id))
    }
  } catch {
    // Workspace members cannot list all workspaces.
  }

  const probed = await probeWorkspaceMembership(userId)
  return probed ? [probed.workspaceId] : []
}

function resolveTeamFromTeams(user, teams) {
  for (const team of teams) {
    const userInTeam = (team.members ?? []).find(
      (member) => String(member.userId) === String(user.id),
    )

    if (userInTeam) {
      return {
        teamId: team.id,
        teamName: team.name,
        teamAssignments: [{ teamId: team.id, teamName: team.name }],
      }
    }
  }

  return null
}

function resolveTeamLeaderStatus(user, teams = []) {
  const ledTeams = teams.filter((team) =>
    (team.members ?? []).some(
      (member) =>
        member.isLeader && String(member.userId) === String(user.id),
    ),
  )

  return {
    isTeamLeader: ledTeams.length > 0,
    ledTeamIds: ledTeams.map((team) => String(team.id)),
    ledTeamNames: ledTeams.map((team) => team.name),
  }
}

/** Resolve workspace + team/department for the signed-in user. */
export async function resolveCurrentUserOrganization(user) {
  if (!user?.id || user.isSystemAdmin) {
    return null
  }

  const workspaceIds = await resolveMembershipWorkspaceIds(user.id)
  const workspaceId = workspaceIds[0] ?? null

  if (!workspaceId) {
    return { ...EMPTY_ORGANIZATION }
  }

  let memberRecord = null

  try {
    const members = await fetchWorkspaceMembers(workspaceId)
    memberRecord = members.find(
      (member) => String(member.user?.id ?? member.userId) === String(user.id),
    )
  } catch {
    memberRecord = null
  }

  let teamId = null
  let teamName = EMPTY_ORGANIZATION.teamName
  let teamAssignments = []
  let isTeamLeader = false
  let ledTeamIds = []
  let ledTeamNames = []

  try {
    const teams = await fetchTeams(workspaceId)
    const leaderStatus = resolveTeamLeaderStatus(user, teams)
    isTeamLeader = leaderStatus.isTeamLeader
    ledTeamIds = leaderStatus.ledTeamIds
    ledTeamNames = leaderStatus.ledTeamNames

    if (memberRecord) {
      const mappedMember = mapWorkspaceMemberResponse(memberRecord)
      const [enriched] = attachTeamAssignmentsToMembers([mappedMember], teams)
      teamId = enriched?.teamId ?? null
      teamName = enriched?.teamName ?? teamName
      teamAssignments = enriched?.teamAssignments ?? []
    } else {
      const fromTeams = resolveTeamFromTeams(user, teams)

      if (fromTeams) {
        teamId = fromTeams.teamId
        teamName = fromTeams.teamName
        teamAssignments = fromTeams.teamAssignments
      }
    }
  } catch {
    // Keep defaults when team APIs are unavailable.
  }

  const departmentName =
    teamName && teamName !== 'Chưa phân phòng ban' ? teamName : null

  return {
    workspaceId,
    workspaceIds,
    workspaceMemberId: memberRecord ? String(memberRecord.id) : null,
    teamId,
    teamName,
    departmentName,
    teamAssignments,
    isTeamLeader,
    ledTeamIds,
    ledTeamNames,
  }
}
