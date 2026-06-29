import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ChevronRight, Info, Plus } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { createWorkspace } from '../../../api/workspacesApi'
import Button from '../../../components/ui/Button'
import TextField from '../../../components/ui/TextField'
import { PERMISSIONS } from '../../../constants/permissions'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import { createWorkspaceSchema } from './createWorkspaceSchema'

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

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(createWorkspaceSchema),
    defaultValues: {
      name: '',
      description: '',
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
      name: values.name,
      description: values.description?.trim() || '',
    })
  }

  return (
    <PermissionRoute permission={PERMISSIONS.WORKSPACE_ADMIN_CREATE}>
      <div className="page page--wide workspaces-page create-workspace-page">
        <div
          className="create-workspace-page__ambient create-workspace-page__ambient--1"
          aria-hidden="true"
        />
        <div
          className="create-workspace-page__ambient create-workspace-page__ambient--2"
          aria-hidden="true"
        />

        <div className="create-workspace-page__content">
          <nav className="create-workspace-page__breadcrumbs" aria-label="Breadcrumb">
            <Link to="/admin/dashboard">Admin</Link>
            <ChevronRight size={14} aria-hidden="true" />
            <Link to="/admin/workspaces">Workspace Management</Link>
            <ChevronRight size={14} aria-hidden="true" />
            <span aria-current="page">Tạo Workspace</span>
          </nav>

          <header className="create-workspace-page__intro">
            <h1>Tạo Workspace</h1>
            <p>
              Tạo workspace độc lập, không bắt buộc tạo account cùng lúc. Sau khi
              tạo, dùng Add Accounts to Workspace để gán account vào workspace.
            </p>
            <p className="create-workspace-page__helper">
              <Info size={14} aria-hidden="true" />
              Workspace được tạo độc lập. Account sẽ được thêm sau.
            </p>
          </header>

          <form
            className="create-workspace-page__form"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            <section className="create-workspace-card">
              {createWorkspaceMutation.isError && (
                <p className="create-workspace-card__error" role="alert">
                  {getErrorMessage(
                    createWorkspaceMutation.error,
                    'Không thể tạo Workspace.',
                  )}
                </p>
              )}

              <div className="create-workspace-card__body">
                <TextField
                  id="create-workspace-name"
                  label="Tên Workspace *"
                  placeholder="Ví dụ: Công ty Tech Solutions"
                  error={errors.name?.message}
                  {...register('name')}
                />

                <TextAreaField
                  id="create-workspace-description"
                  label="Mô tả"
                  placeholder="Mô tả ngắn gọn về workspace..."
                  error={errors.description?.message}
                  {...register('description')}
                />
              </div>

              <footer className="create-workspace-card__footer">
                <Link
                  to="/admin/workspaces"
                  className="create-workspace-page__cancel"
                >
                  Hủy bỏ
                </Link>
                <Button
                  type="submit"
                  variant="primary"
                  className="create-workspace-page__submit"
                  disabled={createWorkspaceMutation.isPending}
                >
                  {createWorkspaceMutation.isPending ? (
                    'Đang tạo...'
                  ) : (
                    <>
                      <span>Tạo Workspace</span>
                      <Plus size={16} strokeWidth={2.25} aria-hidden="true" />
                    </>
                  )}
                </Button>
              </footer>
            </section>
          </form>
        </div>
      </div>
    </PermissionRoute>
  )
}
