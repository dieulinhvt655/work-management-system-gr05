import { isWorkspaceOwnerUser } from '../utils/userRoleUtils'
import { useAuth } from '../context/AuthContext'

export function useIsWorkspaceOwner() {
  const { user } = useAuth()
  return isWorkspaceOwnerUser(user)
}
