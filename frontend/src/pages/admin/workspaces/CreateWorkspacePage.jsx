import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Info } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import {
  createWorkspace,
  fetchWorkspaceOwners,
} from '../../../api/workspacesApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import Button from '../../../components/ui/Button'
import SelectField from '../../../components/ui/SelectField'
import TextField from '../../../components/ui/TextField'
import { PERMISSIONS } from '../../../constants/permissions'
import { WORKSPACE_STATUS } from '../../../constants/workspaces'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import WorkspaceLogoUpload from './components/WorkspaceLogoUpload'
import WorkspaceStatusToggle from './components/WorkspaceStatusToggle'
import { createWorkspaceSchema } from './createWorkspaceSchema'

function FormCard({ icon: Icon, iconTone, title, children }) {
  return (
    <section className="create-workspace-card">
      <header className="create-workspace-card__header">
        <span className={`create-workspace-card__icon create-workspace-card__icon--${iconTone}`}>
          <Icon size={16} aria-hidden="true" />
        </span>
        <h2 className="create-workspace-card__title">{title}</h2>
      </header>
      <div className="create-workspace-card__body">{children}</div>
    </section>
  )
}

function TextAreaField({ id, label, error, className = '', ...props }) {
  return (
    <div className={`field${className ? ` ${className}` : ''}`}>
      {label && (
        <label className="field__label" htmlFor={id}>
          {label}
        </label>
      )}
      <textarea
        id={id}
        className={`field__input field__textarea${error ? ' field__input--error' : ''}`}
        rows={4}
        {...props}
      />
      {error && <p className="field__error">{error}</p>}
    </div>
  )
}

