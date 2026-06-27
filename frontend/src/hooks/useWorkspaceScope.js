import { useCallback } from 'react'
import { useAuth } from '../context/AuthContext'

/**
 * Resource-level workspace scope from auth session.
 * Permission codes control UI visibility; scope controls which workspace data applies.
 * Backend must enforce the same rules on every API call.
 */
export function useWorkspaceScope() {
  const { user } = useAuth()

  const workspaceId = user?.workspaceId ?? null
  const managedWorkspaceIds = user?.managedWorkspaceIds ?? []

  const canAccessWorkspace = useCallback(
    (targetWorkspaceId) => {
      if (!targetWorkspaceId) return false
      if (!managedWorkspaceIds.length) return false
      return managedWorkspaceIds.includes(targetWorkspaceId)
    },
    [managedWorkspaceIds],
  )

  const isSystemScope = managedWorkspaceIds.length === 0 && workspaceId === null

  return {
    workspaceId,
    managedWorkspaceIds,
    isSystemScope,
    canAccessWorkspace,
  }
}
