import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Shield } from 'lucide-react'
import { Link } from 'react-router-dom'
import { fetchPermissions } from '../../../api/permissionsApi'
import {
  createRole,
  deleteRole,
  fetchRoles,
  updateRole,
} from '../../../api/rolesApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import Toast from '../../../components/common/Toast'
import Button from '../../../components/ui/Button'
import { PERMISSIONS } from '../../../constants/permissions'
import { useAuth } from '../../../context/AuthContext'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import RoleFormModal from './components/RoleFormModal'
import RoleTable from './components/RoleTable'

const ROLES_QUERY_KEY = ['admin', 'roles']
const PERMISSIONS_QUERY_KEY = ['admin', 'permissions']

export default function RolesListPage() {
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
  })

  const { data: permissions = [], isLoading: permissionsLoading } = useQuery({
    queryKey: PERMISSIONS_QUERY_KEY,
    queryFn: fetchPermissions,
    enabled: canFetch,
  })

  const filteredRoles = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return roles

    return roles.filter((role) =>
      [role.name, role.description, role.scope]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(query),
    )
  }, [roles, search])

  const invalidateRoles = () => {
    queryClient.invalidateQueries({ queryKey: ROLES_QUERY_KEY })
  }

  const createRoleMutation = useMutation({
    mutationFn: createRole,
    onSuccess: () => {
      setFormError('')
      setCreateOpen(false)
      invalidateRoles()
      setToastMessage('Tạo vai trò thành công')
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
    createRoleMutation.mutate(values)
  }

  const handleSaveEdit = (values) => {
    if (!editRole) return
    setFormError('')
    updateRoleMutation.mutate({ roleId: editRole.id, payload: values })
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
      <div className="page page--wide roles-page">
        <nav className="roles-page__breadcrumbs" aria-label="Breadcrumb">
          <Link to="/admin/dashboard">Admin</Link>
          <span aria-hidden="true">/</span>
          <span aria-current="page">Roles &amp; Permissions</span>
        </nav>

        <header className="roles-page__header">
          <div className="roles-page__intro">
            <div className="roles-page__title-row">
              <Shield size={22} aria-hidden="true" />
              <h1>Roles &amp; Permissions</h1>
            </div>
            <p>
              Quản lý vai trò hệ thống và gán quyền truy cập theo phạm vi
              Workspace, Team hoặc Project.
            </p>
          </div>

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
        </header>

        <div className="roles-page__stats">
          <div className="roles-stat-card">
            <span className="roles-stat-card__value">{roles.length}</span>
            <span className="roles-stat-card__label">Vai trò</span>
          </div>
          <div className="roles-stat-card">
            <span className="roles-stat-card__value">{permissions.length}</span>
            <span className="roles-stat-card__label">Quyền hệ thống</span>
          </div>
        </div>

        <div className="roles-page__toolbar">
          <input
            type="search"
            className="roles-page__search"
            placeholder="Tìm theo tên, mô tả, phạm vi..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Tìm kiếm vai trò"
          />
          <span className="roles-page__count">{filteredRoles.length} vai trò</span>
        </div>

        <RoleTable
          roles={filteredRoles}
          onEdit={(role) => {
            setFormError('')
            setEditRole(role)
          }}
          onDelete={handleDelete}
        />

        {createOpen && (
          <RoleFormModal
            permissions={permissions}
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
