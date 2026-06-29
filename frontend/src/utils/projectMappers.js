import { PROJECT_STATUS } from '../constants/projects'

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
    managerMemberId: project.projectManagerMemberId ?? null,
    startDate: project.startDate ?? null,
    endDate: project.endDate ?? null,
    memberCount: extras.memberCount ?? 0,
    createdAt: project.createdAt ?? null,
    updatedAt: project.updatedAt ?? null,
  }
}
