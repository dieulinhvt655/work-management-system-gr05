import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Lock, LockOpen, Pencil, UserCog } from 'lucide-react'
import { Link, Navigate, Outlet, useParams } from 'react-router-dom'
import {
  fetchDepartments,
  fetchUserById,
  fetchRoles,
  updateUser,
  updateUserRole,
  updateUserStatus,
} from '../../../api/usersApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import PermissionGate from '../../../components/common/PermissionGate'
import UserAvatar from '../../../components/common/UserAvatar'
import Button from '../../../components/ui/Button'
import ConfirmDialog from '../../../components/ui/ConfirmDialog'
import { PERMISSIONS } from '../../../constants/permissions'
import { USER_ACCOUNT_STATUS } from '../../../constants/users'
import { useAuth } from '../../../context/AuthContext'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import ChangeRoleModal from './components/ChangeRoleModal'
import EditUserModal from './components/EditUserModal'
import UserDetailTabs from './components/UserDetailTabs'
import UserStatusBadge from './components/UserStatusBadge'

const USER_QUERY_KEY = (userId) => ['admin', 'users', userId]
const DEPARTMENTS_QUERY_KEY = ['admin', 'departments']
const USERS_LIST_QUERY_KEY = ['admin', 'users']

export default function UserDetailPage() {
  const { userId } = useParams()
  const queryClient = useQueryClient()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const [showEdit, setShowEdit] = useState(false)
  const [showRole, setShowRole] = useState(false)
  const [showStatusConfirm, setShowStatusConfirm] = useState(false)
  const [actionError, setActionError] = useState('')

  const {
    data: user,
    isLoading,
    isError,
  } = useQuery({
    queryKey: USER_QUERY_KEY(userId),
    queryFn: () => fetchUserById(userId),
    enabled: isAuthenticated && !authLoading && Boolean(userId),
  })

  const { data: roles = [] } = useQuery({
    queryKey: ['admin', 'roles'],
    queryFn: fetchRoles,
    enabled: isAuthenticated && !authLoading,
  })

  const { data: departments = [] } = useQuery({
    queryKey: DEPARTMENTS_QUERY_KEY,
    queryFn: fetchDepartments,
  })

  const invalidateUser = () => {
    queryClient.invalidateQueries({ queryKey: USER_QUERY_KEY(userId) })
    queryClient.invalidateQueries({ queryKey: USERS_LIST_QUERY_KEY })
  }

  const updateUserMutation = useMutation({
    mutationFn: ({ id, payload }) => updateUser(id, payload),
    onSuccess: () => {
      setActionError('')
      setShowEdit(false)
      invalidateUser()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật người dùng.'))
    },
  })

  const updateRoleMutation = useMutation({
    mutationFn: ({ id, role }) => updateUserRole(id, role),
    onSuccess: () => {
      setActionError('')
      setShowRole(false)
      invalidateUser()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể đổi vai trò.'))
    },
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }) => updateUserStatus(id, status),
    onSuccess: () => {
      setActionError('')
      setShowStatusConfirm(false)
      invalidateUser()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật trạng thái.'))
    },
  })

  const handleSaveEdit = (id, values) => {
    setActionError('')
    updateUserMutation.mutate({
      id,
      payload: {
        ...values,
        phone: values.phone?.trim() || null,
      },
    })
  }

  const handleChangeRole = (id, role) => {
    setActionError('')
    updateRoleMutation.mutate({ id, role })
  }

  const handleToggleLock = () => {
    if (!user) return

    setActionError('')
    setShowStatusConfirm(true)
  }

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  if (isError || !user) {
    return <Navigate to="/admin/users" replace />
  }

  const isLocked = user.status === USER_ACCOUNT_STATUS.INACTIVE

  return (
    <PermissionRoute permission={PERMISSIONS.USER_READ}>
      <div className="page users-page user-detail-page">
        <header className="user-detail-page__header">
          <Link to="/admin/users" className="create-user-page__back">
            <ArrowLeft size={16} aria-hidden="true" />
            Quay lại danh sách
          </Link>

          <div className="user-detail-page__hero">
            <div className="user-detail-page__identity">
              <UserAvatar fullName={user.fullName} size="lg" />
              <div>
                <h1 className="user-detail-page__name">{user.fullName}</h1>
                <p className="user-detail-page__email">{user.email}</p>
                <div className="user-detail-page__meta">
                  <UserStatusBadge status={user.status} />
                  <span className="user-detail-page__employee-code">
                    <span className="user-detail-page__employee-code-label">Mã NV</span>
                    <code className="user-table__code">{user.employeeCode}</code>
                  </span>
                </div>
              </div>
            </div>

            <PermissionGate permission={PERMISSIONS.USER_MANAGE}>
              <div className="user-detail-page__actions">
                <Button
                  type="button"
                  variant="ghost"
                  className="user-detail-page__action-btn"
                  onClick={() => setShowEdit(true)}
                >
                  <Pencil size={16} aria-hidden="true" />
                  Chỉnh sửa
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  className="user-detail-page__action-btn"
                  onClick={() => setShowRole(true)}
                >
                  <UserCog size={16} aria-hidden="true" />
                  Đổi vai trò
                </Button>
                <Button
                  type="button"
                  variant={isLocked ? 'primary' : 'ghost'}
                  className="user-detail-page__action-btn"
                  onClick={handleToggleLock}
                  disabled={updateStatusMutation.isPending}
                >
                  {isLocked ? (
                    <LockOpen size={16} aria-hidden="true" />
                  ) : (
                    <Lock size={16} aria-hidden="true" />
                  )}
                  {isLocked ? 'Mở khóa' : 'Khóa tài khoản'}
                </Button>
              </div>
            </PermissionGate>
          </div>
        </header>

        <UserDetailTabs />

        <Outlet context={{ user }} />

        {showEdit && (
          <EditUserModal
            user={user}
            departments={departments}
            onClose={() => {
              setShowEdit(false)
              setActionError('')
            }}
            onSave={handleSaveEdit}
            isSaving={updateUserMutation.isPending}
            saveError={actionError}
          />
        )}

        {showRole && (
          <ChangeRoleModal
            user={user}
            roles={roles}
            onClose={() => {
              setShowRole(false)
              setActionError('')
            }}
            onSave={handleChangeRole}
            isSaving={updateRoleMutation.isPending}
            saveError={actionError}
          />
        )}

        {showStatusConfirm && (
          <ConfirmDialog
            title={isLocked ? 'Mở khóa tài khoản?' : 'Khóa tài khoản?'}
            description={`Bạn có chắc muốn ${
              isLocked ? 'mở khóa' : 'khóa'
            } tài khoản ${user.fullName}?`}
            confirmLabel={isLocked ? 'Mở khóa' : 'Khóa tài khoản'}
            tone={isLocked ? 'primary' : 'danger'}
            isSaving={updateStatusMutation.isPending}
            error={actionError}
            onCancel={() => {
              if (!updateStatusMutation.isPending) {
                setShowStatusConfirm(false)
                setActionError('')
              }
            }}
            onConfirm={() =>
              updateStatusMutation.mutate({
                id: user.id,
                status: isLocked
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
