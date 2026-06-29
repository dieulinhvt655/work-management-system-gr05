import { TEAM_STATUS } from '../../../constants/teams'

export const ASSIGN_MEMBER_FILTER = {
  UNASSIGNED: 'UNASSIGNED',
  ALL: 'ALL',
  TRANSFER: 'TRANSFER',
}

export const ASSIGN_MEMBER_FILTER_OPTIONS = [
  { value: ASSIGN_MEMBER_FILTER.UNASSIGNED, label: 'Chưa phân phòng ban' },
  {
    value: ASSIGN_MEMBER_FILTER.ALL,
    label: 'Tất cả (chưa thuộc phòng ban đích)',
  },
  {
    value: ASSIGN_MEMBER_FILTER.TRANSFER,
    label: 'Điều chuyển từ phòng ban khác',
  },
]

function isActiveMember(member) {
  return String(member?.organizationStatus ?? '').toUpperCase() === 'ACTIVE'
}

function isActiveTeamMember(member) {
  return String(member?.status ?? 'ACTIVE').toUpperCase() === 'ACTIVE'
}

function resolveTeamMemberWorkspaceMemberId(member) {
  return (
    member.workspaceMemberId ||
    member.workspaceMember?.id ||
    member.workspace_member_id ||
    member.memberId ||
    null
  )
}

function resolveTeamMemberUserId(member) {
  return member.userId || member.user?.id || null
}

/** workspaceMemberId / userId → [{ teamId, teamName }] */
export function buildMemberTeamIndex(teams = [], { activeTeamsOnly = true } = {}) {
  const map = new Map()
  const scopedTeams = activeTeamsOnly
    ? teams.filter((team) => team.status === TEAM_STATUS.ACTIVE)
    : teams

  const addEntry = (key, entry) => {
    if (!key) return
    const entries = map.get(key) ?? []
    if (entries.some((item) => String(item.teamId) === String(entry.teamId))) {
      return
    }
    entries.push(entry)
    map.set(key, entries)
  }

  for (const team of scopedTeams) {
    const entry = { teamId: team.id, teamName: team.name }

    for (const member of team.members ?? []) {
      if (!isActiveTeamMember(member)) continue

      const workspaceMemberId = resolveTeamMemberWorkspaceMemberId(member)
      const userId = resolveTeamMemberUserId(member)

      if (workspaceMemberId) {
        addEntry(String(workspaceMemberId), entry)
      }
      if (userId) {
        addEntry(`user:${userId}`, entry)
      }
    }
  }

  return map
}

export function getMemberTeamEntries(member, memberTeamIndex) {
  if (!member || !memberTeamIndex) return []

  const byMemberId = memberTeamIndex.get(String(member.id))
  if (byMemberId?.length) return byMemberId

  const byUserId = memberTeamIndex.get(`user:${member.userId}`)
  if (byUserId?.length) return byUserId

  return []
}

/** Gắn phòng ban hiện tại vào danh sách workspace members. */
export function attachTeamAssignmentsToMembers(
  members = [],
  teams = [],
  options = {},
) {
  const memberTeamIndex = buildMemberTeamIndex(teams, options)

  return members.map((member) => {
    const teamEntries = getMemberTeamEntries(member, memberTeamIndex)

    return {
      ...member,
      teamAssignments: teamEntries,
      teamId: teamEntries[0]?.teamId ?? null,
      teamName:
        teamEntries.length > 0
          ? teamEntries.map((entry) => entry.teamName).join(', ')
          : 'Chưa phân phòng ban',
      isUnassignedToTeam: teamEntries.length === 0,
    }
  })
}

function matchesSearch(member, query) {
  if (!query) return true

  return [
    member.fullName,
    member.email,
    member.employeeCode,
    member.username,
    member.phone,
    member.roleName,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
    .includes(query)
}

function matchesFilterMode(teamEntries, filterMode) {
  if (filterMode === ASSIGN_MEMBER_FILTER.UNASSIGNED) {
    return teamEntries.length === 0
  }

  if (filterMode === ASSIGN_MEMBER_FILTER.TRANSFER) {
    return teamEntries.length > 0
  }

  return true
}

export function buildAssignmentFilterCounts({
  members = [],
  memberTeamIndex,
  targetTeamId,
  search = '',
}) {
  const targetId = String(targetTeamId ?? '')
  const query = search.trim().toLowerCase()
  const counts = {
    [ASSIGN_MEMBER_FILTER.UNASSIGNED]: 0,
    [ASSIGN_MEMBER_FILTER.ALL]: 0,
    [ASSIGN_MEMBER_FILTER.TRANSFER]: 0,
  }

  for (const member of members) {
    if (!isActiveMember(member)) continue

    const teamEntries = getMemberTeamEntries(member, memberTeamIndex)
    const currentTeamIds = teamEntries.map((entry) => String(entry.teamId))

    if (targetId && currentTeamIds.includes(targetId)) continue
    if (!matchesSearch(member, query)) continue

    for (const filterMode of Object.keys(counts)) {
      if (matchesFilterMode(teamEntries, filterMode)) {
        counts[filterMode] += 1
      }
    }
  }

  return counts
}

export function buildAssignmentCandidates({
  members = [],
  memberTeamIndex,
  targetTeamId,
  filterMode = ASSIGN_MEMBER_FILTER.UNASSIGNED,
  search = '',
}) {
  const targetId = String(targetTeamId ?? '')
  const query = search.trim().toLowerCase()

  return members
    .filter((member) => {
      if (!isActiveMember(member)) return false

      const teamEntries = getMemberTeamEntries(member, memberTeamIndex)
      const currentTeamIds = teamEntries.map((entry) => String(entry.teamId))

      if (targetId && currentTeamIds.includes(targetId)) return false
      if (!matchesFilterMode(teamEntries, filterMode)) return false

      return matchesSearch(member, query)
    })
    .map((member) => {
      const teamEntries = getMemberTeamEntries(member, memberTeamIndex)

      return {
        ...member,
        currentTeams: teamEntries,
        isTransfer: teamEntries.length > 0,
      }
    })
}

export function formatCurrentTeamsLabel(currentTeams = []) {
  if (currentTeams.length === 0) {
    return 'Chưa phân phòng ban'
  }

  return currentTeams.map((entry) => entry.teamName).join(', ')
}
