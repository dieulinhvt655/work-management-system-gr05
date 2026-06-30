import { PROJECT_STATUS } from '../constants/projects'
import { resolveEmployeeCode } from './userMappers'

/** Map backend ProjectResponse → frontend project model. */
export function mapProjectResponse(project, extras = {}) {
  if (!project) return null

  return {
    id: String(project.id),
    workspaceId: String(project.workspaceId),
    teamId: String(project.teamId),
    teamName: extras.teamName ?? '—',
    code: project.code ?? '',
    name: project.name,
    description: project.description?.trim() || '—',
    objective: project.objective?.trim() || null,
    scope: project.scope?.trim() || null,
    status: project.status ?? PROJECT_STATUS.DRAFT,
    managerName: project.projectManagerName ?? '—',
    managerMemberId:
      project.projectManagerMemberId != null
        ? String(project.projectManagerMemberId)
        : null,
    startDate: project.startDate ?? null,
    endDate: project.endDate ?? null,
    memberCount: extras.memberCount ?? 0,
    members: extras.members ?? [],
    currentMember: extras.currentMember ?? null,
    isCurrentUserTeamLeader: extras.isCurrentUserTeamLeader ?? false,
    isCurrentUserProjectManager: extras.isCurrentUserProjectManager ?? false,
    canCurrentUserSee:
      extras.canCurrentUserSee ?? Boolean(extras.currentMember),
    createdAt: project.createdAt ?? null,
    updatedAt: project.updatedAt ?? null,
  }
}

export function mapProjectMemberResponse(member) {
  if (!member) return null

  const user = member.user ?? {}

  return {
    id: String(member.id),
    projectId: String(member.projectId),
    teamMemberId: String(member.teamMemberId ?? ''),
    userId: String(user.id ?? member.userId ?? ''),
    fullName: user.fullName ?? '—',
    email: user.email ?? '',
    employeeCode: resolveEmployeeCode(user),
    roleId: member.role?.id ?? null,
    roleName: member.role?.name ?? '—',
    roleScope: member.role?.scope ?? null,
    status: member.status ?? 'ACTIVE',
    joinedAt: member.joinedAt ?? null,
    removedAt: member.removedAt ?? null,
  }
}

export function mapProjectDashboardResponse(dashboard) {
  if (!dashboard) return null

  const taskBreakdown = dashboard.taskBreakdown ?? {}
  const doneTasks = Number(taskBreakdown.done ?? 0)
  const totalTasks = Object.values(taskBreakdown).reduce(
    (sum, value) => sum + Number(value ?? 0),
    0,
  )

  return {
    projectId: String(dashboard.projectId),
    projectName: dashboard.projectName,
    status: dashboard.status ?? PROJECT_STATUS.DRAFT,
    totalPbis: Number(dashboard.totalPbis ?? 0),
    readyPbis: Number(dashboard.readyPbis ?? 0),
    inSprintPbis: Number(dashboard.inSprintPbis ?? 0),
    completedPbis: Number(dashboard.completedPbis ?? 0),
    taskBreakdown: {
      todo: Number(taskBreakdown.todo ?? 0),
      inProgress: Number(taskBreakdown.inProgress ?? 0),
      review: Number(taskBreakdown.review ?? 0),
      done: doneTasks,
      reopened: Number(taskBreakdown.reopened ?? 0),
      cancelled: Number(taskBreakdown.cancelled ?? 0),
    },
    totalTasks,
    completionPercent:
      totalTasks > 0 ? Math.round((doneTasks / totalTasks) * 100) : 0,
    activeSprint: dashboard.activeSprint
      ? {
          id: String(dashboard.activeSprint.sprintId),
          name: dashboard.activeSprint.sprintName,
          status: dashboard.activeSprint.status,
          startDate: dashboard.activeSprint.startDate,
          endDate: dashboard.activeSprint.endDate,
          totalTasks: Number(dashboard.activeSprint.totalTasks ?? 0),
          doneTasks: Number(dashboard.activeSprint.doneTasks ?? 0),
          completionPercent: Number(
            dashboard.activeSprint.completionPercent ?? 0,
          ),
        }
      : null,
    memberWorkload: (dashboard.memberWorkload ?? []).map((member) => ({
      memberId: String(member.memberId),
      memberName: member.memberName,
      assignedTasks: Number(member.assignedTasks ?? 0),
      inProgressTasks: Number(member.inProgressTasks ?? 0),
      doneTasks: Number(member.doneTasks ?? 0),
    })),
  }
}

