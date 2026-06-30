import {
  mapActivityLogResponse,
  mapAttachmentResponse,
  mapBacklogItemResponse,
  mapProjectDashboardResponse,
  mapProjectMemberResponse,
  mapProjectResponse,
  mapTaskResponse,
} from '../utils/projectMappers'
import {
  extractPageItems,
  fetchAllPages,
  unwrapApiResponse,
} from './apiResponse'
import api from './axios'
import { fetchTeams } from './teamsApi'
import { fetchWorkspaces } from './workspacesApi'
import { getManagedTeamId, getManagedTeamName } from '../utils/teamLeaderScope'

async function resolveWorkspaceIds(workspaceId) {
  if (workspaceId) {
    return [String(workspaceId)]
  }

  const workspaces = await fetchWorkspaces()
  return workspaces.map((workspace) => workspace.id)
}

function isCurrentUserTeamLeader(team, currentUser) {
  if (!currentUser?.id) return false
  return (team.members ?? []).some(
    (member) =>
      member.isLeader && String(member.userId) === String(currentUser.id),
  )
}

function getCurrentProjectMember(members, currentUser) {
  if (!currentUser?.id) return null
  return members.find(
    (member) =>
      member.userId && String(member.userId) === String(currentUser.id),
  ) ?? null
}

export async function fetchProjectMembers(project) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/members`,
  )
  const members = unwrapApiResponse({ data }) ?? []
  return members.map(mapProjectMemberResponse).filter(Boolean)
}

async function fetchProjectMembersByIds(workspaceId, teamId, projectId) {
  return fetchProjectMembers({ workspaceId, teamId, id: projectId })
}

async function fetchProjectsForTeam(workspaceId, team, params = {}, currentUser) {
  const items = await fetchAllPages(async ({ page, size }) => {
    const { data } = await api.get(
      `/workspaces/${workspaceId}/teams/${team.id}/projects`,
      {
        params: {
          page,
          size,
          keyword: params.keyword?.trim() || undefined,
          status: params.status || undefined,
        },
      },
    )
    return unwrapApiResponse({ data })
  })

  const userIsTeamLeader = isCurrentUserTeamLeader(team, currentUser)

  return Promise.all(
    items.map(async (item) => {
      let members = []

      try {
        members = await fetchProjectMembersByIds(workspaceId, team.id, item.id)
      } catch {
        members = []
      }

      const currentMember = getCurrentProjectMember(members, currentUser)
      const isCurrentUserProjectManager =
        Boolean(currentMember) &&
        currentMember.roleName === 'Project Manager'

      return mapProjectResponse(item, {
        teamName: team.name,
        memberCount: members.length,
        members,
        currentMember,
        isCurrentUserTeamLeader: userIsTeamLeader,
        isCurrentUserProjectManager,
        canCurrentUserSee:
          userIsTeamLeader ||
          isCurrentUserProjectManager ||
          Boolean(currentMember) ||
          currentUser?.isSystemAdmin,
      })
    }),
  )
}

export async function fetchProjectsForCurrentTeam(
  workspaceId,
  teamId,
  params = {},
  currentUser = null,
) {
  if (!workspaceId || !teamId) return []

  return fetchProjectsForTeam(
    workspaceId,
    {
      id: teamId,
      name: getManagedTeamName(currentUser),
      members: [
        {
          isLeader: true,
          userId: currentUser?.id,
        },
      ],
    },
    params,
    currentUser,
  )
}

/** Lấy project thật từ backend, đi qua từng team trong workspace được phép. */
export async function fetchProjects(workspaceId, params = {}, currentUser = null) {
  const workspaceIds = await resolveWorkspaceIds(workspaceId)
  const allProjects = []

  for (const wsId of workspaceIds) {
    const teams = await fetchTeams(wsId)
    const teamProjects = await Promise.all(
      teams.map((team) => fetchProjectsForTeam(wsId, team, params, currentUser)),
    )
    allProjects.push(...teamProjects.flat())
  }

  return allProjects
}

export async function fetchProjectById(projectId, workspaceId, currentUser = null) {
  const managedTeamId = getManagedTeamId(currentUser)
  const projects = managedTeamId
    ? await fetchProjectsForCurrentTeam(workspaceId, managedTeamId, {}, currentUser)
    : await fetchProjects(workspaceId, {}, currentUser)
  const project = projects.find((entry) => entry.id === String(projectId))

  if (!project) {
    throw new Error('Không tìm thấy dự án.')
  }

  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}`,
  )

  const freshProject = mapProjectResponse(unwrapApiResponse({ data }), {
    teamName: project.teamName,
    memberCount: project.memberCount,
    members: project.members,
    currentMember: project.currentMember,
    isCurrentUserTeamLeader: project.isCurrentUserTeamLeader,
    isCurrentUserProjectManager: project.isCurrentUserProjectManager,
    canCurrentUserSee: project.canCurrentUserSee,
  })

  return freshProject
}

