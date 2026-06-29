import { useState } from 'react'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'
import PasswordField from '../../../components/ui/PasswordField'
import { changePasswordSchema } from '../profileSchema'

const INITIAL_VALUES = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
}

export default function ChangePasswordModal({
  onClose,
  onSubmit,
  isSaving = false,
  serverError = '',
}) {
  const [values, setValues] = useState(INITIAL_VALUES)
  const [errors, setErrors] = useState({})

  const set = (key, value) => {
    setErrors((current) => ({ ...current, [key]: '' }))
    setValues((current) => ({ ...current, [key]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    const result = changePasswordSchema.safeParse(values)
    if (!result.success) {
      const fieldErrors = {}
      for (const issue of result.error.issues) {
        const field = issue.path[0]
        if (field && !fieldErrors[field]) {
          fieldErrors[field] = issue.message
        }
      }
      setErrors(fieldErrors)
      return
    }

    onSubmit({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    })
  }

  return (
    <Modal
      title="Đổi mật khẩu"
      description="Nhập mật khẩu hiện tại và mật khẩu mới để cập nhật."
      onClose={onClose}
      size="sm"
    >
      <form className="profile-form" onSubmit={handleSubmit} noValidate>
        {serverError && (
          <p className="profile-form__error" role="alert">
            {serverError}
          </p>
        )}

        <PasswordField
          id="current-password"
          label="Mật khẩu hiện tại"
          value={values.currentPassword}
          onChange={(event) => set('currentPassword', event.target.value)}
          error={errors.currentPassword}
          autoComplete="current-password"
        />

        <PasswordField
          id="new-password"
          label="Mật khẩu mới"
          value={values.newPassword}
          onChange={(event) => set('newPassword', event.target.value)}
          error={errors.newPassword}
          autoComplete="new-password"
        />

        <PasswordField
          id="confirm-password"
          label="Xác nhận mật khẩu mới"
          value={values.confirmPassword}
          onChange={(event) => set('confirmPassword', event.target.value)}
          error={errors.confirmPassword}
          autoComplete="new-password"
        />

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang lưu...' : 'Cập nhật mật khẩu'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
