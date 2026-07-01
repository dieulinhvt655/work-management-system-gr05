import {
  mapActivityLogResponse,
  mapAttachmentResponse,
  mapBacklogItemResponse,
  mapProjectDashboardResponse,
  mapProjectMemberResponse,
  mapProjectResponse,
  mapSprintResponse,
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

const MOCK_PROJECT_STORE_KEY = 'wms_mock_project_store_v1'

function shouldUseProjectMock(error) {
  return (
    error?.response?.status === 403 ||
    error?.response?.data?.errorCode === 'AUTH_004' ||
    error?.code === 'AUTH_MISSING_TOKEN'
  )
}

function nowIso() {
  return new Date().toISOString()
}

function readMockStore() {
  try {
    const parsed = JSON.parse(localStorage.getItem(MOCK_PROJECT_STORE_KEY) || '{}')
    const fallback = readMockStoreFallback()
    return {
      ...fallback,
      ...parsed,
      counters: {
        ...fallback.counters,
        ...(parsed.counters ?? {}),
      },
    }
  } catch {
    return readMockStoreFallback()
  }
}

function readMockStoreFallback() {
  return {
    projects: [],
    members: {},
    backlogItems: {},
    tasks: {},
    sprints: {},
    attachments: {},
    activityLogs: {},
    counters: {
      project: 9000,
      projectMember: 9000,
      backlogItem: 9000,
      task: 9000,
      sprint: 9000,
      attachment: 9000,
      activityLog: 9000,
    },
  }
}

function writeMockStore(store) {
  localStorage.setItem(MOCK_PROJECT_STORE_KEY, JSON.stringify(store))
}

function nextMockId(store, key) {
  store.counters[key] = Number(store.counters[key] ?? 9000) + 1
  return store.counters[key]
}

function addMockActivity(store, projectId, action, targetType = 'PROJECT') {
  const id = nextMockId(store, 'activityLog')
  const log = {
    id,
    projectId: Number(projectId),
    actorName: 'Mock User',
    action,
    targetType,
    createdAt: nowIso(),
  }
  const key = String(projectId)
  store.activityLogs[key] = [log, ...(store.activityLogs[key] ?? [])]
}

function findMockProject(store, projectId) {
  return store.projects.find((project) => String(project.id) === String(projectId))
}

function getMockProjectMembers(store, projectId) {
  return store.members[String(projectId)] ?? []
}

function getMockBacklogItems(store, projectId) {
  return store.backlogItems[String(projectId)] ?? []
}

function getMockTasks(store, itemId) {
  return store.tasks[String(itemId)] ?? []
}

function getMockSprints(store, projectId) {
  return store.sprints[String(projectId)] ?? []
}

function mapMockProject(project, extras = {}) {
  const members = extras.members ?? []
  return mapProjectResponse(project, {
    teamName: project.teamName ?? getManagedTeamName(extras.currentUser),
    memberCount: members.length,
    members,
    currentMember: members[0] ?? null,
    isCurrentUserTeamLeader: true,
    isCurrentUserProjectManager: true,
    canCurrentUserSee: true,
  })
}

function createMockProject(workspaceId, teamId, payload) {
  const store = readMockStore()
  const id = nextMockId(store, 'project')
  const pmMemberId = payload.projectManagerMemberId
    ? Number(payload.projectManagerMemberId)
    : nextMockId(store, 'projectMember')
  const project = {
    id,
    workspaceId: Number(workspaceId),
    teamId: Number(teamId),
    code: `MOCK-${id}`,
    name: payload.name?.trim(),
    description: payload.description?.trim() || '',
    objective: payload.objective?.trim(),
    scope: payload.scope?.trim(),
    status: 'DRAFT',
    projectManagerMemberId: pmMemberId,
    projectManagerName: 'Mock Project Manager',
    startDate: payload.startDate || null,
    endDate: payload.endDate || null,
    teamName: payload.teamName,
    createdAt: nowIso(),
    updatedAt: nowIso(),
  }
  const member = {
    id: pmMemberId,
    projectId: id,
    teamMemberId: pmMemberId,
    userId: payload.currentUserId,
    user: {
      id: payload.currentUserId,
      fullName: payload.currentUserName || 'Mock Project Manager',
      email: payload.currentUserEmail || '',
    },
    role: {
      id: 3,
      name: 'Project Manager',
      scope: 'PROJECT',
    },
    status: 'ACTIVE',
    joinedAt: nowIso(),
  }

  store.projects.unshift(project)
  store.members[String(id)] = [member]
  store.backlogItems[String(id)] = []
  store.sprints[String(id)] = []
  store.attachments[String(id)] = []
  store.activityLogs[String(id)] = []
  addMockActivity(store, id, 'CREATED')
  writeMockStore(store)

  return mapMockProject(project, { members: [mapProjectMemberResponse(member)] })
}

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
  try {
    const { data } = await api.get(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/members`,
    )
    const members = unwrapApiResponse({ data }) ?? []
    return members.map(mapProjectMemberResponse).filter(Boolean)
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return getMockProjectMembers(readMockStore(), project.id)
      .map(mapProjectMemberResponse)
      .filter(Boolean)
  }
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
  try {
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
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    return store.projects
      .filter((project) => !workspaceId || String(project.workspaceId) === String(workspaceId))
      .filter((project) => !params.status || project.status === params.status)
      .filter((project) => {
        const keyword = params.keyword?.trim()?.toLowerCase()
        return !keyword || project.name?.toLowerCase().includes(keyword)
      })
      .map((project) =>
        mapMockProject(project, {
          currentUser,
          members: getMockProjectMembers(store, project.id).map(mapProjectMemberResponse),
        }),
      )
  }
}

export async function fetchProjectById(projectId, workspaceId, currentUser = null) {
  const managedTeamId = getManagedTeamId(currentUser)
  const projects = managedTeamId
    ? await fetchProjectsForCurrentTeam(workspaceId, managedTeamId, {}, currentUser)
    : await fetchProjects(workspaceId, {}, currentUser)
  const project = projects.find((entry) => entry.id === String(projectId))

  if (!project) {
    const store = readMockStore()
    const mockProject = findMockProject(store, projectId)
    if (mockProject) {
      return mapMockProject(mockProject, {
        currentUser,
        members: getMockProjectMembers(store, mockProject.id).map(mapProjectMemberResponse),
      })
    }
    throw new Error('Không tìm thấy dự án.')
  }

  let data
  try {
    const response = await api.get(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}`,
    )
    data = response.data
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const mockProject = findMockProject(store, project.id)
    if (!mockProject) throw error
    return mapMockProject(mockProject, {
      currentUser,
      members: getMockProjectMembers(store, mockProject.id).map(mapProjectMemberResponse),
    })
  }

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

  try {
    const { data } = await api.post(
      `/workspaces/${workspaceId}/teams/${teamId}/projects`,
      body,
    )
    return mapProjectResponse(unwrapApiResponse({ data }))
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return createMockProject(workspaceId, teamId, payload)
  }
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

  try {
    const { data } = await api.put(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}`,
      body,
    )

    return mapProjectResponse(unwrapApiResponse({ data }), {
      teamName: project.teamName,
      memberCount: project.memberCount,
      members: project.members,
    })
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const mockProject = findMockProject(store, project.id)
    if (!mockProject) throw error
    Object.assign(mockProject, body, { updatedAt: nowIso() })
    addMockActivity(store, project.id, 'UPDATED')
    writeMockStore(store)
    return mapMockProject(mockProject, {
      members: getMockProjectMembers(store, project.id).map(mapProjectMemberResponse),
    })
  }
}

export async function activateProject(project) {
  try {
    const { data } = await api.patch(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/activate`,
    )
    return mapProjectResponse(unwrapApiResponse({ data }), project)
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const mockProject = findMockProject(store, project.id)
    if (!mockProject) throw error
    mockProject.status = 'ACTIVE'
    mockProject.updatedAt = nowIso()
    addMockActivity(store, project.id, 'ACTIVATED')
    writeMockStore(store)
    return mapMockProject(mockProject, {
      members: getMockProjectMembers(store, project.id).map(mapProjectMemberResponse),
    })
  }
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
  const body = {
    teamMemberId: Number(payload.teamMemberId),
    roleId: Number(payload.roleId),
  }
  try {
    const { data } = await api.post(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/members`,
      body,
    )
    return mapProjectMemberResponse(unwrapApiResponse({ data }))
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const id = nextMockId(store, 'projectMember')
    const member = {
      id,
      projectId: Number(project.id),
      teamMemberId: body.teamMemberId,
      role: { id: body.roleId, name: body.roleId === 3 ? 'Project Manager' : 'Team Member' },
      user: {
        id: body.teamMemberId,
        fullName: `Mock Member ${body.teamMemberId}`,
        email: '',
      },
      status: 'ACTIVE',
      joinedAt: nowIso(),
    }
    const key = String(project.id)
    store.members[key] = [...(store.members[key] ?? []), member]
    addMockActivity(store, project.id, 'MEMBER_ADDED', 'PROJECT_MEMBER')
    writeMockStore(store)
    return mapProjectMemberResponse(member)
  }
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
  try {
    const { data } = await api.get(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/dashboard`,
    )
    return mapProjectDashboardResponse(unwrapApiResponse({ data }))
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const items = getMockBacklogItems(store, project.id)
    const tasks = items.flatMap((item) => getMockTasks(store, item.id))
    const done = tasks.filter((task) => task.status === 'DONE').length
    return {
      projectId: String(project.id),
      projectName: project.name,
      status: project.status,
      totalPbis: items.length,
      readyPbis: items.filter((item) => item.status === 'READY').length,
      inSprintPbis: items.filter((item) => item.sprintId).length,
      completedPbis: items.filter((item) => item.status === 'COMPLETED').length,
      taskBreakdown: {
        todo: tasks.filter((task) => task.status === 'TO_DO').length,
        inProgress: tasks.filter((task) => task.status === 'IN_PROGRESS').length,
        review: tasks.filter((task) => task.status === 'REVIEW').length,
        done,
        reopened: 0,
        cancelled: 0,
      },
      totalTasks: tasks.length,
      completionPercent: tasks.length ? Math.round((done / tasks.length) * 100) : 0,
      activeSprint: null,
      memberWorkload: [],
    }
  }
}

