import Modal from '../../../../components/ui/Modal'
import { formatWorkspaceDate } from '../utils/formatWorkspaceDate'
import WorkspaceLogo from './WorkspaceLogo'
import WorkspaceStatusBadge from './WorkspaceStatusBadge'

function DetailRow({ label, children }) {
  return (
    <div className="workspace-detail-row">
      <dt className="workspace-detail-row__label">{label}</dt>
      <dd className="workspace-detail-row__value">{children}</dd>
    </div>
  )
}

export default function ViewWorkspaceModal({ workspace, onClose }) {
  return (
    <Modal
      title="Chi tiết Workspace"
      description="Thông tin tổ chức và liên hệ"
      onClose={onClose}
      size="md"
    >
      <div className="workspace-detail">
        <div className="workspace-detail__hero">
          <WorkspaceLogo
            name={workspace.name}
            logoUrl={workspace.logoUrl}
            size="lg"
          />
          <div>
            <h3 className="workspace-detail__name">{workspace.name}</h3>
            <p className="workspace-detail__code">{workspace.code}</p>
          </div>
        </div>

        <dl className="workspace-detail__list">
          <DetailRow label="Workspace Owner">{workspace.ownerName}</DetailRow>
          <DetailRow label="Email liên hệ">{workspace.contactEmail}</DetailRow>
          <DetailRow label="Số điện thoại">
            {workspace.contactPhone || '—'}
          </DetailRow>
          <DetailRow label="Địa chỉ">{workspace.address || '—'}</DetailRow>
          <DetailRow label="Phòng ban / Nhóm">
            {workspace.departmentCount}
          </DetailRow>
          <DetailRow label="Thành viên">{workspace.memberCount}</DetailRow>
          <DetailRow label="Trạng thái">
            <WorkspaceStatusBadge status={workspace.status} />
          </DetailRow>
          <DetailRow label="Ngày tạo">
            {formatWorkspaceDate(workspace.createdAt)}
          </DetailRow>
          <DetailRow label="Mô tả">
            {workspace.description || '—'}
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
