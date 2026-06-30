import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  fetchDepartments,
  fetchUsers,
  updateUser,
  updateUserRole,
  updateUserStatus,
} from '../../../api/usersApi'
import { fetchRoles } from '../../../api/rolesApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import PermissionGate from '../../../components/common/PermissionGate'
import Toast from '../../../components/common/Toast'
import ConfirmDialog from '../../../components/ui/ConfirmDialog'
import { PERMISSIONS } from '../../../constants/permissions'
import { useAuth } from '../../../context/AuthContext'
import { USER_ACCOUNT_STATUS } from '../../../constants/users'
import { mapFrontendStatusToBackend } from '../../../utils/userMappers'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import PermissionRoute from '../../../routes/PermissionRoute'
import ChangeRoleModal from './components/ChangeRoleModal'
import EditUserModal from './components/EditUserModal'
import UserFilters, { FILTER_ALL } from './components/UserFilters'
import UserTable from './components/UserTable'
import { filterUsers, hasActiveUserFilters } from './utils/filterUsers'

const INITIAL_FILTERS = {
  search: '',
  role: FILTER_ALL,
  departmentId: FILTER_ALL,
  status: FILTER_ALL,
}

const USERS_QUERY_KEY = ['admin', 'users']
const DEPARTMENTS_QUERY_KEY = ['admin', 'departments']
const ROLES_QUERY_KEY = ['admin', 'roles']

