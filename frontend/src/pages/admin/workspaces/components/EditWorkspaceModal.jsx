import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Button from '../../../../components/ui/Button'
import Modal from '../../../../components/ui/Modal'
import SelectField from '../../../../components/ui/SelectField'
import TextField from '../../../../components/ui/TextField'
import { editWorkspaceSchema } from '../createWorkspaceSchema'

export default function EditWorkspaceModal({
  workspace,
  owners = [],
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
      code: workspace.code,
      logoUrl: workspace.logoUrl ?? '',
      description: workspace.description ?? '',
      contactEmail: workspace.contactEmail,
      contactPhone: workspace.contactPhone ?? '',
      address: workspace.address ?? '',
      ownerId: workspace.ownerId,
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

        <TextField
          id="edit-workspace-code"
          label="Mã định danh Workspace"
          error={errors.code?.message}
          {...register('code')}
        />

        <TextField
          id="edit-workspace-logo"
          label="Logo Workspace (URL)"
          placeholder="https://..."
          error={errors.logoUrl?.message}
          {...register('logoUrl')}
        />

        <TextField
          id="edit-workspace-description"
          label="Mô tả"
          error={errors.description?.message}
          {...register('description')}
        />

        <TextField
          id="edit-workspace-contactEmail"
          type="email"
          label="Email liên hệ"
          error={errors.contactEmail?.message}
          {...register('contactEmail')}
        />

        <TextField
          id="edit-workspace-contactPhone"
          type="tel"
          label="Số điện thoại liên hệ"
          error={errors.contactPhone?.message}
          {...register('contactPhone')}
        />

        <TextField
          id="edit-workspace-address"
          label="Địa chỉ / thông tin tổ chức"
          error={errors.address?.message}
          {...register('address')}
        />

        <SelectField
          id="edit-workspace-ownerId"
          label="Workspace Owner"
          error={errors.ownerId?.message}
          disabled={owners.length === 0}
          {...register('ownerId')}
        >
          <option value="">
            {owners.length === 0 ? 'Chưa có owner' : 'Chọn Workspace Owner'}
          </option>
          {owners.map((owner) => (
            <option key={owner.id} value={owner.id}>
              {owner.fullName} ({owner.email})
            </option>
          ))}
        </SelectField>

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
