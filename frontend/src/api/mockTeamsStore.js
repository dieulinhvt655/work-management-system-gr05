import {
  MOCK_TEAM_ACTIVITIES,
  MOCK_TEAMS,
  computeTeamSummary,
} from '../constants/mock/teamsData'

function delay(ms = 180) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function cloneTeams() {
  return MOCK_TEAMS.map((team) => ({
    ...team,
    leader: team.leader ? { ...team.leader } : null,
  }))
}

export async function mockFetchTeams(workspaceId) {
  await delay()
  const teams = cloneTeams()
  if (!workspaceId) return teams
  return teams.filter((team) => team.workspaceId === workspaceId)
}

export async function mockFetchTeamSummary(workspaceId) {
  await delay(120)
  const teams = await mockFetchTeams(workspaceId)
  return computeTeamSummary(teams)
}

export async function mockFetchTeamActivities(workspaceId, { limit = 5 } = {}) {
  await delay(120)
  if (!workspaceId) {
    return MOCK_TEAM_ACTIVITIES.slice(0, limit).map((item) => ({ ...item }))
  }
  return MOCK_TEAM_ACTIVITIES.slice(0, limit).map((item) => ({ ...item }))
}
