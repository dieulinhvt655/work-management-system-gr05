import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Button from '../../../../components/ui/Button'
import Modal from '../../../../components/ui/Modal'
import TextField from '../../../../components/ui/TextField'
import { editWorkspaceSchema } from '../createWorkspaceSchema'

export default function EditWorkspaceModal({
  workspace,
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
    resolver: zodResolver(editWorkspaceSchema),
    defaultValues: {
      name: workspace.name,
      description: workspace.description ?? '',
    },
  })

  const onSubmit = (values) => {
    onSave(workspace.id, values)
  }

  return (
    <Modal
      title="Cập nhật Workspace"
      description={`Chỉnh sửa thông tin cho ${workspace.name}`}
      onClose={onClose}
      size="md"
    >
      <form className="workspace-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {saveError && (
          <p className="workspace-form__error" role="alert">
            {saveError}
          </p>
        )}

        <TextField
          id="edit-workspace-name"
          label="Tên Workspace"
          error={errors.name?.message}
          {...register('name')}
        />

        <div className="field">
          <label className="field__label" htmlFor="edit-workspace-description">
            Mô tả
          </label>
          <textarea
            id="edit-workspace-description"
            className={`field__input field__textarea${errors.description ? ' field__input--error' : ''}`}
            rows={4}
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
