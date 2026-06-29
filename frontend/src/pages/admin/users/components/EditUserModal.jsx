import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Button from '../../../../components/ui/Button'
import Modal from '../../../../components/ui/Modal'
import SelectField from '../../../../components/ui/SelectField'
import TextField from '../../../../components/ui/TextField'
import { editUserSchema } from '../usersListSchema'

export default function EditUserModal({
  user,
  departments = [],
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
    resolver: zodResolver(editUserSchema),
    defaultValues: {
      fullName: user.fullName,
      email: user.email,
      username: user.username ?? '',
      departmentId: user.departmentId ?? '',
      phone: user.phone ?? '',
    },
  })

  const onSubmit = (values) => {
    onSave(user.id, values)
  }

  return (
    <Modal
      title="Chỉnh sửa thông tin hành chính"
      description={`Cập nhật thông tin cho ${user.fullName}`}
      onClose={onClose}
      size="md"
    >
      <form className="user-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {saveError && (
          <p className="user-form__error" role="alert">
            {saveError}
          </p>
        )}

        <TextField
          id="edit-fullName"
          label="Họ tên"
          error={errors.fullName?.message}
          {...register('fullName')}
        />

        <TextField
          id="edit-email"
          type="email"
          label="Email công việc"
          error={errors.email?.message}
          {...register('email')}
        />

        <TextField
          id="edit-username"
          label="Username"
          error={errors.username?.message}
          {...register('username')}
        />

        <SelectField
          id="edit-departmentId"
          label="Phòng ban / Nhóm"
          error={errors.departmentId?.message}
          disabled={departments.length === 0}
          {...register('departmentId')}
        >
          <option value="">
            {departments.length === 0
              ? 'Chưa có dữ liệu phòng ban'
              : 'Chọn phòng ban / nhóm'}
          </option>
          {departments.map((dept) => (
            <option key={dept.id} value={dept.id}>
              {dept.name}
            </option>
          ))}
        </SelectField>

        <TextField
          id="edit-phone"
          type="tel"
          label="Số điện thoại"
          placeholder="0901234567"
          error={errors.phone?.message}
          {...register('phone')}
        />

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