export function mapBacklogItemResponse(item) {
  if (!item) return null

  return {
    id: String(item.id),
    backlogId: String(item.backlogId ?? ''),
    projectId: String(item.projectId ?? ''),
    sprintId: item.sprintId != null ? String(item.sprintId) : null,
    title: item.title,
    description: item.description ?? '',
    type: item.type ?? 'FEATURE',
    priority: item.priority ?? 'MEDIUM',
    status: item.status ?? 'NEW',
    desiredDueDate: item.desiredDueDate ?? null,
    proposerMemberId:
      item.proposerMemberId != null ? String(item.proposerMemberId) : null,
    proposerName: item.proposerName ?? '—',
    createdAt: item.createdAt ?? null,
    updatedAt: item.updatedAt ?? null,
  }
}

export function mapTaskResponse(task) {
  if (!task) return null

  return {
    id: String(task.id),
    pbiId: String(task.pbiId ?? ''),
    pbiTitle: task.pbiTitle ?? '',
    sprintId: task.sprintId != null ? String(task.sprintId) : null,
    parentTaskId: task.parentTaskId != null ? String(task.parentTaskId) : null,
    title: task.title,
    description: task.description ?? '',
    priority: task.priority ?? 'MEDIUM',
    status: task.status ?? 'TO_DO',
    progress: Number(task.progress ?? 0),
    startDate: task.startDate ?? null,
    deadline: task.deadline ?? null,
    completedAt: task.completedAt ?? null,
    assigneeMemberId:
      task.assigneeMemberId != null ? String(task.assigneeMemberId) : null,
    assigneeName: task.assigneeName ?? '—',
    reporterMemberId:
      task.reporterMemberId != null ? String(task.reporterMemberId) : null,
    reporterName: task.reporterName ?? '—',
    reviewerMemberId:
      task.reviewerMemberId != null ? String(task.reviewerMemberId) : null,
    reviewerName: task.reviewerName ?? '—',
    workflowStateName: task.workflowStateName ?? '',
    createdAt: task.createdAt ?? null,
    updatedAt: task.updatedAt ?? null,
  }
}

export function mapSprintResponse(sprint) {
  if (!sprint) return null

  return {
    id: String(sprint.id ?? sprint.sprintId),
    projectId: sprint.projectId != null ? String(sprint.projectId) : '',
    name: sprint.name ?? sprint.sprintName ?? '',
    goal: sprint.goal ?? sprint.objective ?? '',
    status: sprint.status ?? 'PLANNING',
    startDate: sprint.startDate ?? null,
    endDate: sprint.endDate ?? null,
    totalTasks: Number(sprint.totalTasks ?? 0),
    doneTasks: Number(sprint.doneTasks ?? 0),
    completionPercent: Number(sprint.completionPercent ?? 0),
    createdAt: sprint.createdAt ?? null,
    updatedAt: sprint.updatedAt ?? null,
  }
}

export function mapAttachmentResponse(attachment) {
  if (!attachment) return null

  return {
    id: String(attachment.id),
    projectId: String(attachment.projectId ?? ''),
    fileName: attachment.fileName,
    fileType: attachment.fileType ?? '',
    fileSize: Number(attachment.fileSize ?? 0),
    uploadedByMemberId:
      attachment.uploadedByMemberId != null
        ? String(attachment.uploadedByMemberId)
        : null,
    uploadedByName: attachment.uploadedByName ?? '—',
    uploadedAt: attachment.uploadedAt ?? null,
  }
}

export function mapActivityLogResponse(log) {
  if (!log) return null

  return {
    id: String(log.id),
    actorUserId: log.actorUserId != null ? String(log.actorUserId) : null,
    actorName: log.actorName ?? '—',
    projectId: log.projectId != null ? String(log.projectId) : null,
    action: log.action ?? '',
    targetType: log.targetType ?? '',
    targetId: log.targetId != null ? String(log.targetId) : null,
    oldValue: log.oldValue ?? '',
    newValue: log.newValue ?? '',
    createdAt: log.createdAt ?? null,
  }
}
