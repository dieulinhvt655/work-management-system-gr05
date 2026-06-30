import { useOutletContext } from 'react-router-dom'
import { USER_ROLE_LABELS } from '../../../../constants/users'
import { formatLastActivity } from '../utils/formatUserDate'
import UserDetailRow from '../components/UserDetailRow'
import UserStatusBadge from '../components/UserStatusBadge'

export default function UserGeneralTab() {
  const { user } = useOutletContext()

  return (
    <section className="user-detail-tab">
      <div className="user-detail-tab__card">
        <dl className="user-detail__list user-detail__list--grid">
          <UserDetailRow label="Email">{user.email}</UserDetailRow>
          <UserDetailRow label="Username">{user.username || '—'}</UserDetailRow>
          <UserDetailRow label="Mã nhân viên">
            <code className="user-table__code">{user.employeeCode}</code>
          </UserDetailRow>
          <UserDetailRow label="Phòng ban / Nhóm">
            {user.departmentName ?? '—'}
          </UserDetailRow>
          <UserDetailRow label="Workspace">
            {user.workspaceName ?? '—'}
          </UserDetailRow>
          <UserDetailRow label="Chức vụ">{user.position ?? '—'}</UserDetailRow>
          <UserDetailRow label="Số điện thoại">{user.phone || '—'}</UserDetailRow>
          <UserDetailRow label="Vai trò hiện tại">
            {user.roleName ?? USER_ROLE_LABELS[user.role] ?? user.role}
          </UserDetailRow>
          <UserDetailRow label="Trạng thái tài khoản">
            <UserStatusBadge status={user.status} />
          </UserDetailRow>
          <UserDetailRow label="Ngày tạo">
            {formatLastActivity(user.createdAt)}
          </UserDetailRow>
          <UserDetailRow label="Lần đăng nhập gần nhất">
            {formatLastActivity(user.lastLoginAt)}
          </UserDetailRow>
        </dl>
      </div>
    </section>
  )
}
