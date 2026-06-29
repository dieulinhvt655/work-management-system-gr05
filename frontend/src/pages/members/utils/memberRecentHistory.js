function formatDateTime(value) {
  if (!value) return '—'

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function classifyOrgHistory(entry) {
  const message = entry.message?.toLowerCase() ?? ''
  if (message.includes('chuyển') || message.includes('gán vào')) {
    return 'TEAM_CHANGE'
  }
  if (message.includes('trạng thái')) {
    return 'STATUS_UPDATE'
  }
  return 'ORG_UPDATE'
}

export function getProjectParticipationType(projectRole) {
  const normalized = projectRole?.toLowerCase() ?? ''
  if (
    normalized.includes('manager') ||
    normalized.includes('leader') ||
    normalized.includes('pm')
  ) {
    return 'Project Manager'
  }
  return 'Contributor'
}

export function buildMemberRecentHistory(member) {
  const items = []

  for (const entry of member.organizationHistory ?? []) {
    items.push({
      id: entry.id,
      type: classifyOrgHistory(entry),
      message: entry.message,
      meta: `${entry.changedBy ?? 'Hệ thống'} · ${formatDateTime(entry.createdAt)}`,
      createdAt: entry.createdAt,
    })
  }

  for (const project of member.projectHistory ?? []) {
    if (project.joinedAt) {
      items.push({
        id: `${project.id}-join-${project.joinedAt}`,
        type: 'PROJECT_ALLOCATION',
        message: `Phân bổ dự án: ${project.name}`,
        meta: formatDateTime(project.joinedAt),
        createdAt: project.joinedAt,
      })
    }

    if (project.leftAt) {
      items.push({
        id: `${project.id}-leave-${project.leftAt}`,
        type: 'PROJECT_ALLOCATION',
        message: `Rời dự án: ${project.name}`,
        meta: formatDateTime(project.leftAt),
        createdAt: project.leftAt,
      })
    }
  }

  return items
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 8)
}

export const RECENT_HISTORY_TYPE_LABELS = {
  TEAM_CHANGE: 'Chuyển Team',
  STATUS_UPDATE: 'Cập nhật trạng thái',
  PROJECT_ALLOCATION: 'Phân bổ dự án',
  ORG_UPDATE: 'Cập nhật tổ chức',
}
