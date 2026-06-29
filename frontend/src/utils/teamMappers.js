import { TEAM_STATUS, TEAM_TYPE } from '../constants/teams'

function mapTeamStatus(status) {
  if (status === 'ACTIVE') return TEAM_STATUS.ACTIVE
  return TEAM_STATUS.INACTIVE
}

function deriveTeamCode(team) {
  if (team.code) return team.code

  const initials = team.name
    ?.split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')

  return initials || `T${team.id}`
}

/** Map backend TeamResponse → frontend team model. */
export function mapTeamResponse(team, extras = {}) {
  if (!team) return null

  return {
    id: String(team.id),
    workspaceId: String(team.workspaceId),
    code: deriveTeamCode(team),
    name: team.name,
    type: extras.type ?? TEAM_TYPE.DEPARTMENT,
    description: team.description?.trim() || '—',
    status: mapTeamStatus(team.status),
    memberCount: extras.memberCount ?? 0,
    projectCount: extras.projectCount ?? 0,
    openTaskCount: extras.openTaskCount ?? 0,
    leader: team.teamLeaderId
      ? {
          memberId: String(team.teamLeaderId),
          fullName: team.teamLeaderName ?? '—',
        }
      : null,
    members: extras.members ?? [],
    createdAt: team.createdAt ?? null,
    updatedAt: team.updatedAt ?? null,
  }
}

export function buildTeamSummary(teams = []) {
  const active = teams.filter((team) => team.status === TEAM_STATUS.ACTIVE).length

  return {
    total: teams.length,
    active,
    disbanded: teams.length - active,
    withoutLeader: teams.filter((team) => !team.leader).length,
    newMembersThisMonth: 0,
  }
}
