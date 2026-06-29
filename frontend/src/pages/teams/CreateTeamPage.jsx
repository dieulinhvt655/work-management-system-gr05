import { useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Briefcase, ChevronRight, Info } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { createTeam } from '../../api/teamsApi'
import { fetchWorkspaceById, fetchWorkspaces } from '../../api/workspacesApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import Button from '../../components/ui/Button'
import SelectField from '../../components/ui/SelectField'
import TextField from '../../components/ui/TextField'
import {
  CREATE_TEAM_STATUS_OPTIONS,
  TEAM_STATUS,
} from '../../constants/teams'
import { WORKSPACE_STATUS } from '../../constants/workspaces'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import { useIsWorkspaceOwner } from '../../hooks/useIsWorkspaceOwner'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { createTeamSchema } from './createTeamSchema'

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

function WorkspaceStatusBadge({ status }) {
  const isActive = status === WORKSPACE_STATUS.ACTIVE

  return (
    <span
      className={`create-team-workspace-box__status${isActive ? ' create-team-workspace-box__status--active' : ''}`}
    >
      <span className="create-team-workspace-box__status-dot" aria-hidden="true" />
      {isActive ? 'Active' : status}
    </span>
  )
}

export default function CreateTeamPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const isWorkspaceOwner = useIsWorkspaceOwner()
  const isSystemAdmin = Boolean(user?.isSystemAdmin)
  const ownerWorkspaceId = user?.workspaceId ?? ''

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(createTeamSchema),
    defaultValues: {
      name: '',
      description: '',
      status: TEAM_STATUS.ACTIVE,
      workspaceId: isSystemAdmin ? '' : ownerWorkspaceId,
    },
  })

  const watchedWorkspaceId = watch('workspaceId')

  const effectiveWorkspaceId = isSystemAdmin
    ? watchedWorkspaceId
    : ownerWorkspaceId

  const { data: workspaces = [], isLoading: workspacesLoading } = useQuery({
    queryKey: ['admin', 'workspaces'],
    queryFn: fetchWorkspaces,
    enabled: isSystemAdmin,
  })

  const { data: selectedWorkspace, isLoading: workspaceLoading } = useQuery({
    queryKey: ['workspace', effectiveWorkspaceId],
    queryFn: () => fetchWorkspaceById(effectiveWorkspaceId),
    enabled: Boolean(effectiveWorkspaceId),
  })

  const activeWorkspaces = useMemo(
    () =>
      workspaces.filter(
        (workspace) => workspace.status === WORKSPACE_STATUS.ACTIVE,
      ),
    [workspaces],
  )

  const createTeamMutation = useMutation({
    mutationFn: ({ workspaceId, payload }) => createTeam(workspaceId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['teams'] })
      navigate('/teams', {
        state: { toast: 'Tạo phòng ban thành công' },
      })
    },
  })

  const onSubmit = (values) => {
    const workspaceId = isSystemAdmin ? values.workspaceId : ownerWorkspaceId

    if (!workspaceId) {
      return
    }

    createTeamMutation.mutate({
      workspaceId,
      payload: {
        name: values.name.trim(),
        description: values.description?.trim() || '',
      },
    })
  }

  if (workspacesLoading || (effectiveWorkspaceId && workspaceLoading)) {
    return <LoadingScreen />
  }

  const workspaceMissing = isSystemAdmin && !effectiveWorkspaceId

  return (
    <PermissionRoute permission={PERMISSIONS.TEAM_MANAGE}>
      <div className="page page--wide teams-page create-team-page">
        <header className="create-team-page__header">
          <nav className="create-team-page__breadcrumbs" aria-label="Breadcrumb">
            <Link to="/teams">{isWorkspaceOwner ? 'Phòng ban' : 'Teams'}</Link>
            <ChevronRight size={14} aria-hidden="true" />
            <span aria-current="page">Tạo mới</span>
          </nav>

          <Link to="/teams" className="create-team-page__back">
            <ArrowLeft size={16} aria-hidden="true" />
            Quay lại danh sách
          </Link>
        </header>

        <div className="create-team-page__intro">
          <h1>{isWorkspaceOwner ? 'Tạo phòng ban' : 'Thêm mới Phòng ban'}</h1>
          <p>
            {isWorkspaceOwner
              ? 'Thiết lập phòng ban mới trong workspace của bạn.'
              : 'Thiết lập phòng ban hoặc nhóm làm việc mới trong Workspace.'}
          </p>
        </div>

        <form
          className="create-team-page__layout"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
        >
          <section className="create-team-page__form">
            <div className="create-team-form-card">
              {isSystemAdmin ? (
                <SelectField
                  id="create-team-workspace"
                  label="Workspace *"
                  error={
                    workspaceMissing
                      ? 'Vui lòng chọn workspace'
                      : errors.workspaceId?.message
                  }
                  {...register('workspaceId', {
                    required: isSystemAdmin ? 'Vui lòng chọn workspace' : false,
                  })}
                >
                  <option value="">Chọn workspace</option>
                  {activeWorkspaces.map((workspace) => (
                    <option key={workspace.id} value={workspace.id}>
                      {workspace.name}
                    </option>
                  ))}
                </SelectField>
              ) : null}

              {selectedWorkspace ? (
                <div className="create-team-workspace-box">
                  <div className="create-team-workspace-box__icon" aria-hidden="true">
                    <Briefcase size={18} />
                  </div>
                  <div className="create-team-workspace-box__content">
                    <p className="create-team-workspace-box__name">
                      {selectedWorkspace.name}
                    </p>
                    <p className="create-team-workspace-box__meta">
                      ID: {selectedWorkspace.code}
                      {selectedWorkspace.contactEmail &&
                      selectedWorkspace.contactEmail !== '—'
                        ? ` | Email: ${selectedWorkspace.contactEmail}`
                        : ''}
                    </p>
                  </div>
                  <WorkspaceStatusBadge status={selectedWorkspace.status} />
                </div>
              ) : isSystemAdmin ? (
                <div className="create-team-workspace-box create-team-workspace-box--empty">
                  <p>Chọn workspace để xem thông tin và tạo phòng ban.</p>
                </div>
              ) : null}

              {createTeamMutation.isError && (
                <p className="create-team-form__error" role="alert">
                  {getErrorMessage(
                    createTeamMutation.error,
                    'Không thể tạo phòng ban.',
                  )}
                </p>
              )}

              <TextField
                id="create-team-name"
                label="Tên phòng ban / Team *"
                placeholder="VD: Product Department, Design Team, QA Team"
                error={errors.name?.message}
                {...register('name')}
              />

              <TextAreaField
                id="create-team-description"
                label="Mô tả"
                placeholder="Mô tả vai trò, phạm vi hoặc nhiệm vụ chính của phòng ban..."
                error={errors.description?.message}
                {...register('description')}
              />

              <SelectField
                id="create-team-status"
                label="Trạng thái ban đầu"
                error={errors.status?.message}
                {...register('status')}
              >
                {CREATE_TEAM_STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </SelectField>

              <div className="create-team-form__note" role="note">
                <Info size={16} aria-hidden="true" />
                <p>
                  <strong>Ghi chú:</strong> Team Leader / Department Manager có thể
                  được gán sau tại màn hình chi tiết phòng ban khi danh sách nhân viên
                  đã sẵn sàng.
                </p>
              </div>
            </div>

            <footer className="create-team-page__footer">
              <Link to="/teams" className="create-team-page__cancel">
                Hủy
              </Link>
              <Button
                type="submit"
                variant="primary"
                className="create-team-page__submit"
                disabled={
                  createTeamMutation.isPending ||
                  workspaceMissing ||
                  (!isSystemAdmin && !ownerWorkspaceId)
                }
              >
                {createTeamMutation.isPending ? 'Đang tạo...' : 'Tạo phòng ban'}
              </Button>
            </footer>
          </section>
        </form>
      </div>
    </PermissionRoute>
  )
}
