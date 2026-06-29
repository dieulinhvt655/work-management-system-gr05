import { WORKSPACE_STATUS } from '../constants/workspaces'

/** Map backend workspace status → frontend status. */
export function mapWorkspaceStatus(status) {
  if (status === 'INACTIVE') return WORKSPACE_STATUS.DISABLED
  return status ?? WORKSPACE_STATUS.ACTIVE
}

function deriveWorkspaceCode(workspace) {
  if (workspace.code) return workspace.code

  const slug = workspace.name
    ?.toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')

  return slug || `ws-${workspace.id}`
}

/** Map backend WorkspaceResponse → frontend workspace model. */
export function mapWorkspaceResponse(workspace, extras = {}) {
  if (!workspace) return null

  return {
    id: String(workspace.id),
    name: workspace.name,
    code: deriveWorkspaceCode(workspace),
    description: workspace.description ?? '',
    status: mapWorkspaceStatus(workspace.status),
    ownerId: workspace.ownerId != null ? String(workspace.ownerId) : null,
    ownerName: workspace.ownerName ?? '—',
    contactEmail: workspace.contactEmail ?? '—',
    contactPhone: workspace.contactPhone ?? '',
    address: workspace.address ?? '',
    logoUrl: workspace.logoUrl ?? '',
    departmentCount: workspace.departmentCount ?? 0,
    memberCount: workspace.memberCount ?? extras.memberCount ?? 0,
    createdAt: workspace.createdAt ?? null,
    updatedAt: workspace.updatedAt ?? null,
  }
}
