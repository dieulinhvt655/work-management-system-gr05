export function getManagedTeamId(user) {
  return user?.ledTeamIds?.[0] ?? user?.teamId ?? null
}

export function getManagedTeamName(user) {
  return user?.ledTeamNames?.[0] ?? user?.teamName ?? 'Team hiện tại'
}

export function getManagedTeamOptions(user) {
  const ledTeamIds = Array.isArray(user?.ledTeamIds) ? user.ledTeamIds : []
  const ledTeamNames = Array.isArray(user?.ledTeamNames) ? user.ledTeamNames : []

  if (ledTeamIds.length > 0) {
    return ledTeamIds.map((teamId, index) => ({
      id: String(teamId),
      workspaceId: String(user?.workspaceId ?? ''),
      name: ledTeamNames[index] ?? `Team ${teamId}`,
      members: [],
    }))
  }

  if (user?.teamId && user?.workspaceId) {
    return [
      {
        id: String(user.teamId),
        workspaceId: String(user.workspaceId),
        name: user.teamName || 'Team hiện tại',
        members: [],
      },
    ]
  }

  return []
}

export function buildManagedTeamOption(user) {
  return getManagedTeamOptions(user)[0] ?? null
}
