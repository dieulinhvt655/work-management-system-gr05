import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, UserPlus } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  fetchTeamActivities,
  fetchTeamSummary,
  fetchTeams,
  updateTeam,
} from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import Toast from '../../components/common/Toast'
import PermissionGate from '../../components/common/PermissionGate'
import { PERMISSIONS } from '../../constants/permissions'
import { useWorkspaceScope } from '../../hooks/useWorkspaceScope'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import EditTeamModal from './components/EditTeamModal'
import TeamActivityPanel from './components/TeamActivityPanel'
import TeamTable from './components/TeamTable'
import TeamFilters, { FILTER_ALL } from './components/TeamFilters'
import TeamStatsCards from './components/TeamStatsCards'
import { filterTeams, hasActiveTeamFilters } from './utils/filterTeams'

const INITIAL_FILTERS = {
  search: '',
  status: FILTER_ALL,
  leader: FILTER_ALL,
}

export default function TeamsListPage() {
  const queryClient = useQueryClient()
  const { workspaceId } = useWorkspaceScope()
  const location = useLocation()
  const navigate = useNavigate()
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [toastMessage, setToastMessage] = useState('')
  const [editTeam, setEditTeam] = useState(null)
  const [actionError, setActionError] = useState('')

  useEffect(() => {
    if (!location.state?.toast) return

    setToastMessage(location.state.toast)
    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  const teamsQueryKey = ['teams', workspaceId]

  const { data: teams = [], isLoading: teamsLoading } = useQuery({
    queryKey: teamsQueryKey,
    queryFn: () => fetchTeams(workspaceId),
    enabled: Boolean(workspaceId),
  })

  const { data: summary } = useQuery({
    queryKey: ['teams', 'summary', workspaceId],
    queryFn: () => fetchTeamSummary(workspaceId),
    enabled: Boolean(workspaceId),
  })

  const { data: activities = [] } = useQuery({
    queryKey: ['teams', 'activities', workspaceId],
    queryFn: () => fetchTeamActivities(workspaceId, { limit: 5 }),
    enabled: Boolean(workspaceId),
  })

  const filteredTeams = useMemo(
    () => filterTeams(teams, filters),
    [teams, filters],
  )

  const filtersActive = hasActiveTeamFilters(filters)

  const invalidateTeams = () => {
    queryClient.invalidateQueries({ queryKey: teamsQueryKey })
    queryClient.invalidateQueries({ queryKey: ['teams', 'summary', workspaceId] })
  }

  const updateTeamMutation = useMutation({
    mutationFn: ({ team, payload }) =>
      updateTeam(team.workspaceId, team.id, payload),
    onSuccess: () => {
      setEditTeam(null)
      setActionError('')
      invalidateTeams()
      setToastMessage('Cập nhật phòng ban thành công.')
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật phòng ban.'))
    },
  })

  if (teamsLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.TEAM_READ}>
      <div className="page page--wide teams-page">
        <header className="teams-page__toolbar">
          <div className="teams-page__intro">
            <h1 className="teams-page__title">Danh sách phòng ban / nhóm</h1>
            <p className="teams-page__subtitle">
              Quản lý cơ cấu tổ chức, thành viên và trưởng nhóm trong Workspace.
            </p>
          </div>

          <PermissionGate permission={PERMISSIONS.TEAM_MANAGE}>
            <div className="teams-page__actions">
              <Link to="/teams/assign-members" className="teams-page__cta teams-page__cta--secondary">
                <UserPlus size={16} aria-hidden="true" />
                Phân công nhân viên
              </Link>
              <Link to="/teams/create" className="teams-page__cta">
                <Plus size={16} aria-hidden="true" />
                Tạo phòng ban / nhóm
              </Link>
            </div>
          </PermissionGate>
        </header>

        <TeamStatsCards summary={summary} />

        <TeamFilters
          filters={filters}
          onChange={setFilters}
          resultCount={filteredTeams.length}
        />

        {teams.length === 0 ? (
          <div className="teams-empty">
            <p className="teams-empty__title">Chưa có nhóm / phòng ban</p>
            <p className="teams-empty__text">
              Tạo nhóm hoặc phòng ban đầu tiên để tổ chức cơ cấu workspace.
            </p>
          </div>
        ) : filteredTeams.length === 0 ? (
          <div className="teams-empty">
            <p className="teams-empty__title">Không có kết quả</p>
            <p className="teams-empty__text">
              {filtersActive
                ? 'Thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.'
                : 'Không tìm thấy nhóm / phòng ban phù hợp.'}
            </p>
          </div>
        ) : (
          <div className="teams-page__list">
            <TeamTable
              teams={filteredTeams}
              onEdit={(team) => {
                setActionError('')
                setEditTeam(team)
              }}
            />
          </div>
        )}

        <TeamActivityPanel
          activities={activities}
          newMembersCount={summary?.newMembersThisMonth ?? 0}
        />

        {editTeam && (
          <EditTeamModal
            team={editTeam}
            onClose={() => {
              setEditTeam(null)
              setActionError('')
            }}
            onSave={(team, payload) =>
              updateTeamMutation.mutate({ team, payload })
            }
            isSaving={updateTeamMutation.isPending}
            saveError={actionError}
          />
        )}

        {toastMessage && (
          <Toast message={toastMessage} onClose={() => setToastMessage('')} />
        )}
      </div>
    </PermissionRoute>
  )
}