export async function fetchPersonalProjectDashboard(project) {
  const { data } = await api.get(
    `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/dashboard/personal`,
  )
  return unwrapApiResponse({ data })
}

export async function fetchProjectBacklog(project) {
  try {
    const { data } = await api.get(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog`,
    )
    return unwrapApiResponse({ data })
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return {
      id: `mock-backlog-${project.id}`,
      projectId: project.id,
      name: `${project.name} Backlog`,
    }
  }
}

export async function fetchProjectBacklogItems(project, params = {}) {
  try {
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
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return getMockBacklogItems(readMockStore(), project.id)
      .filter((item) => !params.status || item.status === params.status)
      .map(mapBacklogItemResponse)
      .filter(Boolean)
  }
}

export async function createBacklogItem(project, payload) {
  const body = {
    title: payload.title?.trim(),
    description: payload.description?.trim() || undefined,
    type: payload.type || 'FEATURE',
    priority: payload.priority || 'MEDIUM',
    desiredDueDate: payload.desiredDueDate || undefined,
    proposerMemberId: payload.proposerMemberId
      ? Number(payload.proposerMemberId)
      : undefined,
  }
  try {
    const { data } = await api.post(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items`,
      body,
    )
    return mapBacklogItemResponse(unwrapApiResponse({ data }))
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const item = {
      id: nextMockId(store, 'backlogItem'),
      backlogId: `mock-backlog-${project.id}`,
      projectId: Number(project.id),
      status: 'NEW',
      proposerName: 'Mock User',
      createdAt: nowIso(),
      updatedAt: nowIso(),
      ...body,
    }
    const key = String(project.id)
    store.backlogItems[key] = [item, ...(store.backlogItems[key] ?? [])]
    addMockActivity(store, project.id, 'PBI_CREATED', 'PBI')
    writeMockStore(store)
    return mapBacklogItemResponse(item)
  }
}

