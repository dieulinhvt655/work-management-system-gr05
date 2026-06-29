import { useEffect, useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Button from '../../../../components/ui/Button'
import Modal from '../../../../components/ui/Modal'
import SelectField from '../../../../components/ui/SelectField'
import TextField from '../../../../components/ui/TextField'
import { ROLE_SCOPE, ROLE_SCOPE_OPTIONS } from '../../../../constants/roles'
import { groupPermissionsByModule } from '../../../../utils/roleMappers'
import { roleFormSchema } from '../roleFormSchema'

function TextAreaField({ id, label, error, ...props }) {
  return (
    <div className="field">
      {label && (
        <label className="field__label" htmlFor={id}>
          {label}
        </label>
      )}
      <textarea
        id={id}
        className={`field__input field__textarea${error ? ' field__input--error' : ''}`}
        rows={3}
        {...props}
      />
      {error && <p className="field__error">{error}</p>}
    </div>
  )
}

export default function RoleFormModal({
  role,
  permissions = [],
  lockedScope = null,
  onClose,
  onSave,
  isSaving = false,
  saveError = '',
}) {
  const isEdit = Boolean(role)
  const permissionGroups = useMemo(
    () => groupPermissionsByModule(permissions),
    [permissions],
  )

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(roleFormSchema),
    defaultValues: {
      name: role?.name ?? '',
      description: role?.description ?? '',
      scope: role?.scope ?? lockedScope ?? ROLE_SCOPE.WORKSPACE,
      permissionIds: role?.permissionIds ?? [],
    },
  })

  const selectedPermissionIds = watch('permissionIds') ?? []

  useEffect(() => {
    if (!role) return

    setValue('name', role.name)
    setValue('description', role.description ?? '')
    setValue('scope', role.scope)
    setValue('permissionIds', role.permissionIds ?? [])
  }, [role, setValue])

  const togglePermission = (permissionId) => {
    const next = selectedPermissionIds.includes(permissionId)
      ? selectedPermissionIds.filter((id) => id !== permissionId)
      : [...selectedPermissionIds, permissionId]

    setValue('permissionIds', next, { shouldValidate: true })
  }

  const toggleModule = (modulePermissions, checked) => {
    const moduleIds = modulePermissions.map((permission) => permission.id)
    const withoutModule = selectedPermissionIds.filter(
      (id) => !moduleIds.includes(id),
    )
    const next = checked
      ? [...new Set([...withoutModule, ...moduleIds])]
      : withoutModule

    setValue('permissionIds', next, { shouldValidate: true })
  }

  return (
    <Modal
      title={isEdit ? 'Chỉnh sửa vai trò' : 'Tạo vai trò mới'}
      description={
        isEdit
          ? `Cập nhật thông tin và quyền cho vai trò "${role.name}".`
          : 'Tạo vai trò mới và gán quyền truy cập.'
      }
      onClose={onClose}
      size="lg"
    >
      <form
        className="role-form"
        onSubmit={handleSubmit(onSave)}
        noValidate
      >
        {saveError && (
          <p className="role-form__error" role="alert">
            {saveError}
          </p>
        )}

        <div className="role-form__grid">
          <TextField
            id="role-name"
            label="Tên vai trò *"
            placeholder="VD: Workspace Member"
            error={errors.name?.message}
            {...register('name')}
          />

          <SelectField
            id="role-scope"
            label="Phạm vi *"
            error={errors.scope?.message}
            disabled={Boolean(lockedScope)}
            {...register('scope')}
          >
            {(lockedScope
              ? ROLE_SCOPE_OPTIONS.filter(({ value }) => value === lockedScope)
              : ROLE_SCOPE_OPTIONS
            ).map(({ value, label }) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </SelectField>

          <div className="role-form__full">
            <TextAreaField
              id="role-description"
              label="Mô tả"
              placeholder="Mô tả ngắn về vai trò..."
              error={errors.description?.message}
              {...register('description')}
            />
          </div>
        </div>

        <section className="role-form__permissions">
          <div className="role-form__permissions-header">
            <h3>Quyền truy cập</h3>
            <span>{selectedPermissionIds.length} đã chọn</span>
          </div>

          {permissionGroups.length === 0 ? (
            <p className="role-form__permissions-empty">Chưa có quyền trên hệ thống.</p>
          ) : (
            <div className="role-form__permission-groups">
              {permissionGroups.map(({ module, permissions: modulePermissions }) => {
                const moduleIds = modulePermissions.map((permission) => permission.id)
                const selectedCount = moduleIds.filter((id) =>
                  selectedPermissionIds.includes(id),
                ).length
                const allSelected = selectedCount === moduleIds.length

                return (
                  <div key={module} className="role-permission-group">
                    <label className="role-permission-group__header">
                      <input
                        type="checkbox"
                        checked={allSelected}
                        onChange={(event) =>
                          toggleModule(modulePermissions, event.target.checked)
                        }
                      />
                      <span className="role-permission-group__title">{module}</span>
                      <span className="role-permission-group__count">
                        {selectedCount}/{moduleIds.length}
                      </span>
                    </label>

                    <ul className="role-permission-group__list">
                      {modulePermissions.map((permission) => (
                        <li key={permission.id}>
                          <label className="role-permission-item">
                            <input
                              type="checkbox"
                              checked={selectedPermissionIds.includes(permission.id)}
                              onChange={() => togglePermission(permission.id)}
                            />
                            <span className="role-permission-item__content">
                              <span className="role-permission-item__name">
                                {permission.name}
                              </span>
                              <span className="role-permission-item__code">
                                {permission.code}
                              </span>
                            </span>
                          </label>
                        </li>
                      ))}
                    </ul>
                  </div>
                )
              })}
            </div>
          )}
        </section>

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang lưu...' : isEdit ? 'Lưu thay đổi' : 'Tạo vai trò'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
