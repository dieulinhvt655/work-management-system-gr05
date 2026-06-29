import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Shield } from 'lucide-react'
import { fetchPermissions } from '../../api/permissionsApi'
import {
  createRole,
  deleteRole,
  fetchRoles,
  updateRole,
} from '../../api/rolesApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import Toast from '../../components/common/Toast'
import Button from '../../components/ui/Button'
import { PERMISSIONS } from '../../constants/permissions'
import { ROLE_SCOPE } from '../../constants/roles'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import RoleFormModal from '../admin/roles/components/RoleFormModal'
import RoleTable from '../admin/roles/components/RoleTable'

const ROLES_QUERY_KEY = ['workspace', 'roles']
const PERMISSIONS_QUERY_KEY = ['workspace', 'permissions']

const WORKSPACE_ROLE_SCOPES = new Set([ROLE_SCOPE.WORKSPACE])

const EXCLUDED_PERMISSION_PREFIXES = [
  'workspace:admin',
  'audit:',
  'settings:',
]

function filterWorkspaceRoles(roles) {
  return roles.filter((role) => WORKSPACE_ROLE_SCOPES.has(role.scope))
}

function filterWorkspacePermissions(permissions) {
  return permissions.filter(
    (permission) =>
      !EXCLUDED_PERMISSION_PREFIXES.some((prefix) =>
        permission.code.startsWith(prefix),
      ),
  )
}

export default function WorkspaceRolesPage() {
  const queryClient = useQueryClient()
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const [search, setSearch] = useState('')
  const [editRole, setEditRole] = useState(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [formError, setFormError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  const canFetch = isAuthenticated && !authLoading

  const { data: roles = [], isLoading: rolesLoading } = useQuery({
    queryKey: ROLES_QUERY_KEY,
    queryFn: () => fetchRoles({ force: true }),
    enabled: canFetch,
    select: filterWorkspaceRoles,
  })

  const { data: permissions = [], isLoading: permissionsLoading } = useQuery({
    queryKey: PERMISSIONS_QUERY_KEY,
    queryFn: fetchPermissions,
    enabled: canFetch,
    select: filterWorkspacePermissions,
  })

  const filteredRoles = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return roles

    return roles.filter((role) =>
      [role.name, role.description]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(query),
    )
  }, [roles, search])

  const invalidateRoles = () => {
    queryClient.invalidateQueries({ queryKey: ROLES_QUERY_KEY })
    queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] })
  }

  const createRoleMutation = useMutation({
    mutationFn: createRole,
    onSuccess: () => {
      setFormError('')
      setCreateOpen(false)
      invalidateRoles()
      setToastMessage('Tạo vai trò workspace thành công')
    },
    onError: (error) => {
      setFormError(getErrorMessage(error, 'Không thể tạo vai trò.'))
    },
  })

  const updateRoleMutation = useMutation({
    mutationFn: ({ roleId, payload }) => updateRole(roleId, payload),
    onSuccess: () => {
      setFormError('')
      setEditRole(null)
      invalidateRoles()
      setToastMessage('Cập nhật vai trò thành công')
    },
    onError: (error) => {
      setFormError(getErrorMessage(error, 'Không thể cập nhật vai trò.'))
    },
  })

  const deleteRoleMutation = useMutation({
    mutationFn: deleteRole,
    onSuccess: () => {
      invalidateRoles()
      setToastMessage('Xóa vai trò thành công')
    },
    onError: (error) => {
      window.alert(getErrorMessage(error, 'Không thể xóa vai trò.'))
    },
  })

  const handleSaveCreate = (values) => {
    setFormError('')
    createRoleMutation.mutate({
      ...values,
      scope: ROLE_SCOPE.WORKSPACE,
    })
  }

  const handleSaveEdit = (values) => {
    if (!editRole) return
    setFormError('')
    updateRoleMutation.mutate({
      roleId: editRole.id,
      payload: {
        ...values,
        scope: ROLE_SCOPE.WORKSPACE,
      },
    })
  }

  const handleDelete = (role) => {
    const confirmed = window.confirm(
      `Bạn có chắc muốn xóa vai trò "${role.name}"? Hành động này không thể hoàn tác.`,
    )
    if (!confirmed) return
    deleteRoleMutation.mutate(role.id)
  }

  if (authLoading || rolesLoading || permissionsLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.ROLE_MANAGE}>
      <div className="page page--wide roles-page workspace-roles-page">
        <header className="workspace-roles-page__header">
          <p className="workspace-roles-page__eyebrow">Workspace Owner</p>
          <div className="roles-page__title-row">
            <Shield size={22} aria-hidden="true" />
            <h1>Roles &amp; Permissions</h1>
          </div>
          <p className="workspace-roles-page__subtitle">
            Quản lý vai trò và quyền truy cập trong phạm vi workspace.
          </p>
        </header>

        <div className="roles-page__stats">
          <div className="roles-stat-card">
            <span className="roles-stat-card__value">{roles.length}</span>
            <span className="roles-stat-card__label">Vai trò workspace</span>
          </div>
          <div className="roles-stat-card">
            <span className="roles-stat-card__value">{permissions.length}</span>
            <span className="roles-stat-card__label">Quyền khả dụng</span>
          </div>
        </div>

        <div className="roles-page__toolbar">
          <input
            type="search"
            className="roles-page__search"
            placeholder="Tìm theo tên hoặc mô tả..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Tìm kiếm vai trò"
          />
          <Button
            type="button"
            variant="primary"
            className="roles-page__cta"
            onClick={() => {
              setFormError('')
              setCreateOpen(true)
            }}
          >
            <Plus size={16} aria-hidden="true" />
            Tạo vai trò
          </Button>
        </div>

        <RoleTable
          roles={filteredRoles}
          onEdit={(role) => {
            setFormError('')
            setEditRole(role)
          }}
          onDelete={handleDelete}
          emptyTitle="Chưa có vai trò workspace"
          emptyText="Tạo vai trò đầu tiên để phân quyền thành viên trong workspace."
        />

        {createOpen && (
          <RoleFormModal
            permissions={permissions}
            lockedScope={ROLE_SCOPE.WORKSPACE}
            onClose={() => {
              setCreateOpen(false)
              setFormError('')
            }}
            onSave={handleSaveCreate}
            isSaving={createRoleMutation.isPending}
            saveError={formError}
          />
        )}

        {editRole && (
          <RoleFormModal
            role={editRole}
            permissions={permissions}
            lockedScope={ROLE_SCOPE.WORKSPACE}
            onClose={() => {
              setEditRole(null)
              setFormError('')
            }}
            onSave={handleSaveEdit}
            isSaving={updateRoleMutation.isPending}
            saveError={formError}
          />
        )}

        {toastMessage && (
          <Toast message={toastMessage} onClose={() => setToastMessage('')} />
        )}
      </div>
    </PermissionRoute>
  )
}
