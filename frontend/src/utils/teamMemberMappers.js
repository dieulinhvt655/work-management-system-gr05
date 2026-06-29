import { resolveEmployeeCode } from './userMappers'

/** Map backend TeamMemberResponse → frontend team member model. */
export function mapTeamMemberResponse(member, extras = {}) {
  if (!member) return null

  const user = member.user ?? {}

  const workspaceMemberId =
    member.workspaceMemberId ??
    member.workspaceMember?.id ??
    member.workspace_member_id ??
    member.memberId ??
    null

  return {
    id: String(member.id),
    teamId: String(member.teamId),
    workspaceMemberId: workspaceMemberId != null ? String(workspaceMemberId) : '',
    userId: String(user.id ?? member.userId ?? ''),
    fullName: user.fullName ?? '—',
    email: user.email ?? '',
    employeeCode: resolveEmployeeCode(user),
    roleName: member.role?.name ?? '—',
    roleId: member.role?.id ?? null,
    status: member.status ?? 'ACTIVE',
    isLeader: extras.isLeader ?? false,
    joinedAt: member.joinedAt ?? null,
  }
}