export async function updateBacklogItem(project, itemId, payload) {
  const path = `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}`
  const body = {
    title: payload.title?.trim(),
    description: payload.description?.trim() || undefined,
    type: payload.type || 'FEATURE',
    priority: payload.priority || 'MEDIUM',
    status: payload.status || undefined,
    desiredDueDate: payload.desiredDueDate || undefined,
  }
  let data

  try {
    const response = await api.patch(path, body)
    data = response.data
  } catch (error) {
    if (shouldUseProjectMock(error)) {
      const store = readMockStore()
      const item = getMockBacklogItems(store, project.id).find(
        (entry) => String(entry.id) === String(itemId),
      )
      if (!item) throw error
      Object.assign(item, body, { updatedAt: nowIso() })
      addMockActivity(store, project.id, 'PBI_UPDATED', 'PBI')
      writeMockStore(store)
      return mapBacklogItemResponse(item)
    }
    if (error?.response?.status !== 405) {
      throw error
    }
    const response = await api.put(path, body)
    data = response.data
  }

  return mapBacklogItemResponse(unwrapApiResponse({ data }))
}

export async function deleteBacklogItem(project, itemId) {
  try {
    await api.delete(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}`,
    )
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const key = String(project.id)
    store.backlogItems[key] = (store.backlogItems[key] ?? []).filter(
      (item) => String(item.id) !== String(itemId),
    )
    delete store.tasks[String(itemId)]
    addMockActivity(store, project.id, 'PBI_DELETED', 'PBI')
    writeMockStore(store)
  }
  return itemId
}

export async function fetchBacklogItemTasks(project, itemId) {
  try {
    const { data } = await api.get(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}/tasks`,
    )
    const tasks = unwrapApiResponse({ data }) ?? []
    return tasks.map(mapTaskResponse).filter(Boolean)
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return getMockTasks(readMockStore(), itemId).map(mapTaskResponse).filter(Boolean)
  }
}