export async function createProject(workspaceId, teamId, payload) {
  const projectManagerMemberId = Number(payload.projectManagerMemberId)
  const body = {
    name: payload.name?.trim(),
    objective: payload.objective?.trim(),
    scope: payload.scope?.trim(),
    description: payload.description?.trim() || undefined,
    startDate: payload.startDate || undefined,
    endDate: payload.endDate || undefined,
  }

  if (Number.isFinite(projectManagerMemberId)) {
    body.projectManagerMemberId = projectManagerMemberId
  }

  let data

  try {
    const response = await api.post(
      `/workspaces/${workspaceId}/teams/${teamId}/projects`,
      body,
    )
    data = response.data
  } catch (error) {
    const isAuthForbidden =
      error?.response?.status === 403 &&
      error?.response?.data?.errorCode === 'AUTH_004'

    if (!isAuthForbidden) {
      throw error
    }

    try {
      const response = await api.post('/projects', {
        ...body,
        workspaceId: Number(workspaceId),
        teamId: Number(teamId),
      })
      data = response.data
    } catch {
      throw error
    }
  }

  return mapProjectResponse(unwrapApiResponse({ data }))
}

export async function updateProject(project, payload) {
  const projectManagerMemberId = Number(payload.projectManagerMemberId)
  const body = {
    name: payload.name?.trim(),
    description: payload.description?.trim() || undefined,
    objective: payload.objective?.trim() || undefined,
    scope: payload.scope?.trim() || undefined,
    startDate: payload.startDate || undefined,
    endDate: payload.endDate || undefined,
  }

  if (Number.isFinite(projectManagerMemberId)) {
    body.projectManagerMemberId = projectManagerMemberId
  }

  const { data } = await api.put(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}`,
    body,
  )

  return mapProjectResponse(unwrapApiResponse({ data }), {
    teamName: project.teamName,
    memberCount: project.memberCount,
    members: project.members,
  })
}

export async function activateProject(project) {
  const { data } = await api.patch(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/activate`,
  )
  return mapProjectResponse(unwrapApiResponse({ data }), project)
}

export async function completeProject(project) {
  const { data } = await api.patch(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/complete`,
  )
  return mapProjectResponse(unwrapApiResponse({ data }), project)
}

export async function archiveProject(project) {
  const { data } = await api.patch(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/archive`,
  )
  return mapProjectResponse(unwrapApiResponse({ data }), project)
}

export async function addProjectMember(project, payload) {
  const { data } = await api.post(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/members`,
    {
      teamMemberId: Number(payload.teamMemberId),
      roleId: Number(payload.roleId),
    },
  )
  return mapProjectMemberResponse(unwrapApiResponse({ data }))
}

export async function updateProjectMember(project, memberId, payload) {
  const { data } = await api.patch(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/members/${memberId}`,
    {
      roleId: Number(payload.roleId),
      status: payload.status,
    },
  )
  return mapProjectMemberResponse(unwrapApiResponse({ data }))
}

export async function fetchProjectDashboard(project) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/dashboard`,
  )
  return mapProjectDashboardResponse(unwrapApiResponse({ data }))
}

export async function fetchPersonalProjectDashboard(project) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/dashboard/personal`,
  )
  return unwrapApiResponse({ data })
}

export async function fetchProjectBacklog(project) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog`,
  )
  return unwrapApiResponse({ data })
}

export async function fetchProjectBacklogItems(project, params = {}) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 100,
        status: params.status || undefined,
      },
    },
  )
  return extractPageItems(unwrapApiResponse({ data }))
    .map(mapBacklogItemResponse)
    .filter(Boolean)
}

export async function createBacklogItem(project, payload) {
  const { data } = await api.post(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items`,
    {
      title: payload.title?.trim(),
      description: payload.description?.trim() || undefined,
      type: payload.type || 'FEATURE',
      priority: payload.priority || 'MEDIUM',
      desiredDueDate: payload.desiredDueDate || undefined,
      proposerMemberId: payload.proposerMemberId
        ? Number(payload.proposerMemberId)
        : undefined,
    },
  )
  return mapBacklogItemResponse(unwrapApiResponse({ data }))
}

export async function fetchBacklogItemTasks(project, itemId) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}/tasks`,
  )
  const tasks = unwrapApiResponse({ data }) ?? []
  return tasks.map(mapTaskResponse).filter(Boolean)
}

export async function createBacklogItemTask(project, itemId, payload) {
  const { data } = await api.post(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}/tasks`,
    {
      title: payload.title?.trim(),
      description: payload.description?.trim() || undefined,
      priority: payload.priority || 'MEDIUM',
      assigneeMemberId: payload.assigneeMemberId
        ? Number(payload.assigneeMemberId)
        : undefined,
      deadline: payload.deadline || undefined,
    },
  )
  return mapTaskResponse(unwrapApiResponse({ data }))
}

export async function fetchProjectAttachments(project) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/attachments`,
  )
  const attachments = unwrapApiResponse({ data }) ?? []
  return attachments.map(mapAttachmentResponse).filter(Boolean)
}

export async function uploadProjectAttachment(project, file) {
  const formData = new FormData()
  formData.append('file', file)

  const { data } = await api.post(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/attachments`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return mapAttachmentResponse(unwrapApiResponse({ data }))
}

export async function fetchProjectActivityLogs(project, params = {}) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/activity-logs`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
      },
    },
  )
  return extractPageItems(unwrapApiResponse({ data }))
    .map(mapActivityLogResponse)
    .filter(Boolean)
}

export async function fetchProjectSummary(workspaceId, currentUser = null) {
  const projects = await fetchProjects(workspaceId, {}, currentUser)

  return {
    total: projects.length,
    active: projects.filter((project) => project.status === 'ACTIVE').length,
    completed: projects.filter((project) => project.status === 'COMPLETED').length,
    draft: projects.filter((project) => project.status === 'DRAFT').length,
  }
}
