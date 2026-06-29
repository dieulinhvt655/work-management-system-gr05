import { mapProjectResponse } from '../utils/projectMappers'
import { unwrapApiResponse } from './apiResponse'
import api, { getAuthRequestConfig } from './axios'
import { fetchTeams } from './teamsApi'
import { fetchWorkspaces } from './workspacesApi'

async function resolveWorkspaceIds(workspaceId) {
  if (workspaceId) {
    return [String(workspaceId)]
  }

  const workspaces = await fetchWorkspaces()
  return workspaces.map((workspace) => workspace.id)
}

async function fetchProjectMemberCount(workspaceId, teamId, projectId) {
  try {
    const { data } = await api.get(
      `/workspaces/${workspaceId}/teams/${teamId}/projects/${projectId}/members`,
      getAuthRequestConfig(),
    )
    const members = unwrapApiResponse({ data }) ?? []
    return members.length
  } catch {
    return 0
  }
}

async function fetchProjectsForTeam(workspaceId, team, params = {}) {
  const size = 100
  let page = 0
  let totalPages = 1
  const items = []

  while (page < totalPages) {
    const { data } = await api.get(
      `/workspaces/${workspaceId}/teams/${team.id}/projects`,
      getAuthRequestConfig({
        params: {
          page,
          size,
          keyword: params.keyword?.trim() || undefined,
          status: params.status || undefined,
        },
      }),
    )
    const payload = unwrapApiResponse({ data })
    items.push(...(payload.items ?? []))
    totalPages = payload.totalPages ?? 1
    page += 1
  }

  return Promise.all(
    items.map(async (item) => {
      const memberCount = await fetchProjectMemberCount(
        workspaceId,
        team.id,
        item.id,
      )
      return mapProjectResponse(item, {
        teamName: team.name,
        memberCount,
      })
    }),
  )
}

/** Lấy tất cả dự án trong workspace (qua từng team). */
export async function fetchProjects(workspaceId, params = {}) {
  const workspaceIds = await resolveWorkspaceIds(workspaceId)
  const allProjects = []

  for (const wsId of workspaceIds) {
    const teams = await fetchTeams(wsId, params)
    const teamProjects = await Promise.all(
      teams.map((team) => fetchProjectsForTeam(wsId, team, params)),
    )
    allProjects.push(...teamProjects.flat())
  }

  return allProjects
}

export async function fetchProjectById(projectId, workspaceId) {
  const projects = await fetchProjects(workspaceId)
  const project = projects.find((entry) => entry.id === String(projectId))

  if (!project) {
    throw new Error('Không tìm thấy dự án.')
  }

  return project
}

export async function fetchProjectSummary(workspaceId) {
  const projects = await fetchProjects(workspaceId)

  return {
    total: projects.length,
    active: projects.filter((project) => project.status === 'ACTIVE').length,
    completed: projects.filter((project) => project.status === 'COMPLETED').length,
    draft: projects.filter((project) => project.status === 'DRAFT').length,
  }
}
