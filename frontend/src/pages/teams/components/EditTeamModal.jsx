import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'
import TextField from '../../../components/ui/TextField'
import { editTeamSchema } from '../editTeamSchema'

export default function EditTeamModal({
  team,
  onClose,
  onSave,
  isSaving = false,
  saveError = '',
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(editTeamSchema),
    defaultValues: {
      name: team.name,
      description: team.description === '—' ? '' : (team.description ?? ''),
    },
  })

  const onSubmit = (values) => {
    onSave(team, {
      name: values.name,
      description: values.description?.trim() || '',
    })
  }

  return (
    <Modal
      title="Cập nhật phòng ban / nhóm"
      description={`Chỉnh sửa thông tin cho ${team.name}`}
      onClose={onClose}
      size="md"
    >
      <form className="team-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {saveError && (
          <p className="team-form__error" role="alert">
            {saveError}
          </p>
        )}

        <TextField
          id="edit-team-name"
          label="Tên phòng ban / nhóm"
          error={errors.name?.message}
          {...register('name')}
        />

        <div className="field">
          <label className="field__label" htmlFor="edit-team-description">
            Mô tả
          </label>
          <textarea
            id="edit-team-description"
            className={`field__input field__textarea${errors.description ? ' field__input--error' : ''}`}
            rows={4}
            placeholder="Mô tả ngắn về chức năng của phòng ban"
            {...register('description')}
          />
          {errors.description?.message && (
            <p className="field__error">{errors.description.message}</p>
          )}
        </div>

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
