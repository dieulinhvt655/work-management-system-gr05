import { AlertTriangle } from 'lucide-react'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'

export default function DisbandTeamModal({
  team,
  onClose,
  onConfirm,
  isSaving = false,
  saveError = '',
}) {
  const memberCount = team.memberCount ?? team.members?.length ?? 0

  return (
    <Modal
      title="Giải thể phòng ban / nhóm"
      description={team.name}
      onClose={onClose}
      size="sm"
    >
      <div className="disband-team-modal">
        {saveError && (
          <p className="team-form__error" role="alert">
            {saveError}
          </p>
        )}

        <div className="disband-team-modal__warning">
          <AlertTriangle size={20} aria-hidden="true" />
          <p>
            Phòng ban sẽ chuyển sang trạng thái <strong>Đã giải thể</strong> và
            không thể gán nhân viên mới. Thao tác này không xóa dữ liệu lịch sử.
          </p>
        </div>

        {memberCount > 0 && (
          <p className="disband-team-modal__note">
            Phòng ban hiện có <strong>{memberCount}</strong> nhân viên. Họ vẫn
            giữ tư cách thành viên workspace sau khi giải thể.
          </p>
        )}

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button
            type="button"
            variant="danger"
            onClick={() => onConfirm(team)}
            disabled={isSaving}
          >
            {isSaving ? 'Đang giải thể...' : 'Xác nhận giải thể'}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
