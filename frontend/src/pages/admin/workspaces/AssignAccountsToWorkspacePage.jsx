import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronRight, Search, UserPlus, UserSearch } from 'lucide-react'
import { Link } from 'react-router-dom'
import {
  assignAccountsToWorkspace,
  fetchWorkspaceMemberUserIds,
} from '../../../api/workspaceMembersApi'
import { fetchRoles, fetchUsers } from '../../../api/usersApi'
import { fetchWorkspaces } from '../../../api/workspacesApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import Toast from '../../../components/common/Toast'
import UserAvatar from '../../../components/common/UserAvatar'
import Button from '../../../components/ui/Button'
import SelectField from '../../../components/ui/SelectField'
import { WORKSPACE_STATUS } from '../../../constants/workspaces'
import { PERMISSIONS } from '../../../constants/permissions'
import { USER_ROLE_LABELS } from '../../../constants/users'
import { useAuth } from '../../../context/AuthContext'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'

const WORKSPACE_MEMBER_ROLE_NAMES = [
  'Workspace Member',
  'Workspace Owner',
  'Team Leader',
  'Team Member',
  'Project Manager',
  'Project Contributor',
]

export default function AssignAccountsToWorkspacePage() {
  const queryClient = useQueryClient()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const [workspaceId, setWorkspaceId] = useState('')
  const [roleId, setRoleId] = useState('')
  const [search, setSearch] = useState('')
  const [selectedUserIds, setSelectedUserIds] = useState([])
  const [toastMessage, setToastMessage] = useState('')
  const [formError, setFormError] = useState('')
  const assignedUsersQueryKey = [
    'admin',
    'workspace-member-user-ids',
    workspaceId,
  ]

  const { data: workspaces = [], isLoading: workspacesLoading } = useQuery({
    queryKey: ['admin', 'workspaces'],
    queryFn: fetchWorkspaces,
  })

  const { data: users = [], isLoading: usersLoading } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: fetchUsers,
  })

  const { data: roles = [], isLoading: rolesLoading } = useQuery({
    queryKey: ['admin', 'roles'],
    queryFn: fetchRoles,
    enabled: isAuthenticated && !authLoading,
  })

  const { data: assignedUserIds = new Set(), isLoading: membersLoading } =
    useQuery({
      queryKey: assignedUsersQueryKey,
      queryFn: () => fetchWorkspaceMemberUserIds(workspaceId),
      enabled: Boolean(workspaceId),
    })

  const assignableWorkspaces = useMemo(
    () =>
      workspaces.filter(
        (workspace) => workspace.status === WORKSPACE_STATUS.ACTIVE,
      ),
    [workspaces],
  )

  const workspaceRoleOptions = useMemo(
    () =>
      roles.filter((role) => WORKSPACE_MEMBER_ROLE_NAMES.includes(role.name)),
    [roles],
  )

  const availableUsers = useMemo(() => {
    const query = search.trim().toLowerCase()

    return users.filter((user) => {
      if (assignedUserIds.has(String(user.id))) return false
      if (!query) return true

      return [user.fullName, user.email, user.employeeCode, user.username]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(query)
    })
  }, [users, assignedUserIds, search])

  const assignMutation = useMutation({
    mutationFn: ({ workspaceId: wsId, userIds, roleId: selectedRoleId }) =>
      assignAccountsToWorkspace(wsId, {
        userIds,
        roleId: Number(selectedRoleId),
      }),
    onSuccess: (results, variables) => {
      const successCount = results.filter((item) => item.success).length
      const failedCount = results.length - successCount
      const successfulUserIds = results
        .filter((item) => item.success)
        .map((item) => String(item.userId))

      setSelectedUserIds([])
      setFormError('')
      queryClient.setQueryData(
        ['admin', 'workspace-member-user-ids', variables.workspaceId],
        (current = new Set()) =>
          new Set([...current].map(String).concat(successfulUserIds)),
      )
      queryClient.invalidateQueries({
        queryKey: ['admin', 'workspace-member-user-ids', variables.workspaceId],
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'members'] })

      if (failedCount === 0) {
        setToastMessage(`Đã thêm ${successCount} account vào workspace.`)
      } else {
        setToastMessage(
          `Thêm thành công ${successCount} account, thất bại ${failedCount}.`,
        )
      }
    },
    onError: (error) => {
      setFormError(getErrorMessage(error, 'Không thể gán account vào workspace.'))
    },
  })

  const toggleUser = (userId) => {
    const normalizedUserId = String(userId)
    setSelectedUserIds((current) =>
      current.includes(normalizedUserId)
        ? current.filter((id) => id !== normalizedUserId)
        : [...current, normalizedUserId],
    )
  }

  const toggleAllVisible = () => {
    const visibleIds = availableUsers.map((user) => String(user.id))
    const allSelected = visibleIds.every((id) => selectedUserIds.includes(id))

    if (allSelected) {
      setSelectedUserIds((current) =>
        current.filter((id) => !visibleIds.includes(id)),
      )
      return
    }

    setSelectedUserIds((current) => [...new Set([...current, ...visibleIds])])
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    setFormError('')

    if (!workspaceId) {
      setFormError('Vui lòng chọn workspace.')
      return
    }

    if (!roleId) {
      setFormError('Vui lòng chọn vai trò trong workspace.')
      return
    }

    if (selectedUserIds.length === 0) {
      setFormError('Vui lòng chọn ít nhất một account.')
      return
    }

    assignMutation.mutate({
      workspaceId,
      userIds: selectedUserIds,
      roleId,
    })
  }

  if (workspacesLoading || usersLoading || rolesLoading) {
    return <LoadingScreen />
  }

  const visibleAllSelected =
    availableUsers.length > 0 &&
    availableUsers.every((user) => selectedUserIds.includes(String(user.id)))

  const showEmptyState = !workspaceId || membersLoading || availableUsers.length === 0

  return (
    <PermissionRoute permission={PERMISSIONS.WORKSPACE_ADMIN_MANAGE}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide assign-accounts-page">
        <header className="assign-accounts-page__header">
          <nav className="assign-accounts-page__breadcrumbs" aria-label="Breadcrumb">
            <Link to="/admin/dashboard">Admin</Link>
            <ChevronRight size={14} aria-hidden="true" />
            <Link to="/admin/workspaces">Workspace Management</Link>
            <ChevronRight size={14} aria-hidden="true" />
            <span aria-current="page">Add Accounts to Workspace</span>
          </nav>
          <h1 className="assign-accounts-page__title">Add Accounts to Workspace</h1>
          <p className="assign-accounts-page__subtitle">
            Chọn workspace, tick các account cần thêm và xác nhận. Account và
            Workspace được quản lý độc lập — bước này chỉ gán account đã có
            sẵn vào workspace.
          </p>
        </header>

        <form className="assign-accounts-card" onSubmit={handleSubmit}>
          {formError && (
            <p className="assign-accounts-form__error" role="alert">
              {formError}
            </p>
          )}

          <section className="assign-accounts-form__controls">
            <SelectField
              id="assign-workspace"
              label="Workspace đích"
              className="assign-accounts-field"
              value={workspaceId}
              onChange={(event) => {
                setWorkspaceId(event.target.value)
                setSelectedUserIds([])
              }}
              disabled={assignableWorkspaces.length === 0}
            >
              <option value="">
                {assignableWorkspaces.length === 0
                  ? 'Chưa có workspace đang hoạt động'
                  : 'Chọn workspace'}
              </option>
              {assignableWorkspaces.map((workspace) => (
                <option key={workspace.id} value={workspace.id}>
                  {workspace.name}
                  {workspace.code ? ` (${workspace.code})` : ''}
                </option>
              ))}
            </SelectField>

            <SelectField
              id="assign-role"
              label="Vai trò trong workspace"
              className="assign-accounts-field"
              value={roleId}
              onChange={(event) => setRoleId(event.target.value)}
              disabled={workspaceRoleOptions.length === 0}
            >
              <option value="">Chọn vai trò</option>
              {workspaceRoleOptions.map((role) => (
                <option key={role.id} value={role.id}>
                  {role.name}
                </option>
              ))}
            </SelectField>
          </section>

          <section className="assign-accounts-form__list-panel">
            <div className="assign-accounts-form__list-toolbar">
              <div className="assign-accounts-form__search">
                <Search size={16} aria-hidden="true" />
                <input
                  type="search"
                  placeholder="Tìm account theo tên, email, mã NV..."
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  disabled={!workspaceId}
                />
              </div>
              <label className="assign-accounts-form__select-all">
                <input
                  type="checkbox"
                  checked={visibleAllSelected}
                  onChange={toggleAllVisible}
                  disabled={!workspaceId || availableUsers.length === 0}
                />
                Chọn tất cả ({availableUsers.length})
              </label>
            </div>

            {showEmptyState ? (
              <div className="assign-accounts-form__empty-state">
                <span className="assign-accounts-form__empty-icon" aria-hidden="true">
                  <UserSearch size={28} strokeWidth={1.5} />
                </span>
                <p className="assign-accounts-form__empty-title">
                  Chưa có dữ liệu hiển thị
                </p>
                <p className="assign-accounts-form__empty-text">
                  {!workspaceId
                    ? 'Hãy chọn một workspace bên trên để xem danh sách các tài khoản có thể thêm vào hệ thống.'
                    : membersLoading
                      ? 'Đang tải danh sách account...'
                      : 'Không còn account nào để thêm vào workspace này.'}
                </p>
              </div>
            ) : (
              <ul className="assign-accounts-list">
                {availableUsers.map((user) => {
                  const checked = selectedUserIds.includes(String(user.id))

                  return (
                    <li key={user.id}>
                      <label
                        className={`assign-accounts-list__item${checked ? ' assign-accounts-list__item--selected' : ''}`}
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleUser(user.id)}
                        />
                        <UserAvatar fullName={user.fullName} size="sm" />
                        <span className="assign-accounts-list__info">
                          <strong>{user.fullName}</strong>
                          <span>{user.email}</span>
                        </span>
                        <span className="assign-accounts-list__meta">
                          {user.roleName ??
                            USER_ROLE_LABELS[user.role] ??
                            'Chưa gán vai trò hệ thống'}
                        </span>
                      </label>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>

          <footer className="assign-accounts-form__footer">
            <p className="assign-accounts-form__selected-count">
              <strong>{selectedUserIds.length}</strong> account được chọn
            </p>
            <Button
              type="submit"
              variant="primary"
              className="assign-accounts-form__submit"
              disabled={
                assignMutation.isPending ||
                !workspaceId ||
                selectedUserIds.length === 0
              }
            >
              <UserPlus size={16} aria-hidden="true" />
              {assignMutation.isPending
                ? 'Đang gán...'
                : 'Thêm vào Workspace'}
            </Button>
          </footer>
        </form>
      </div>
    </PermissionRoute>
  )
}
