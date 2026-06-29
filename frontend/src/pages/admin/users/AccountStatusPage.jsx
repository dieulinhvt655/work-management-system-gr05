import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { fetchUsers, updateUserStatus } from '../../../api/usersApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import Toast from '../../../components/common/Toast'
import { PERMISSIONS } from '../../../constants/permissions'
import { useAuth } from '../../../context/AuthContext'
import {
  USER_ACCOUNT_STATUS,
  USER_STATUS_LABELS,
} from '../../../constants/users'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import UserTable from './components/UserTable'

const STATUS_TABS = [
  { value: '', label: 'Tất cả' },
  ...Object.entries(USER_STATUS_LABELS).map(([value, label]) => ({
    value,
    label,
  })),
]

export default function AccountStatusPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const [statusFilter, setStatusFilter] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  const { data: users = [], isLoading } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: fetchUsers,
    enabled: isAuthenticated && !authLoading,
  })

  const filteredUsers = useMemo(() => {
    if (!statusFilter) return users
    return users.filter((user) => user.status === statusFilter)
  }, [users, statusFilter])

  const statusCounts = useMemo(() => {
    return Object.values(USER_ACCOUNT_STATUS).reduce((counts, status) => {
      counts[status] = users.filter((user) => user.status === status).length
      return counts
    }, {})
  }, [users])

  const updateStatusMutation = useMutation({
    mutationFn: ({ userId, status }) => updateUserStatus(userId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      setToastMessage('Đã cập nhật trạng thái account.')
    },
    onError: (error) => {
      window.alert(getErrorMessage(error, 'Không thể cập nhật trạng thái.'))
    },
  })

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

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.USER_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide users-page account-status-page">
        <header className="account-status-page__header">
          <nav className="account-status-page__breadcrumbs" aria-label="Breadcrumb">
            <Link to="/admin/dashboard">Admin</Link>
            <span aria-hidden="true">/</span>
            <span aria-current="page">Trạng thái Account</span>
          </nav>
          <h1 className="account-status-page__title">Trạng thái Account</h1>
          <p className="account-status-page__subtitle">
            Theo dõi và quản lý trạng thái tài khoản hệ thống.
          </p>
        </header>

        <div className="account-status-page__summary">
          {Object.entries(USER_STATUS_LABELS).map(([status, label]) => (
            <button
              key={status}
              type="button"
              className={`account-status-chip${statusFilter === status ? ' account-status-chip--active' : ''}`}
              onClick={() =>
                setStatusFilter((current) => (current === status ? '' : status))
              }
            >
              <span>{label}</span>
              <strong>{statusCounts[status] ?? 0}</strong>
            </button>
          ))}
        </div>

        <div className="account-status-page__tabs" role="tablist" aria-label="Lọc trạng thái">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value || 'all'}
              type="button"
              role="tab"
              aria-selected={statusFilter === tab.value}
              className={`account-status-page__tab${statusFilter === tab.value ? ' account-status-page__tab--active' : ''}`}
              onClick={() => setStatusFilter(tab.value)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {filteredUsers.length === 0 ? (
          <div className="user-table-empty">
            <p className="user-table-empty__title">Không có account</p>
            <p className="user-table-empty__text">
              Không tìm thấy account với trạng thái đã chọn.
            </p>
          </div>
        ) : (
          <UserTable
            users={filteredUsers}
            onView={(user) => navigate(`/admin/users/${user.id}/account-status`)}
            onEdit={() => {}}
            onChangeRole={() => {}}
            onToggleLock={handleToggleLock}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
