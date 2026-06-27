import { useOutletContext } from 'react-router-dom'
import { getPermissionLabel } from '../../../../constants/permissionLabels'
import { ROLE_PERMISSIONS } from '../../../../constants/mock/rolePermissions'
import { MOCK_ROLE_LABELS } from '../../../../constants/roles'
import { USER_ROLE_LABELS } from '../../../../constants/users'
import UserStatusBadge from '../components/UserStatusBadge'

export default function UserRolesTab() {
  const { user } = useOutletContext()
  const permissions = ROLE_PERMISSIONS[user.role] ?? []
  const roleLabel =
    USER_ROLE_LABELS[user.role] ?? MOCK_ROLE_LABELS[user.role] ?? user.role

  return (
    <section className="user-detail-tab">
      <div className="user-detail-tab__card">
        <div className="user-role-summary">
          <div>
            <p className="user-role-summary__label">Vai trò hiện tại</p>
            <p className="user-role-summary__value">{roleLabel}</p>
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
                <li key={permission} className="user-permissions__item">
                  <span className="user-permissions__dot" aria-hidden="true" />
                  {getPermissionLabel(permission)}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