export async function createBacklogItemTask(project, itemId, payload) {
  const body = {
    title: payload.title?.trim(),
    description: payload.description?.trim() || undefined,
    priority: payload.priority || 'MEDIUM',
    assigneeMemberId: payload.assigneeMemberId
      ? Number(payload.assigneeMemberId)
      : undefined,
    deadline: payload.deadline || undefined,
  }
  try {
    const { data } = await api.post(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}/tasks`,
      body,
    )
    return mapTaskResponse(unwrapApiResponse({ data }))
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const task = {
      id: nextMockId(store, 'task'),
      pbiId: Number(itemId),
      pbiTitle:
        getMockBacklogItems(store, project.id).find((item) => String(item.id) === String(itemId))
          ?.title ?? '',
      status: 'TO_DO',
      progress: 0,
      assigneeName: body.assigneeMemberId ? `Mock Member ${body.assigneeMemberId}` : '—',
      createdAt: nowIso(),
      updatedAt: nowIso(),
      ...body,
    }
    const key = String(itemId)
    store.tasks[key] = [...(store.tasks[key] ?? []), task]
    addMockActivity(store, project.id, 'TASK_CREATED', 'TASK')
    writeMockStore(store)
    return mapTaskResponse(task)
  }
}

export async function updateBacklogItemTask(project, itemId, taskId, payload) {
  const path = `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}/tasks/${taskId}`
  const body = {
    title: payload.title?.trim(),
    description: payload.description?.trim() || undefined,
    priority: payload.priority || 'MEDIUM',
    status: payload.status || undefined,
    assigneeMemberId: payload.assigneeMemberId
      ? Number(payload.assigneeMemberId)
      : undefined,
    deadline: payload.deadline || undefined,
  }
  let data

  try {
    const response = await api.patch(path, body)
    data = response.data
  } catch (error) {
    if (shouldUseProjectMock(error)) {
      const store = readMockStore()
      const task = getMockTasks(store, itemId).find(
        (entry) => String(entry.id) === String(taskId),
      )
      if (!task) throw error
      Object.assign(task, body, {
        assigneeName: body.assigneeMemberId ? `Mock Member ${body.assigneeMemberId}` : '—',
        updatedAt: nowIso(),
      })
      addMockActivity(store, project.id, 'TASK_UPDATED', 'TASK')
      writeMockStore(store)
      return mapTaskResponse(task)
    }
    if (error?.response?.status !== 405) {
      throw error
    }
    const response = await api.put(path, body)
    data = response.data
  }

  return mapTaskResponse(unwrapApiResponse({ data }))
}

export async function deleteBacklogItemTask(project, itemId, taskId) {
  try {
    await api.delete(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/backlog/items/${itemId}/tasks/${taskId}`,
    )
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const key = String(itemId)
    store.tasks[key] = (store.tasks[key] ?? []).filter(
      (task) => String(task.id) !== String(taskId),
    )
    addMockActivity(store, project.id, 'TASK_DELETED', 'TASK')
    writeMockStore(store)
  }
  return taskId
}

