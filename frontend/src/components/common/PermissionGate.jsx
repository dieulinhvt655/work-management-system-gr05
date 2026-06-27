import { usePermission } from '../../hooks/usePermission'

export default function PermissionGate({
  permission,
  anyOf,
  allOf,
  fallback = null,
  children,
}) {
  const { can, canAny, canAll } = usePermission()

  let allowed = true

  if (permission) {
    allowed = can(permission)
  } else if (anyOf?.length) {
    allowed = canAny(anyOf)
  } else if (allOf?.length) {
    allowed = canAll(allOf)
  }

  if (!allowed) {
    return fallback
  }

  return children
}
