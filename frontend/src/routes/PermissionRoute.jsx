import { Navigate } from 'react-router-dom'
import LoadingScreen from '../components/common/LoadingScreen'
import { usePermission } from '../hooks/usePermission'

export default function PermissionRoute({ permission, anyOf, children }) {
  const { can, canAny, isLoading } = usePermission()

  if (isLoading) {
    return <LoadingScreen />
  }

  const allowed = permission ? can(permission) : canAny(anyOf ?? [])

  if (!allowed) {
    return <Navigate to="/403" replace />
  }

  return children
}
