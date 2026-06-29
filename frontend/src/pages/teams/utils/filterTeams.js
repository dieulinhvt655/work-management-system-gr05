import { LEADER_FILTER } from '../../../constants/teams'

export const FILTER_ALL = ''

export function filterTeams(teams, filters) {
  const search = filters.search.trim().toLowerCase()

  return teams.filter((team) => {
    if (filters.status && team.status !== filters.status) {
      return false
    }

    if (filters.leader === LEADER_FILTER.WITH && !team.leader) {
      return false
    }

    if (filters.leader === LEADER_FILTER.WITHOUT && team.leader) {
      return false
    }

    if (!search) {
      return true
    }

    const haystack = [team.name, team.code, team.description, team.leader?.fullName]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    return haystack.includes(search)
  })
}

export function hasActiveTeamFilters(filters) {
  return Boolean(
    filters.search.trim()
    || (filters.status && filters.status !== FILTER_ALL)
    || (filters.leader && filters.leader !== FILTER_ALL),
  )
}
