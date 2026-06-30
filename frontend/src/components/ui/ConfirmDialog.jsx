import Button from './Button'
import Modal from './Modal'

export default function ConfirmDialog({
  title,
  description,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy',
  tone = 'danger',
  isSaving = false,
  error = '',
  onCancel,
  onConfirm,
}) {
  return (
    <Modal title={title} description={description} onClose={onCancel} size="sm">
      {error && (
        <p className="modal__error" role="alert">
          {error}
        </p>
      )}
      <div className="modal__footer">
        <Button
          type="button"
          variant="ghost"
          onClick={onCancel}
          disabled={isSaving}
        >
          {cancelLabel}
        </Button>
        <Button
          type="button"
          variant={tone}
          onClick={onConfirm}
          disabled={isSaving}
        >
          {isSaving ? 'Đang xử lý...' : confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}
