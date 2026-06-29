import { useEffect, useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Info } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { createUser } from '../../../api/usersApi'
import { fetchCreatableRolesForAccountCreation } from '../../../api/rolesApi'
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
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import { createUserSchema } from './createUserSchema'
import { filterCreatableRoles } from './utils/creatableRoles'

export default function CreateUserPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const isWorkspaceOwner = useIsWorkspaceOwner()

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

  const roleOptions = useMemo(
    () => filterCreatableRoles(roles, { isWorkspaceOwner }),
    [roles, isWorkspaceOwner],
  )

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
      navigate('/admin/users', {
        state: { toast: 'Tạo account thành công' },
      })
    },
  })

  const onSubmit = (values) => {
    createUserMutation.mutate({
      ...values,
      roleId: Number(values.role),
      phone: values.phone?.trim() || null,
    })
  }

  if (authLoading && !user) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.USER_MANAGE}>
      <div className="page users-page create-user-page">
        <header className="page__header create-user-page__header">
          <Link to="/admin/users" className="create-user-page__back">
            <ArrowLeft size={16} aria-hidden="true" />
            Quay lại danh sách Account
          </Link>
        </header>

        <div className="create-user-page__intro">
          <h1>Tạo Account</h1>
          <p>
            Tạo tài khoản hệ thống độc lập. Gán account vào workspace thực hiện
            riêng tại màn hình Add Accounts to Workspace.
          </p>
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

            <section className="user-form-section">
              <h2 className="user-form-section__title">Vai trò & trạng thái hệ thống</h2>
              <div className="user-form-section__grid">
                <SelectField
                  id="create-role"
                  label="Vai trò hệ thống"
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
              <Link to="/admin/users" className="btn btn--ghost">
                Hủy
              </Link>
              <Button
                type="submit"
                variant="primary"
                disabled={createUserMutation.isPending}
              >
                {createUserMutation.isPending ? 'Đang tạo...' : 'Tạo Account'}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </PermissionRoute>
  )
}