export default function CreateWorkspacePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: owners = [], isLoading } = useQuery({
    queryKey: ['admin', 'workspaces', 'owners'],
    queryFn: fetchWorkspaceOwners,
  })

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(createWorkspaceSchema),
    defaultValues: {
      name: '',
      code: '',
      logoUrl: '',
      description: '',
      contactEmail: '',
      contactPhone: '',
      address: '',
      ownerId: '',
      status: WORKSPACE_STATUS.ACTIVE,
    },
  })

  const createWorkspaceMutation = useMutation({
    mutationFn: createWorkspace,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'workspaces'] })
      navigate('/admin/workspaces', {
        state: { toast: 'Tạo Workspace thành công' },
      })
    },
  })

  const onSubmit = (values) => {
    createWorkspaceMutation.mutate({
      ...values,
      code: values.code.trim().toLowerCase(),
      logoUrl: values.logoUrl?.trim() || '',
      description: values.description?.trim() || '',
      contactPhone: values.contactPhone?.trim() || '',
      address: values.address?.trim() || '',
    })
  }

  if (isLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.WORKSPACE_ADMIN_CREATE}>
      <div className="page page--wide workspaces-page create-workspace-page">
        <nav className="create-workspace-page__breadcrumbs" aria-label="Breadcrumb">
          <Link to="/admin/workspaces">Hệ thống</Link>
          <span aria-hidden="true">/</span>
          <Link to="/admin/workspaces">Quản lý Workspace</Link>
          <span aria-hidden="true">/</span>
          <span aria-current="page">Tạo mới</span>
        </nav>

        <header className="create-workspace-page__intro">
          <h1>Tạo Workspace mới</h1>
          <p>
            Thiết lập không gian làm việc cho tổ chức hoặc doanh nghiệp của bạn.
          </p>
        </header>

        <form
          className="create-workspace-page__form"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
        >
          {createWorkspaceMutation.isError && (
            <p className="workspace-form__error" role="alert">
              {getErrorMessage(
                createWorkspaceMutation.error,
                'Không thể tạo Workspace.',
              )}
            </p>
          )}

          <div className="create-workspace-page__layout">
            <aside className="create-workspace-page__sidebar">
              <section className="create-workspace-card">
                <h2 className="create-workspace-card__title create-workspace-card__title--plain">
                  Hình ảnh thương hiệu
                </h2>
                <Controller
                  name="logoUrl"
                  control={control}
                  render={({ field }) => (
                    <WorkspaceLogoUpload
                      value={field.value}
                      onChange={field.onChange}
                    />
                  )}
                />
              </section>

              <section className="create-workspace-card">
                <h2 className="create-workspace-card__title create-workspace-card__title--plain">
                  Trạng thái ban đầu
                </h2>
                <Controller
                  name="status"
                  control={control}
                  render={({ field }) => (
                    <WorkspaceStatusToggle
                      value={field.value}
                      onChange={field.onChange}
                      error={errors.status?.message}
                    />
                  )}
                />
              </section>
            </aside>

            <div className="create-workspace-page__main">
              <FormCard icon={Info} iconTone="blue" title="Thông tin cơ bản">
                <div className="create-workspace-fields create-workspace-fields--2">
                  <TextField
                    id="create-workspace-name"
                    label="Tên Workspace *"
                    placeholder="Ví dụ: Công ty Tech Solutions"
                    error={errors.name?.message}
                    {...register('name')}
                  />

                  <div className="field">
                    <TextField
                      id="create-workspace-code"
                      label="Mã định danh (Slug) *"
                      placeholder="tech-solutions"
                      error={errors.code?.message}
                      {...register('code')}
                    />
                    {!errors.code?.message && (
                      <p className="field__hint">
                        Chỉ chứa chữ cái, số và dấu gạch ngang.
                      </p>
                    )}
                  </div>

                  <TextAreaField
                    id="create-workspace-description"
                    label="Mô tả Workspace"
                    placeholder="Mô tả ngắn gọn về tổ chức của bạn..."
                    className="create-workspace-fields__full"
                    error={errors.description?.message}
                    {...register('description')}
                  />

                  <SelectField
                    id="create-workspace-ownerId"
                    label="Workspace Owner *"
                    error={errors.ownerId?.message}
                    disabled={owners.length === 0}
                    className="create-workspace-fields__full"
                    {...register('ownerId')}
                  >
                    <option value="">
                      {owners.length === 0
                        ? 'Chưa có Workspace Owner'
                        : 'Chọn Workspace Owner'}
                    </option>
                    {owners.map((owner) => (
                      <option key={owner.id} value={owner.id}>
                        {owner.fullName} ({owner.email})
                      </option>
                    ))}
                  </SelectField>
                </div>
              </FormCard>

              <FormCard icon={Building2} iconTone="green" title="Liên hệ & Tổ chức">
                <div className="create-workspace-fields create-workspace-fields--2">
                  <TextField
                    id="create-workspace-contactEmail"
                    type="email"
                    label="Email liên hệ *"
                    placeholder="contact@company.com"
                    error={errors.contactEmail?.message}
                    {...register('contactEmail')}
                  />

                  <TextField
                    id="create-workspace-contactPhone"
                    type="tel"
                    label="Số điện thoại liên hệ"
                    placeholder="024 XXXX XXXX"
                    error={errors.contactPhone?.message}
                    {...register('contactPhone')}
                  />

                  <TextField
                    id="create-workspace-address"
                    label="Địa chỉ / Thông tin tổ chức"
                    placeholder="Số 123, Đường ABC, Quận XYZ, TP. Hồ Chí Minh"
                    className="create-workspace-fields__full"
                    error={errors.address?.message}
                    {...register('address')}
                  />
                </div>
              </FormCard>
            </div>
          </div>

          <footer className="create-workspace-page__footer">
            <div className="create-workspace-page__footer-actions">
              <Link
                to="/admin/workspaces"
                className="btn create-workspace-page__cancel"
              >
                Hủy bỏ
              </Link>
              <Button
                type="submit"
                variant="primary"
                className="create-workspace-page__submit"
                disabled={createWorkspaceMutation.isPending}
              >
                {createWorkspaceMutation.isPending ? 'Đang tạo...' : 'Tạo Workspace'}
              </Button>
            </div>
          </footer>
        </form>
      </div>
    </PermissionRoute>
  )
}
