import { useState } from 'react'
import Button from '../../../../components/ui/Button'
import Modal from '../../../../components/ui/Modal'
import SelectField from '../../../../components/ui/SelectField'

export default function ChangeRoleModal({
  user,
  roles = [],
  onClose,
  onSave,
  isSaving = false,
  saveError = '',
}) {
  const [roleId, setRoleId] = useState(String(user.roleId ?? ''))
  const [error, setError] = useState('')

  const handleSubmit = (event) => {
    event.preventDefault()
    if (!roleId) {
      setError('Vui lòng chọn vai trò')
      return
    }
    onSave(user.id, roleId)
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
          value={roleId}
          onChange={(event) => {
            setRoleId(event.target.value)
            setError('')
          }}
        >
          {roles.map((role) => (
            <option key={role.id} value={role.id}>
              {role.name}
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
