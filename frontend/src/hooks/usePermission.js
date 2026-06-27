import { useCallback, useMemo } from 'react'
import { useAuth } from '../context/AuthContext'

export function usePermission() {
  const { user, isLoading } = useAuth()

  const permissions = useMemo(() => user?.permissions ?? [], [user?.permissions])
  const permissionSet = useMemo(() => new Set(permissions), [permissions])

  const can = useCallback(
    (permission) => {
      if (!permission) return true
      return permissionSet.has(permission)
    },
    [permissionSet],
  )

  const canAny = useCallback(
    (required = []) => {
      if (!required.length) return true
      return required.some((permission) => permissionSet.has(permission))
    },
    [permissionSet],
  )

  const canAll = useCallback(
    (required = []) => {
      if (!required.length) return true
      return required.every((permission) => permissionSet.has(permission))
    },
    [permissionSet],
  )

  return {
    permissions,
    isLoading,
    can,
    canAny,
    canAll,
  }
}
