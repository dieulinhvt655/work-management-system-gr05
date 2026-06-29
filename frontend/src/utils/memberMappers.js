import { mapBackendRoleToFrontendKey, resolveEmployeeCode } from './userMappers'

function normalizeOrganizationStatus(status) {
  const value = String(status ?? 'ACTIVE').trim().toUpperCase()
  return value === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE'
}

/** Map backend WorkspaceMemberResponse → frontend organization member model. */
export function mapWorkspaceMemberResponse(member) {
  if (!member) return null

  const user = member.user ?? {}
  const roleName = member.role?.name ?? null

  return {
    id: String(member.id),
    workspaceId: String(member.workspaceId),
    userId: String(user.id ?? member.userId ?? ''),
    fullName: user.fullName,
    email: user.email,
    employeeCode: resolveEmployeeCode(user),
    username: user.username ?? '',
    phone: user.phone ?? null,
    teamId: null,
    teamName: '—',
    position: '—',
    role: mapBackendRoleToFrontendKey(member.role),
    roleId: member.role?.id ?? null,
    roleName,
    organizationStatus: normalizeOrganizationStatus(member.status),
    activeTaskCount: 0,
    joinedAt: member.joinedAt ?? null,
    updatedAt: member.removedAt ?? member.joinedAt ?? null,
    organizationHistory: [],
    projectHistory: [],
    currentProject: null,
  }
}

export function buildMemberSummary(members = []) {
  const total = members.length
  const active = members.filter((m) => m.organizationStatus === 'ACTIVE').length
  const inactive = total - active

  return {
    total,
    active,
    inactive,
    unassigned: members.filter(
      (member) => member.isUnassignedToTeam ?? !member.teamId,
    ).length,
  }
}
