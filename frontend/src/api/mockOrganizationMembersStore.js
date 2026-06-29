import { MEMBER_ORG_STATUS } from '../constants/members'
import {
  getMemberCurrentProject,
  getMemberProjectHistory,
  MOCK_MEMBER_ORG_HISTORY,
} from '../constants/mock/memberDetailData'
import { MOCK_ORGANIZATION_MEMBERS } from '../constants/mock/organizationMembersData'
import { MOCK_TEAMS } from '../constants/mock/teamsData'
import { TEAM_STATUS } from '../constants/teams'

function delay(ms = 180) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function cloneMembers() {
  return MOCK_ORGANIZATION_MEMBERS.map((member) => ({ ...member }))
}

let members = cloneMembers()

function cloneOrgHistory() {
  const history = {}
  for (const [memberId, entries] of Object.entries(MOCK_MEMBER_ORG_HISTORY)) {
    history[memberId] = entries.map((entry) => ({ ...entry }))
  }
  return history
}

let memberOrgHistory = cloneOrgHistory()

function getTeamMap() {
  return new Map(MOCK_TEAMS.map((team) => [team.id, team]))
}

function enrichMember(member) {
  const team = member.teamId ? getTeamMap().get(member.teamId) : null
  const currentProject = getMemberCurrentProject(member.id)

  return {
    ...member,
    teamName: team?.name ?? 'Chưa phân nhóm',
    teamType: team?.type ?? null,
    teamStatus: team?.status ?? null,
    currentProject,
    projectCount: currentProject ? 1 : 0,
  }
}

function findMemberIndex(memberId) {
  return members.findIndex((member) => member.id === memberId)
}

export async function mockFetchOrganizationMembers(workspaceId) {
  await delay()

  return members
    .filter((member) => !workspaceId || member.workspaceId === workspaceId)
    .map(enrichMember)
}

export async function mockFetchAssignableTeams(workspaceId) {
  await delay(120)

  return MOCK_TEAMS.filter(
    (team) =>
      (!workspaceId || team.workspaceId === workspaceId) &&
      team.status === TEAM_STATUS.ACTIVE,
  ).map((team) => ({ ...team, leader: team.leader ? { ...team.leader } : null }))
}

export async function mockFetchOrganizationMemberSummary(workspaceId) {
  await delay(120)
  const scopedMembers = await mockFetchOrganizationMembers(workspaceId)

  return {
    total: scopedMembers.length,
    active: scopedMembers.filter(
      (member) => member.organizationStatus === MEMBER_ORG_STATUS.ACTIVE,
    ).length,
    inactive: scopedMembers.filter(
      (member) => member.organizationStatus === MEMBER_ORG_STATUS.INACTIVE,
    ).length,
    unassigned: scopedMembers.filter((member) => !member.teamId).length,
  }
}

function appendOrgHistory(memberId, message) {
  if (!memberOrgHistory[memberId]) {
    memberOrgHistory[memberId] = []
  }

  memberOrgHistory[memberId].unshift({
    id: `mhist-${Date.now()}`,
    action: 'ORG_UPDATED',
    message,
    changedBy: 'Workspace Owner',
    createdAt: new Date().toISOString(),
  })
}

export async function mockFetchOrganizationMemberById(memberId) {
  await delay()

  const index = findMemberIndex(memberId)
  if (index < 0) {
    throw new Error('Không tìm thấy thành viên')
  }

  const member = enrichMember(members[index])

  return {
    ...member,
    projectHistory: getMemberProjectHistory(memberId),
    organizationHistory: (memberOrgHistory[memberId] ?? []).map((entry) => ({
      ...entry,
    })),
  }
}

export async function mockFetchMemberOrgHistory(memberId) {
  await delay(120)
  return (memberOrgHistory[memberId] ?? []).map((entry) => ({ ...entry }))
}

export async function mockUpdateMemberOrganization(memberId, payload) {
  await delay()

  const index = findMemberIndex(memberId)
  if (index < 0) {
    throw new Error('Không tìm thấy thành viên')
  }

  const current = members[index]
  const nextTeamId = payload.teamId || null
  if (nextTeamId) {
    const team = MOCK_TEAMS.find((entry) => entry.id === nextTeamId)

    if (!team || team.status !== TEAM_STATUS.ACTIVE) {
      throw new Error('Team / Department không tồn tại hoặc đã ngừng hoạt động')
    }
  }

  const historyParts = []
  if (nextTeamId !== current.teamId) {
    const team = nextTeamId ? getTeamMap().get(nextTeamId) : null
    historyParts.push(
      team
        ? `Chuyển sang ${team.name}`
        : 'Gỡ khỏi Team / Department',
    )
  }

  const nextPosition = payload.position?.trim() || '—'
  if (nextPosition !== current.position) {
    historyParts.push(`Cập nhật vị trí: ${nextPosition}`)
  }

  if (
    payload.organizationStatus &&
    payload.organizationStatus !== current.organizationStatus
  ) {
    historyParts.push(`Đổi trạng thái tổ chức: ${payload.organizationStatus}`)
  }

  if (payload.note?.trim()) {
    historyParts.push(payload.note.trim())
  }

  members[index] = {
    ...current,
    teamId: nextTeamId,
    position: nextPosition,
    organizationStatus:
      payload.organizationStatus ?? current.organizationStatus,
    updatedAt: new Date().toISOString(),
  }

  if (historyParts.length > 0) {
    appendOrgHistory(memberId, historyParts.join(' · '))
  }

  return enrichMember(members[index])
}

export function resetMockOrganizationMembersStore() {
  members = cloneMembers()
  memberOrgHistory = cloneOrgHistory()
}
