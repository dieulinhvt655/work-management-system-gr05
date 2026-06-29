import { useQuery } from '@tanstack/react-query'
import { useOutletContext } from 'react-router-dom'
import { fetchRoleById } from '../../../../api/rolesApi'
import LoadingScreen from '../../../../components/common/LoadingScreen'
import { useAuth } from '../../../../context/AuthContext'
import { MOCK_ROLE_LABELS } from '../../../../constants/roles'
import { USER_ROLE_LABELS } from '../../../../constants/users'
import UserStatusBadge from '../components/UserStatusBadge'

export default function UserRolesTab() {
  const { user } = useOutletContext()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const roleLabel =
    user.roleName ??
    USER_ROLE_LABELS[user.role] ??
    MOCK_ROLE_LABELS[user.role] ??
    user.role

  const { data: role, isLoading } = useQuery({
    queryKey: ['admin', 'roles', user.roleId],
    queryFn: () => fetchRoleById(user.roleId),
    enabled: isAuthenticated && !authLoading && Boolean(user.roleId),
  })

  const permissions = role?.permissions ?? []

  if (isLoading) {
    return <LoadingScreen />
  }

  return (
    <section className="user-detail-tab">
      <div className="user-detail-tab__card">
        <div className="user-role-summary">
          <div>
            <p className="user-role-summary__label">Vai trò hiện tại</p>
            <p className="user-role-summary__value">{roleLabel}</p>
            {role?.scope && (
              <p className="user-role-summary__meta">Phạm vi: {role.scope}</p>
            )}
          </div>
          <UserStatusBadge status={user.status} />
        </div>

        <div className="user-permissions">
          <h3 className="user-permissions__title">
            Quyền truy cập ({permissions.length})
          </h3>

          {permissions.length === 0 ? (
            <p className="user-detail-tab__empty">
              Không có quyền nào được gán cho vai trò này.
            </p>
          ) : (
            <ul className="user-permissions__list">
              {permissions.map((permission) => (
                <li key={permission.id} className="user-permissions__item">
                  <span className="user-permissions__dot" aria-hidden="true" />
                  <span>
                    {permission.name}
                    <span className="user-permissions__code">{permission.code}</span>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