export async function fetchProjectSprints(project, params = {}) {
  try {
    const { data } = await api.get(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/sprints`,
      {
        params: {
          page: params.page ?? 0,
          size: params.size ?? 100,
          status: params.status || undefined,
        },
      },
    )

    return extractPageItems(unwrapApiResponse({ data }))
      .map(mapSprintResponse)
      .filter(Boolean)
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return getMockSprints(readMockStore(), project.id)
      .filter((sprint) => !params.status || sprint.status === params.status)
      .map(mapSprintResponse)
      .filter(Boolean)
  }
}

export async function createProjectSprint(project, payload) {
  const body = {
    name: payload.name?.trim(),
    goal: payload.goal?.trim() || undefined,
    startDate: payload.startDate || undefined,
    endDate: payload.endDate || undefined,
  }
  try {
    const { data } = await api.post(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/sprints`,
      body,
    )

    return mapSprintResponse(unwrapApiResponse({ data }))
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    const store = readMockStore()
    const sprint = {
      id: nextMockId(store, 'sprint'),
      projectId: Number(project.id),
      status: 'PLANNING',
      totalTasks: 0,
      doneTasks: 0,
      completionPercent: 0,
      createdAt: nowIso(),
      updatedAt: nowIso(),
      ...body,
    }
    const key = String(project.id)
    store.sprints[key] = [sprint, ...(store.sprints[key] ?? [])]
    addMockActivity(store, project.id, 'SPRINT_CREATED', 'SPRINT')
    writeMockStore(store)
    return mapSprintResponse(sprint)
  }
}

export async function fetchProjectAttachments(project) {
  try {
    const { data } = await api.get(
      `/workspaces/${project.workspaceId}/teams/${project.teamId}/projects/${project.id}/attachments`,
    )
    const attachments = unwrapApiResponse({ data }) ?? []
    return attachments.map(mapAttachmentResponse).filter(Boolean)
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return (readMockStore().attachments[String(project.id)] ?? [])
      .map(mapAttachmentResponse)
      .filter(Boolean)
  }
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
  try {
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
  } catch (error) {
    if (!shouldUseProjectMock(error)) throw error
    return (readMockStore().activityLogs[String(project.id)] ?? [])
      .map(mapActivityLogResponse)
      .filter(Boolean)
  }
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
