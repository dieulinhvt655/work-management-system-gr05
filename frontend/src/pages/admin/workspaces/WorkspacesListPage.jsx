import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  fetchWorkspaceOwners,
  fetchWorkspaces,
  updateWorkspace,
  updateWorkspaceStatus,
} from '../../../api/workspacesApi'
import LoadingScreen from '../../../components/common/LoadingScreen'
import PermissionGate from '../../../components/common/PermissionGate'
import Toast from '../../../components/common/Toast'
import { PERMISSIONS } from '../../../constants/permissions'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'
import EditWorkspaceModal from './components/EditWorkspaceModal'
import ViewWorkspaceModal from './components/ViewWorkspaceModal'
import WorkspaceFilters, { FILTER_ALL } from './components/WorkspaceFilters'
import WorkspaceTable from './components/WorkspaceTable'
import {
  filterWorkspaces,
  hasActiveWorkspaceFilters,
} from './utils/filterWorkspaces'

const INITIAL_FILTERS = {
  search: '',
  status: FILTER_ALL,
  createdDate: '',
  ownerId: FILTER_ALL,
}

const WORKSPACES_QUERY_KEY = ['admin', 'workspaces']
const OWNERS_QUERY_KEY = ['admin', 'workspaces', 'owners']

export default function WorkspacesListPage() {
  const queryClient = useQueryClient()
  const location = useLocation()
  const navigate = useNavigate()
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [viewWorkspace, setViewWorkspace] = useState(null)
  const [editWorkspace, setEditWorkspace] = useState(null)
  const [actionError, setActionError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  const { data: workspaces = [], isLoading } = useQuery({
    queryKey: WORKSPACES_QUERY_KEY,
    queryFn: fetchWorkspaces,
  })

  const { data: owners = [] } = useQuery({
    queryKey: OWNERS_QUERY_KEY,
    queryFn: fetchWorkspaceOwners,
  })

  const filteredWorkspaces = useMemo(
    () => filterWorkspaces(workspaces, filters),
    [workspaces, filters],
  )

  const filtersActive = hasActiveWorkspaceFilters(filters)

  const invalidateWorkspaces = () => {
    queryClient.invalidateQueries({ queryKey: WORKSPACES_QUERY_KEY })
  }

  useEffect(() => {
    if (!location.state?.toast) return
    setToastMessage(location.state.toast)
    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  const updateWorkspaceMutation = useMutation({
    mutationFn: ({ workspaceId, payload }) =>
      updateWorkspace(workspaceId, payload),
    onSuccess: () => {
      setActionError('')
      setEditWorkspace(null)
      invalidateWorkspaces()
      setToastMessage('Cập nhật Workspace thành công')
    },
    onError: (error) => {
      setActionError(
        getErrorMessage(error, 'Không thể cập nhật Workspace.'),
      )
    },
  })

  const disableWorkspaceMutation = useMutation({
    mutationFn: (workspaceId) => updateWorkspaceStatus(workspaceId),
    onSuccess: () => {
      invalidateWorkspaces()
      setToastMessage('Đã vô hiệu hóa Workspace')
    },
    onError: (error) => {
      window.alert(getErrorMessage(error, 'Không thể vô hiệu hóa Workspace.'))
    },
  })

  const handleSaveEdit = (workspaceId, values) => {
    setActionError('')
    updateWorkspaceMutation.mutate({
      workspaceId,
      payload: {
        name: values.name,
        description: values.description?.trim() || '',
      },
    })
  }

  const handleDisable = (workspace) => {
    const confirmed = window.confirm(
      `Bạn có chắc muốn vô hiệu hóa Workspace "${workspace.name}"?`,
    )
    if (!confirmed) return
    disableWorkspaceMutation.mutate(workspace.id)
  }

  if (isLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.WORKSPACE_ADMIN_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide workspaces-page">
        <header className="workspaces-page__title-bar">
          <div>
            <h1 className="workspaces-page__title">Workspaces</h1>
            <p className="workspaces-page__subtitle">
              Xem và quản lý danh sách workspace trên hệ thống.
            </p>
          </div>
        </header>

        <PermissionGate permission={PERMISSIONS.WORKSPACE_ADMIN_CREATE}>
          <header className="page__header page__header--row page__header--actions-only workspaces-page__header">
            <Link
              to="/admin/workspaces/create"
              className="btn btn--primary page-header-btn workspaces-page__cta"
            >
              <Plus size={16} aria-hidden="true" />
              Tạo Workspace
            </Link>
          </header>
        </PermissionGate>

        <WorkspaceFilters
          filters={filters}
          onChange={setFilters}
          resultCount={filteredWorkspaces.length}
          owners={owners}
        />

        {workspaces.length === 0 ? (
          <div className="workspace-table-empty">
            <p className="workspace-table-empty__title">Chưa có Workspace</p>
            <p className="workspace-table-empty__text">
              Tạo Workspace đầu tiên để bắt đầu quản lý tổ chức.
            </p>
          </div>
        ) : filteredWorkspaces.length === 0 ? (
          <div className="workspace-table-empty">
            <p className="workspace-table-empty__title">Không có kết quả</p>
            <p className="workspace-table-empty__text">
              {filtersActive
                ? 'Thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.'
                : 'Không tìm thấy workspace phù hợp.'}
            </p>
          </div>
        ) : (
          <WorkspaceTable
            workspaces={filteredWorkspaces}
            onView={setViewWorkspace}
            onEdit={setEditWorkspace}
            onDisable={handleDisable}
          />
        )}

        {viewWorkspace && (
          <ViewWorkspaceModal
            workspace={viewWorkspace}
            onClose={() => setViewWorkspace(null)}
          />
        )}

        {editWorkspace && (
          <EditWorkspaceModal
            workspace={editWorkspace}
            onClose={() => {
              setEditWorkspace(null)
              setActionError('')
            }}
            onSave={handleSaveEdit}
            isSaving={updateWorkspaceMutation.isPending}
            saveError={actionError}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
