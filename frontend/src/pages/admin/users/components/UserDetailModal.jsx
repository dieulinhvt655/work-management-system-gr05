import Modal from '../../../../components/ui/Modal'
import UserAvatar from '../../../../components/common/UserAvatar'
import { USER_ROLE_LABELS } from '../../../../constants/users'
import { formatLastActivity } from '../utils/formatUserDate'
import UserStatusBadge from './UserStatusBadge'

function DetailRow({ label, children }) {
  return (
    <div className="user-detail-row">
      <dt className="user-detail-row__label">{label}</dt>
      <dd className="user-detail-row__value">{children}</dd>
    </div>
  )
}

export default function UserDetailModal({ user, onClose }) {
  return (
    <Modal
      title="Chi tiết người dùng"
      description="Thông tin tài khoản và hành chính"
      onClose={onClose}
      size="md"
    >
      <div className="user-detail">
        <div className="user-detail__hero">
          <UserAvatar fullName={user.fullName} size="lg" />
          <div>
            <h3 className="user-detail__name">{user.fullName}</h3>
            <p className="user-detail__email">{user.email}</p>
          </div>
        </div>

        <dl className="user-detail__list">
          <DetailRow label="Workspace">{user.workspaceName ?? '—'}</DetailRow>
          <DetailRow label="Username">{user.username || '—'}</DetailRow>
          <DetailRow label="Mã nhân viên">
            <code className="user-table__code">{user.employeeCode}</code>
          </DetailRow>
          <DetailRow label="Phòng ban / Nhóm">{user.departmentName ?? '—'}</DetailRow>
          <DetailRow label="Vai trò">
            {user.roleName ?? USER_ROLE_LABELS[user.role] ?? user.role}
          </DetailRow>
          <DetailRow label="Trạng thái">
            <UserStatusBadge status={user.status} />
          </DetailRow>
          <DetailRow label="Số điện thoại">{user.phone || '—'}</DetailRow>
          <DetailRow label="Hoạt động gần nhất">
            {formatLastActivity(user.lastActivityAt)}
          </DetailRow>
        </dl>

        <div className="modal__footer modal__footer--single">
          <button type="button" className="btn btn--ghost" onClick={onClose}>
            Đóng
          </button>
        </div>
      </div>
    </Modal>
  )
}
