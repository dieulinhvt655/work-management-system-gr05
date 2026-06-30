import { useEffect, useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Info } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { createUser } from '../../../api/usersApi'
import { fetchCreatableRolesForAccountCreation } from '../../../api/rolesApi'
import { fetchWorkspaces } from '../../../api/workspacesApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import Button from '../../../components/ui/Button'
import SelectField from '../../../components/ui/SelectField'
import TextField from '../../../components/ui/TextField'
import { PERMISSIONS } from '../../../constants/permissions'
import {
  CREATE_USER_STATUS_OPTIONS,
  USER_ACCOUNT_STATUS,
} from '../../../constants/users'
import { useAuth } from '../../../context/AuthContext'
import { useIsWorkspaceOwner } from '../../../hooks/useIsWorkspaceOwner'
import { useWorkspaceScope } from '../../../hooks/useWorkspaceScope'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import { createUserSchema } from './createUserSchema'
import { filterCreatableRoles } from './utils/creatableRoles'

export default function CreateUserPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const isWorkspaceOwner = useIsWorkspaceOwner()
  const { workspaceId: currentWorkspaceId } = useWorkspaceScope()

  const {
    data: roles = [],
    isLoading: rolesLoading,
    isError: rolesError,
    refetch: refetchRoles,
  } = useQuery({
    queryKey: ['admin', 'roles', 'creatable', isWorkspaceOwner],
    queryFn: () => fetchCreatableRolesForAccountCreation(),
    enabled: isAuthenticated && Boolean(user),
    retry: 1,
    staleTime: 60_000,
  })

  const {
    data: workspaces = [],
    isLoading: workspacesLoading,
  } = useQuery({
    queryKey: ['admin', 'workspaces'],
    queryFn: fetchWorkspaces,
    enabled: isAuthenticated && !isWorkspaceOwner,
    staleTime: 60_000,
  })

  // System Admin: Workspace option có thể chọn hoặc để trống
  // Workspace Owner: Không có option, sử dụng currentWorkspaceId
  const workspaceOptions = useMemo(() => {
    return workspaces.map((ws) => ({
      id: ws.id,
      name: ws.name,
      code: ws.code,
    }))
  }, [workspaces])

  const roleOptions = useMemo(
    () => filterCreatableRoles(roles, { isWorkspaceOwner }),
    [roles, isWorkspaceOwner],
  )

  const currentWorkspace = useMemo(() => {
    if (!isWorkspaceOwner || !currentWorkspaceId) return null
    return workspaces.find((ws) => ws.id === currentWorkspaceId)
  }, [isWorkspaceOwner, currentWorkspaceId, workspaces])

  const defaultRoleId = useMemo(() => {
    const workspaceMember = roleOptions.find(
      (role) => role.name === 'Workspace Member',
    )
    return workspaceMember ? String(workspaceMember.id) : ''
  }, [roleOptions])

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      fullName: '',
      email: '',
      username: '',
      phone: '',
      role: '',
      workspaceId: isWorkspaceOwner ? currentWorkspaceId : '',
      status: USER_ACCOUNT_STATUS.ACTIVE,
    },
  })

  useEffect(() => {
    if (!defaultRoleId) return

    reset((current) => ({
      ...current,
      role: current.role || defaultRoleId,
    }))
  }, [defaultRoleId, reset])

  const createUserMutation = useMutation({
    mutationFn: createUser,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['admin', 'users'],
      })
      if (isWorkspaceOwner) {
        // Workspace Owner: Redirect tới danh sách members của workspace
        navigate('/members', {
          state: { toast: 'Tạo account và thêm vào workspace thành công' },
        })
      } else {
        // System Admin: Redirect tới danh sách tất cả accounts
        navigate('/admin/users', {
          state: { toast: 'Tạo account thành công' },
        })
      }
    },
  })

  const onSubmit = (values) => {
    const payload = {
      ...values,
      roleId: Number(values.role),
      phone: values.phone?.trim() || null,
    }

    // Workspace Owner: Tự động thêm currentWorkspaceId
    if (isWorkspaceOwner) {
      payload.workspaceId = currentWorkspaceId
    }

    createUserMutation.mutate(payload)
  }

  if (authLoading && !user) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.USER_MANAGE}>
      <div className="page users-page create-user-page">
        <header className="page__header create-user-page__header">
          <Link
            to={isWorkspaceOwner ? '/members' : '/admin/users'}
            className="create-user-page__back"
          >
            <ArrowLeft size={16} aria-hidden="true" />
            {isWorkspaceOwner
              ? 'Quay lại danh sách thành viên'
              : 'Quay lại danh sách Account'}
          </Link>
        </header>

        <div className="create-user-page__intro">
          <h1>Tạo Account</h1>
          {isWorkspaceOwner ? (
            <>
              <p>
                Tạo tài khoản mới cho Workspace: <strong>{currentWorkspace?.name}</strong>
              </p>
              <p>
                Account sẽ được tự động thêm vào workspace hiện tại với vai trò
                bạn chọn.
              </p>
            </>
          ) : (
            <>
              <p>
                Tạo tài khoản hệ thống độc lập. Gán account vào workspace thực
                hiện riêng tại màn hình Add Accounts to Workspace.
              </p>
            </>
          )}
          <p className="create-user-page__helper">
            <Info size={14} aria-hidden="true" />
            Mã NV và mật khẩu ban đầu do hệ thống tự sinh — không cần nhập thủ công.
          </p>
        </div>

        <div className="create-user-page__card">
          <form
            className="user-form create-user-form"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            {createUserMutation.isError && (
              <p className="user-form__error" role="alert">
                {getErrorMessage(
                  createUserMutation.error,
                  'Không thể tạo account.',
                )}
              </p>
            )}

            <section className="user-form-section">
              <h2 className="user-form-section__title">Thông tin Account</h2>
              <div className="user-form-section__grid">
                <TextField
                  id="create-fullName"
                  label="Họ tên"
                  error={errors.fullName?.message}
                  {...register('fullName')}
                />

                <TextField
                  id="create-email"
                  type="email"
                  label="Email công việc"
                  error={errors.email?.message}
                  {...register('email')}
                />

                <TextField
                  id="create-username"
                  label="Username"
                  error={errors.username?.message}
                  {...register('username')}
                />

                <TextField
                  id="create-phone"
                  type="tel"
                  label="Số điện thoại"
                  placeholder="0901234567"
                  error={errors.phone?.message}
                  {...register('phone')}
                />
              </div>
            </section>

            {/* System Admin: Hiển thị dropdown Workspace */}
            {!isWorkspaceOwner && (
              <section className="user-form-section">
                <h2 className="user-form-section__title">Workspace (tùy chọn)</h2>
                <p style={{ fontSize: '0.85rem', color: '#666', marginBottom: '0.5rem' }}>
                  Để trống để tạo account global, hoặc chọn workspace để gán
                  trực tiếp.
                </p>
                <div className="user-form-section__grid">
                  <SelectField
                    id="create-workspaceId"
                    label="Workspace"
                    disabled={workspacesLoading}
                    {...register('workspaceId')}
                  >
                    <option value="">
                      {workspacesLoading
                        ? 'Đang tải workspace...'
                        : 'Không chọn (Global)'}
                    </option>
                    {workspaceOptions.map((ws) => (
                      <option key={ws.id} value={ws.id}>
                        {ws.name} ({ws.code})
                      </option>
                    ))}
                  </SelectField>
                </div>
              </section>
            )}

            <section className="user-form-section">
              <h2 className="user-form-section__title">Vai trò & trạng thái</h2>
              <div className="user-form-section__grid">
                <SelectField
                  id="create-role"
                  label={
                    isWorkspaceOwner
                      ? 'Vai trò trong Workspace'
                      : 'Vai trò hệ thống'
                  }
                  error={errors.role?.message}
                  disabled={rolesLoading}
                  {...register('role')}
                >
                  <option value="">
                    {rolesLoading
                      ? 'Đang tải vai trò...'
                      : roleOptions.length === 0
                        ? 'Không có vai trò khả dụng'
                        : 'Chọn vai trò'}
                  </option>
                  {roleOptions.map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.name}
                    </option>
                  ))}
                </SelectField>

                <SelectField
                  id="create-status"
                  label="Trạng thái tài khoản"
                  error={errors.status?.message}
                  {...register('status')}
                >
                  {CREATE_USER_STATUS_OPTIONS.map(({ value, label }) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </SelectField>
              </div>
              {rolesError && (
                <p className="user-form__error" role="alert">
                  {getErrorMessage(
                    rolesError,
                    'Không thể tải danh sách vai trò.',
                  )}{' '}
                  <button
                    type="button"
                    className="create-user-page__retry-link"
                    onClick={() => refetchRoles()}
                  >
                    Thử lại
                  </button>
                </p>
              )}
            </section>

            <div className="create-user-form__actions">
              <Link
                to={isWorkspaceOwner ? '/members' : '/admin/users'}
                className="btn btn--ghost"
              >
                Hủy
              </Link>
              <Button
                type="submit"
                variant="primary"
                disabled={createUserMutation.isPending}
              >
                {createUserMutation.isPending
                  ? 'Đang tạo...'
                  : isWorkspaceOwner
                    ? 'Tạo & Thêm vào Workspace'
                    : 'Tạo Account'}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </PermissionRoute>
  )
}