export default function UsersListPage() {
  const queryClient = useQueryClient()
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [editUser, setEditUser] = useState(null)
  const [roleUser, setRoleUser] = useState(null)
  const [lockUser, setLockUser] = useState(null)
  const [actionError, setActionError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  useEffect(() => {
    if (!location.state?.toast) return

    setToastMessage(location.state.toast)
    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  const apiFilters = useMemo(
    () => ({
      keyword: filters.search.trim() || undefined,
      status: filters.status
        ? mapFrontendStatusToBackend(filters.status)
        : mapFrontendStatusToBackend(USER_ACCOUNT_STATUS.ACTIVE),
    }),
    [filters.search, filters.status],
  )

  const {
    data: users = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: [...USERS_QUERY_KEY, apiFilters],
    queryFn: () => fetchUsers(apiFilters),
    enabled: isAuthenticated && !authLoading,
  })

  const { data: departments = [] } = useQuery({
    queryKey: DEPARTMENTS_QUERY_KEY,
    queryFn: fetchDepartments,
  })

  const { data: roles = [] } = useQuery({
    queryKey: ROLES_QUERY_KEY,
    queryFn: fetchRoles,
    enabled: isAuthenticated && !authLoading,
  })

  const filteredUsers = useMemo(() => {
    const roleAndDeptFilters = {
      ...filters,
      search: '',
      status: FILTER_ALL,
    }
    return filterUsers(users, roleAndDeptFilters)
  }, [users, filters])

  const resultCount = filteredUsers.length
  const userFiltersActive = hasActiveUserFilters(filters)

  const invalidateUsers = () => {
    queryClient.invalidateQueries({ queryKey: USERS_QUERY_KEY })
  }

  const updateUserMutation = useMutation({
    mutationFn: ({ userId, payload }) => updateUser(userId, payload),
    onSuccess: () => {
      setActionError('')
      setEditUser(null)
      invalidateUsers()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật người dùng.'))
    },
  })

  const updateRoleMutation = useMutation({
    mutationFn: ({ userId, role }) => updateUserRole(userId, role),
    onSuccess: () => {
      setActionError('')
      setRoleUser(null)
      invalidateUsers()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể đổi vai trò.'))
    },
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ userId, status }) => updateUserStatus(userId, status),
    onSuccess: () => {
      setActionError('')
      setLockUser(null)
      setToastMessage('Đã cập nhật trạng thái account.')
      invalidateUsers()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật trạng thái.'))
    },
  })

  const handleSaveEdit = (userId, values) => {
    setActionError('')
    updateUserMutation.mutate({
      userId,
      payload: {
        ...values,
        phone: values.phone?.trim() || null,
      },
    })
  }

  const handleChangeRole = (userId, role) => {
    setActionError('')
    updateRoleMutation.mutate({ userId, role })
  }

  const handleToggleLock = (user) => {
    setActionError('')
    setLockUser(user)
  }

  const openDetail = (user) => {
    navigate(`/admin/users/${user.id}`)
  }

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  if (isError) {
    return (
      <PermissionRoute permission={PERMISSIONS.USER_READ}>
        <div className="page page--wide users-page">
          <div className="user-table-empty users-page__list">
            <p className="user-table-empty__title">Không thể tải danh sách</p>
            <p className="user-table-empty__text">
              {getErrorMessage(error, 'Vui lòng thử lại sau.')}
            </p>
          </div>
        </div>
      </PermissionRoute>
    )
  }

  return (
    <PermissionRoute permission={PERMISSIONS.USER_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide users-page">
        <PermissionGate permission={PERMISSIONS.USER_MANAGE}>
          <header className="page__header page__header--row page__header--actions-only users-page__header">
            <Link
              to="/admin/users/create"
              className="btn btn--primary page-header-btn users-page__cta"
            >
              <Plus size={16} aria-hidden="true" />
              Tạo Account
            </Link>
          </header>
        </PermissionGate>

        <UserFilters
          filters={filters}
          onChange={setFilters}
          resultCount={resultCount}
          departments={departments}
          roles={roles}
        />

        <div className="users-page__list">
          {filteredUsers.length === 0 ? (
            <div className="user-table-empty user-table-empty--inline">
              <p className="user-table-empty__title">
                {userFiltersActive ? 'Không tìm thấy kết quả' : 'Chưa có tài khoản'}
              </p>
              <p className="user-table-empty__text">
                {userFiltersActive
                  ? 'Thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.'
                  : 'Tài khoản đã tạo sẽ hiển thị tại đây.'}
              </p>
            </div>
          ) : (
            <UserTable
              users={filteredUsers}
              onView={openDetail}
              onEdit={setEditUser}
              onChangeRole={setRoleUser}
              onToggleLock={handleToggleLock}
            />
          )}
        </div>

        {editUser && (
          <EditUserModal
            user={editUser}
            departments={departments}
            onClose={() => {
              setEditUser(null)
              setActionError('')
            }}
            onSave={handleSaveEdit}
            isSaving={updateUserMutation.isPending}
            saveError={actionError}
          />
        )}

        {roleUser && (
          <ChangeRoleModal
            user={roleUser}
            roles={roles}
            onClose={() => {
              setRoleUser(null)
              setActionError('')
            }}
            onSave={handleChangeRole}
            isSaving={updateRoleMutation.isPending}
            saveError={actionError}
          />
        )}

        {lockUser && (
          <ConfirmDialog
            title={
              lockUser.status === USER_ACCOUNT_STATUS.INACTIVE
                ? 'Mở khóa tài khoản?'
                : 'Khóa tài khoản?'
            }
            description={`Bạn có chắc muốn ${
              lockUser.status === USER_ACCOUNT_STATUS.INACTIVE ? 'mở khóa' : 'khóa'
            } tài khoản ${lockUser.fullName}?`}
            confirmLabel={
              lockUser.status === USER_ACCOUNT_STATUS.INACTIVE
                ? 'Mở khóa'
                : 'Khóa tài khoản'
            }
            tone={lockUser.status === USER_ACCOUNT_STATUS.INACTIVE ? 'primary' : 'danger'}
            isSaving={updateStatusMutation.isPending}
            error={actionError}
            onCancel={() => {
              if (!updateStatusMutation.isPending) {
                setLockUser(null)
                setActionError('')
              }
            }}
            onConfirm={() =>
              updateStatusMutation.mutate({
                userId: lockUser.id,
                status:
                  lockUser.status === USER_ACCOUNT_STATUS.INACTIVE
                    ? USER_ACCOUNT_STATUS.ACTIVE
                    : USER_ACCOUNT_STATUS.INACTIVE,
              })
            }
          />
        )}
      </div>
    </PermissionRoute>
  )
}
