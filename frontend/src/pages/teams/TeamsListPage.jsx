import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, UserPlus } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { fetchOrganizationMembers } from '../../api/organizationMembersApi'
import { resolveRoleIdByKey } from '../../api/rolesApi'
import {
  addTeamMember,
  assignTeamLeader,
  assignTeamLeaderFromWorkspaceMember,
  disbandTeam,
  fetchTeamActivities,
  fetchTeamSummary,
  fetchTeams,
  updateTeam,
  updateTeamMember,
} from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import Toast from '../../components/common/Toast'
import PermissionGate from '../../components/common/PermissionGate'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import { useWorkspaceScope } from '../../hooks/useWorkspaceScope'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import AddTeamMemberModal from './components/AddTeamMemberModal'
import AssignTeamLeaderModal from './components/AssignTeamLeaderModal'
import DisbandTeamModal from './components/DisbandTeamModal'
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
  const { user } = useAuth()
  const { workspaceId } = useWorkspaceScope()
  const location = useLocation()
  const navigate = useNavigate()
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [toastMessage, setToastMessage] = useState('')
  const [assignTeam, setAssignTeam] = useState(null)
  const [addMemberTeam, setAddMemberTeam] = useState(null)
  const [editTeam, setEditTeam] = useState(null)
  const [disbandTeamTarget, setDisbandTeamTarget] = useState(null)
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

  const { data: workspaceMembers = [] } = useQuery({
    queryKey: ['organization', 'members', workspaceId],
    queryFn: () => fetchOrganizationMembers(workspaceId),
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

  const assignLeaderMutation = useMutation({
    mutationFn: async ({ team, payload }) => {
      const teamLeaderRoleId = await resolveRoleIdByKey('TEAM_LEADER')

      if (payload.mode === 'existing') {
        const member = team.members.find((entry) => entry.id === payload.teamMemberId)
        if (!member) {
          throw new Error('Không tìm thấy thành viên trong phòng ban.')
        }

        if (Number(member.roleId) !== Number(teamLeaderRoleId)) {
          await updateTeamMember(team.workspaceId, team.id, member.id, {
            roleId: teamLeaderRoleId,
          })
        }

        return assignTeamLeader(team.workspaceId, team.id, member.id)
      }

      return assignTeamLeaderFromWorkspaceMember(team.workspaceId, team.id, {
        workspaceMemberId: payload.workspaceMemberId,
        teamLeaderRoleId,
        existingMembers: team.members,
      })
    },
    onSuccess: () => {
      setAssignTeam(null)
      setActionError('')
      invalidateTeams()
      setToastMessage('Gán Team Leader thành công.')
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể gán Team Leader.'))
    },
  })

  const addMemberMutation = useMutation({
    mutationFn: async ({ team, workspaceMemberId }) => {
      const teamMemberRoleId = await resolveRoleIdByKey('Team Member')
      return addTeamMember(team.workspaceId, team.id, {
        workspaceMemberId,
        roleId: teamMemberRoleId,
      })
    },
    onSuccess: () => {
      setAddMemberTeam(null)
      setActionError('')
      invalidateTeams()
      setToastMessage('Đã thêm nhân viên vào phòng ban.')
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể thêm nhân viên.'))
    },
  })

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

  const disbandTeamMutation = useMutation({
    mutationFn: (team) => disbandTeam(team.workspaceId, team.id),
    onSuccess: () => {
      setDisbandTeamTarget(null)
      setActionError('')
      invalidateTeams()
      setToastMessage('Đã giải thể phòng ban.')
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể giải thể phòng ban.'))
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
              onDisband={(team) => {
                setActionError('')
                setDisbandTeamTarget(team)
              }}
              onAssignLeader={(team) => {
                setActionError('')
                setAssignTeam(team)
              }}
              onAddMember={(team) => {
                setActionError('')
                setAddMemberTeam(team)
              }}
            />
          </div>
        )}

        <TeamActivityPanel
          activities={activities}
          newMembersCount={summary?.newMembersThisMonth ?? 0}
        />

        {assignTeam && (
          <AssignTeamLeaderModal
            team={assignTeam}
            workspaceMembers={workspaceMembers}
            onClose={() => {
              setAssignTeam(null)
              setActionError('')
            }}
            onSave={(payload) => assignLeaderMutation.mutate({ team: assignTeam, payload })}
            isSaving={assignLeaderMutation.isPending}
            saveError={actionError}
          />
        )}

        {addMemberTeam && (
          <AddTeamMemberModal
            team={addMemberTeam}
            workspaceMembers={workspaceMembers}
            onClose={() => {
              setAddMemberTeam(null)
              setActionError('')
            }}
            onSave={({ workspaceMemberId }) =>
              addMemberMutation.mutate({ team: addMemberTeam, workspaceMemberId })
            }
            isSaving={addMemberMutation.isPending}
            saveError={actionError}
          />
        )}

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

        {disbandTeamTarget && (
          <DisbandTeamModal
            team={disbandTeamTarget}
            onClose={() => {
              setDisbandTeamTarget(null)
              setActionError('')
            }}
            onConfirm={(team) => disbandTeamMutation.mutate(team)}
            isSaving={disbandTeamMutation.isPending}
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
