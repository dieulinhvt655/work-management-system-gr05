import { useState } from 'react'
import Button from '../../../../components/ui/Button'
import Modal from '../../../../components/ui/Modal'
import SelectField from '../../../../components/ui/SelectField'
import { USER_ROLE_OPTIONS } from '../../../../constants/users'

export default function ChangeRoleModal({
  user,
  onClose,
  onSave,
  isSaving = false,
  saveError = '',
}) {
  const [role, setRole] = useState(user.role)
  const [error, setError] = useState('')

  const handleSubmit = (event) => {
    event.preventDefault()
    if (!role) {
      setError('Vui lòng chọn vai trò')
      return
    }
    onSave(user.id, role)
  }

  return (
    <Modal
      title="Đổi vai trò"
      description={`Thay đổi vai trò hệ thống cho ${user.fullName}`}
      onClose={onClose}
      size="sm"
    >
      <form className="user-form" onSubmit={handleSubmit} noValidate>
        {(saveError || error) && (
          <p className="user-form__error" role="alert">
            {saveError || error}
          </p>
        )}

        <SelectField
          id="change-role"
          label="Vai trò mới"
          value={role}
          onChange={(event) => {
            setRole(event.target.value)
            setError('')
          }}
        >
          {USER_ROLE_OPTIONS.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>

        <p className="user-form__hint">
          Thay đổi vai trò sẽ cập nhật quyền truy cập của người dùng trong hệ
          thống.
        </p>

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang lưu...' : 'Xác nhận'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
