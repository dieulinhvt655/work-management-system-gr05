import { MEMBER_ORG_STATUS } from '../../../constants/members'

export const FILTER_ALL = ''

export function filterMembers(members, filters) {
  const search = filters.search.trim().toLowerCase()

  return members.filter((member) => {
    const matchesSearch =
      !search ||
      [
        member.fullName,
        member.email,
        member.employeeCode,
        member.position,
        member.teamName,
      ]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(search))

    const matchesTeam =
      filters.teamId === FILTER_ALL ||
      (filters.teamId === 'unassigned'
        ? !member.teamId
        : member.teamId === filters.teamId)

    const matchesStatus =
      filters.status === FILTER_ALL ||
      member.organizationStatus === filters.status

    const hasProject = Boolean(member.currentProject ?? member.projectCount)

    const matchesAvailability =
      filters.availability === FILTER_ALL ||
      (filters.availability === 'allocated' ? hasProject : !hasProject)

    return (
      matchesSearch &&
      matchesTeam &&
      matchesStatus &&
      matchesAvailability
    )
  })
}

export function hasActiveMemberFilters(filters) {
  return (
    filters.search.trim() !== '' ||
    filters.teamId !== FILTER_ALL ||
    filters.status !== FILTER_ALL ||
    filters.availability !== FILTER_ALL
  )
}

export function getMemberStatusTone(status) {
  return status === MEMBER_ORG_STATUS.ACTIVE ? 'active' : 'inactive'
}
