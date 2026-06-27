import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  fetchDepartments,
  fetchUsersGroupedByWorkspace,
  updateUser,
  updateUserRole,
  updateUserStatus,
} from '../../../api/usersApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import PermissionGate from '../../../components/common/PermissionGate'
import Toast from '../../../components/common/Toast'
import { PERMISSIONS } from '../../../constants/permissions'
import { USER_ACCOUNT_STATUS } from '../../../constants/users'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import PermissionRoute from '../../../routes/PermissionRoute'
import ChangeRoleModal from './components/ChangeRoleModal'
import EditUserModal from './components/EditUserModal'
import UserFilters, { FILTER_ALL } from './components/UserFilters'
import WorkspaceUsersSection from './components/WorkspaceUsersSection'
import {
  getFilteredWorkspaceGroup,
  hasActiveUserFilters,
} from './utils/filterUsers'

const INITIAL_FILTERS = {
  workspaceId: '',
  search: '',
  role: FILTER_ALL,
  departmentId: FILTER_ALL,
  status: FILTER_ALL,
}

const USERS_QUERY_KEY = ['admin', 'users', 'grouped-by-workspace']
const DEPARTMENTS_QUERY_KEY = ['admin', 'departments']

export default function UsersListPage() {
  const queryClient = useQueryClient()
  const location = useLocation()
  const navigate = useNavigate()
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [editUser, setEditUser] = useState(null)
  const [roleUser, setRoleUser] = useState(null)
  const [actionError, setActionError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  useEffect(() => {
    if (!location.state?.toast) return

    setToastMessage(location.state.toast)
    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  const { data: workspaceGroups = [], isLoading } = useQuery({
    queryKey: USERS_QUERY_KEY,
    queryFn: fetchUsersGroupedByWorkspace,
  })

  const { data: departments = [] } = useQuery({
    queryKey: DEPARTMENTS_QUERY_KEY,
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

  useEffect(() => {
    if (workspaceGroups.length === 0) return

    const exists = workspaceGroups.some(
      (group) => group.workspaceId === filters.workspaceId,
    )

    if (!filters.workspaceId || !exists) {
      setFilters((current) => ({
        ...current,
        workspaceId: workspaceGroups[0].workspaceId,
      }))
    }
  }, [workspaceGroups, filters.workspaceId])

  const selectedGroup = useMemo(
    () => getFilteredWorkspaceGroup(workspaceGroups, filters),
    [workspaceGroups, filters],
  )

  const resultCount = selectedGroup?.users.length ?? 0
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
    onSuccess: invalidateUsers,
    onError: (error) => {
      window.alert(getErrorMessage(error, 'Không thể cập nhật trạng thái.'))
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
    const nextStatus =
      user.status === USER_ACCOUNT_STATUS.LOCKED
        ? USER_ACCOUNT_STATUS.ACTIVE
        : USER_ACCOUNT_STATUS.LOCKED

    const label =
      nextStatus === USER_ACCOUNT_STATUS.LOCKED ? 'khóa' : 'mở khóa'

    const confirmed = window.confirm(
      `Bạn có chắc muốn ${label} tài khoản "${user.fullName}"?`,
    )

    if (!confirmed) return

    updateStatusMutation.mutate({ userId: user.id, status: nextStatus })
  }

  const openDetail = (user) => {
    navigate(`/admin/users/${user.id}`)
  }

  if (isLoading) {
    return <LoadingScreen />
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
              Tạo tài khoản
            </Link>
          </header>
        </PermissionGate>

        <UserFilters
          filters={filters}
          onChange={setFilters}
          resultCount={resultCount}
          departments={departments}
          workspaces={workspaceOptions}
        />

        {workspaceGroups.length === 0 ? (
          <div className="user-table-empty users-page__list">
            <p className="user-table-empty__title">Chưa có workspace</p>
            <p className="user-table-empty__text">
              Dữ liệu người dùng sẽ hiển thị khi có workspace.
            </p>
          </div>
        ) : selectedGroup ? (
          <div className="users-page__list">
            <WorkspaceUsersSection
            workspaceId={selectedGroup.workspaceId}
            workspaceName={selectedGroup.workspaceName}
            workspaceCode={selectedGroup.workspaceCode}
            users={selectedGroup.users}
            hasActiveFilters={userFiltersActive}
            onView={openDetail}
            onEdit={setEditUser}
            onChangeRole={setRoleUser}
            onToggleLock={handleToggleLock}
            />
          </div>
        ) : null}

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
            onClose={() => {
              setRoleUser(null)
              setActionError('')
            }}
            onSave={handleChangeRole}
            isSaving={updateRoleMutation.isPending}
            saveError={actionError}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
