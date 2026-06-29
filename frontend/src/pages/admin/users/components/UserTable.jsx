import {
  Eye,
  Lock,
  LockOpen,
  Pencil,
  UserCog,
} from 'lucide-react'
import IconButton from '../../../../components/ui/IconButton'
import PermissionGate from '../../../../components/common/PermissionGate'
import { PERMISSIONS } from '../../../../constants/permissions'
import { USER_ACCOUNT_STATUS, USER_ROLE_LABELS } from '../../../../constants/users'
import UserAvatar from '../../../../components/common/UserAvatar'
import { formatLastActivity } from '../utils/formatUserDate'
import UserStatusBadge from './UserStatusBadge'

export default function UserTable({
  users,
  onView,
  onEdit,
  onChangeRole,
  onToggleLock,
}) {
  return (
    <div className="user-table-wrap">
      <table className="user-table">
        <colgroup>
          <col className="user-table__col-avatar" />
          <col className="user-table__col-name" />
          <col className="user-table__col-email" />
          <col className="user-table__col-code" />
          <col className="user-table__col-dept" />
          <col className="user-table__col-role" />
          <col className="user-table__col-status" />
          <col className="user-table__col-activity" />
          <col className="user-table__col-actions" />
        </colgroup>
        <thead>
          <tr>
            <th scope="col">
              <span className="sr-only">Avatar</span>
            </th>
            <th scope="col">Họ tên</th>
            <th scope="col">Email</th>
            <th scope="col">Mã NV</th>
            <th scope="col">Phòng ban</th>
            <th scope="col">Vai trò</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Hoạt động</th>
            <th scope="col" className="user-table__actions-col">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => {
            const isLocked = user.status === USER_ACCOUNT_STATUS.LOCKED

            return (
              <tr
                key={user.id}
                className="user-table__row--clickable"
                onClick={() => onView(user)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    onView(user)
                  }
                }}
                tabIndex={0}
                role="link"
                aria-label={`Xem chi tiết ${user.fullName}`}
              >
                <td>
                  <UserAvatar fullName={user.fullName} className="user-avatar--table" />
                </td>
                <td className="user-table__name user-table__truncate" title={user.fullName}>
                  {user.fullName}
                </td>
                <td className="user-table__truncate" title={user.email}>
                  {user.email}
                </td>
                <td>
                  <code className="user-table__code">{user.employeeCode}</code>
                </td>
                <td className="user-table__truncate" title={user.departmentName}>
                  {user.departmentName ?? '—'}
                </td>
                <td className="user-table__truncate" title={user.roleName ?? USER_ROLE_LABELS[user.role]}>
                  {user.roleName ?? USER_ROLE_LABELS[user.role] ?? 'Chưa gán'}
                </td>
                <td>
                  <UserStatusBadge status={user.status} />
                </td>
                <td className="user-table__muted user-table__truncate">
                  {formatLastActivity(user.lastActivityAt)}
                </td>
                <td
                  className="user-table__actions-cell"
                  onClick={(event) => event.stopPropagation()}
                  onKeyDown={(event) => event.stopPropagation()}
                >
                  <div className="user-table__actions">
                    <IconButton label="Xem chi tiết" onClick={() => onView(user)}>
                      <Eye size={15} aria-hidden="true" />
                    </IconButton>

                    <PermissionGate permission={PERMISSIONS.USER_MANAGE}>
                      <IconButton label="Chỉnh sửa" onClick={() => onEdit(user)}>
                        <Pencil size={15} aria-hidden="true" />
                      </IconButton>

                      <IconButton
                        label="Đổi vai trò"
                        onClick={() => onChangeRole(user)}
                      >
                        <UserCog size={15} aria-hidden="true" />
                      </IconButton>

                      <IconButton
                        label={isLocked ? 'Mở khóa' : 'Khóa tài khoản'}
                        variant={isLocked ? 'unlock' : 'lock'}
                        onClick={() => onToggleLock(user)}
                      >
                        {isLocked ? (
                          <LockOpen size={15} aria-hidden="true" />
                        ) : (
                          <Lock size={15} aria-hidden="true" />
                        )}
                      </IconButton>
                    </PermissionGate>
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
