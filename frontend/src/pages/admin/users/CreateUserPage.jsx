import { useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import {
  createUser,
  fetchDepartments,
  fetchUsersGroupedByWorkspace,
} from '../../../api/usersApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import Button from '../../../components/ui/Button'
import SelectField from '../../../components/ui/SelectField'
import TextField from '../../../components/ui/TextField'
import { PERMISSIONS } from '../../../constants/permissions'
import {
  CREATE_USER_STATUS_OPTIONS,
  USER_ACCOUNT_STATUS,
  USER_ROLE_OPTIONS,
} from '../../../constants/users'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import { createUserSchema } from './createUserSchema'

export default function CreateUserPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: workspaceGroups = [], isLoading: isLoadingWorkspaces } = useQuery({
    queryKey: ['admin', 'users', 'grouped-by-workspace'],
    queryFn: fetchUsersGroupedByWorkspace,
  })

  const { data: departments = [], isLoading: isLoadingDepartments } = useQuery({
    queryKey: ['admin', 'departments'],
    queryFn: fetchDepartments,
  })

  const workspaceOptions = useMemo(
    () =>
      workspaceGroups.map((group) => ({
        id: group.workspaceId,
        name: group.workspaceName,
        code: group.workspaceCode,
      })),
    [workspaceGroups],
  )

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      fullName: '',
      email: '',
      employeeCode: '',
      phone: '',
      workspaceId: '',
      departmentId: '',
      position: '',
      role: '',
      status: USER_ACCOUNT_STATUS.ACTIVE,
    },
  })

  const createUserMutation = useMutation({
    mutationFn: createUser,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['admin', 'users', 'grouped-by-workspace'],
      })
      navigate('/admin/users', {
        state: { toast: 'Tạo tài khoản thành công' },
      })
    },
  })

  const onSubmit = (values) => {
    createUserMutation.mutate({
      ...values,
      phone: values.phone?.trim() || null,
    })
  }

  if (isLoadingWorkspaces || isLoadingDepartments) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.USER_MANAGE}>
      <div className="page users-page create-user-page">
        <header className="page__header create-user-page__header">
          <Link to="/admin/users" className="create-user-page__back">
            <ArrowLeft size={16} aria-hidden="true" />
            Quay lại danh sách
          </Link>
        </header>

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
                  'Không thể tạo tài khoản.',
                )}
              </p>
            )}

            <section className="user-form-section">
              <h2 className="user-form-section__title">Thông tin cơ bản</h2>
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
                  id="create-employeeCode"
                  label="Mã nhân viên"
                  error={errors.employeeCode?.message}
                  {...register('employeeCode')}
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
              <h2 className="user-form-section__title">Thông tin tổ chức</h2>
              <div className="user-form-section__grid">
                <SelectField
                  id="create-workspaceId"
                  label="Workspace"
                  error={errors.workspaceId?.message}
                  disabled={workspaceOptions.length === 0}
                  {...register('workspaceId')}
                >
                  <option value="">
                    {workspaceOptions.length === 0
                      ? 'Chưa có workspace'
                      : 'Chọn workspace'}
                  </option>
                  {workspaceOptions.map((workspace) => (
                    <option key={workspace.id} value={workspace.id}>
                      {workspace.name}
                      {workspace.code ? ` (${workspace.code})` : ''}
                    </option>
                  ))}
                </SelectField>

                <SelectField
                  id="create-departmentId"
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
                  id="create-position"
                  label="Chức vụ"
                  placeholder="Ví dụ: Senior Developer"
                  error={errors.position?.message}
                  {...register('position')}
                />
              </div>
            </section>

            <section className="user-form-section">
              <h2 className="user-form-section__title">Phân quyền ban đầu</h2>
              <div className="user-form-section__grid">
                <SelectField
                  id="create-role"
                  label="Vai trò ban đầu"
                  error={errors.role?.message}
                  {...register('role')}
                >
                  <option value="">Chọn vai trò</option>
                  {USER_ROLE_OPTIONS.map(({ value, label }) => (
                    <option key={value} value={value}>
                      {label}
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
                {createUserMutation.isPending
                  ? 'Đang tạo...'
                  : 'Tạo tài khoản'}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </PermissionRoute>
  )
}
