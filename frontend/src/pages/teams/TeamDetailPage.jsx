import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Ban, Crown, Pencil, UserPlus, Users } from 'lucide-react'
import { Link, Navigate, useLocation, useNavigate, useParams } from 'react-router-dom'
import { fetchOrganizationMembers } from '../../api/organizationMembersApi'
import { resolveRoleIdByKey } from '../../api/rolesApi'
import {
  addTeamMember,
  disbandTeam,
  fetchTeamById,
  fetchTeams,
  updateTeam,
} from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import PermissionGate from '../../components/common/PermissionGate'
import Toast from '../../components/common/Toast'
import UserAvatar from '../../components/common/UserAvatar'
import Button from '../../components/ui/Button'
import { PERMISSIONS } from '../../constants/permissions'
import { TEAM_STATUS } from '../../constants/teams'
import { useWorkspaceScope } from '../../hooks/useWorkspaceScope'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import AddTeamMemberModal from './components/AddTeamMemberModal'
import AssignTeamLeaderForm from './components/AssignTeamLeaderForm'
import DisbandTeamModal from './components/DisbandTeamModal'
import EditTeamModal from './components/EditTeamModal'
import TeamStatusBadge from './components/TeamStatusBadge'
import { useAssignTeamLeaderMutation } from './hooks/useAssignTeamLeaderMutation'

function teamDetailQueryKey(workspaceId, teamId) {
  return ['teams', workspaceId, teamId]
}

export default function TeamDetailPage() {
  const { teamId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { workspaceId: scopeWorkspaceId } = useWorkspaceScope()

  const workspaceId =
    location.state?.workspaceId ?? scopeWorkspaceId ?? null

  const [showEdit, setShowEdit] = useState(false)
  const [showAddMember, setShowAddMember] = useState(false)
  const [showDisband, setShowDisband] = useState(false)
  const [actionError, setActionError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  useEffect(() => {
    if (!location.state?.toast) return

    setToastMessage(location.state.toast)
    navigate(location.pathname, {
      replace: true,
      state: { workspaceId: location.state?.workspaceId },
    })
  }, [location.pathname, location.state, navigate])

  const {
    data: team,
    isLoading,
    isError,
  } = useQuery({
    queryKey: teamDetailQueryKey(workspaceId, teamId),
    queryFn: async () => {
      if (workspaceId) {
        return fetchTeamById(workspaceId, teamId)
      }

      const teams = await fetchTeams()
      const match = teams.find((entry) => String(entry.id) === String(teamId))
      if (!match) {
        throw new Error('Không tìm thấy phòng ban')
      }

      return fetchTeamById(match.workspaceId, teamId)
    },
    enabled: Boolean(teamId),
  })

  const resolvedWorkspaceId = team?.workspaceId ?? workspaceId

  const { data: workspaceMembers = [] } = useQuery({
    queryKey: ['organization', 'members', resolvedWorkspaceId],
    queryFn: () => fetchOrganizationMembers(resolvedWorkspaceId),
    enabled: Boolean(resolvedWorkspaceId),
  })

  const invalidateTeam = () => {
    queryClient.invalidateQueries({
      queryKey: teamDetailQueryKey(resolvedWorkspaceId, teamId),
    })
    queryClient.invalidateQueries({ queryKey: ['teams', resolvedWorkspaceId] })
    queryClient.invalidateQueries({
      queryKey: ['teams', 'summary', resolvedWorkspaceId],
    })
  }

  const assignLeaderMutation = useAssignTeamLeaderMutation({
    onSuccess: () => {
      setActionError('')
      invalidateTeam()
      setToastMessage('Gán Team Leader thành công.')
    },
    onError: (message) => setActionError(message),
  })

  const addMemberMutation = useMutation({
    mutationFn: async ({ workspaceMemberId }) => {
      const teamMemberRoleId = await resolveRoleIdByKey('Team Member')
      return addTeamMember(team.workspaceId, team.id, {
        workspaceMemberId,
        roleId: teamMemberRoleId,
      })
    },
    onSuccess: () => {
      setShowAddMember(false)
      setActionError('')
      invalidateTeam()
      setToastMessage('Đã thêm nhân viên vào phòng ban.')
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể thêm nhân viên.'))
    },
  })

  const updateTeamMutation = useMutation({
    mutationFn: (payload) => updateTeam(team.workspaceId, team.id, payload),
    onSuccess: () => {
      setShowEdit(false)
      setActionError('')
      invalidateTeam()
      setToastMessage('Cập nhật phòng ban thành công.')
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật phòng ban.'))
    },
  })

  const disbandTeamMutation = useMutation({
    mutationFn: () => disbandTeam(team.workspaceId, team.id),
    onSuccess: () => {
      setShowDisband(false)
      navigate('/teams', {
        state: { toast: 'Đã giải thể phòng ban.' },
      })
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể giải thể phòng ban.'))
    },
  })

  useEffect(() => {
    if (!location.hash || isLoading || !team) return
    const target = document.querySelector(location.hash)
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [location.hash, isLoading, team])

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isError || !team) {
    return <Navigate to="/teams" replace />
  }

  const isActive = team.status === TEAM_STATUS.ACTIVE
  const members = team.members ?? []

  return (
    <PermissionRoute permission={PERMISSIONS.TEAM_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide teams-page team-detail-page">
        <header className="team-detail-page__header">
          <Link
            to="/teams"
            className="team-detail-page__back"
            state={{ workspaceId: resolvedWorkspaceId }}
          >
            <ArrowLeft size={16} aria-hidden="true" />
            Quay lại danh sách
          </Link>

          <div className="team-detail-page__hero">
            <div className="team-detail-page__identity">
              <span className="team-detail-page__avatar" aria-hidden="true">
                {team.code?.slice(0, 2) ?? 'T'}
              </span>
              <div>
                <div className="team-detail-page__title-row">
                  <h1 className="team-detail-page__name">{team.name}</h1>
                  <TeamStatusBadge status={team.status} />
                </div>
                <p className="team-detail-page__code">Mã: {team.code}</p>
              </div>
            </div>

            <PermissionGate permission={PERMISSIONS.TEAM_MANAGE}>
              <div className="team-detail-page__header-actions">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => {
                    setActionError('')
                    setShowEdit(true)
                  }}
                >
                  <Pencil size={16} aria-hidden="true" />
                  Chỉnh sửa
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => {
                    setActionError('')
                    setShowAddMember(true)
                  }}
                  disabled={!isActive}
                >
                  <UserPlus size={16} aria-hidden="true" />
                  Thêm nhân viên
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => {
                    setActionError('')
                    setShowDisband(true)
                  }}
                  disabled={!isActive}
                >
                  <Ban size={16} aria-hidden="true" />
                  Giải thể
                </Button>
              </div>
            </PermissionGate>
          </div>
        </header>

        <div className="team-detail-page__grid">
          <section className="team-detail-card">
            <header className="team-detail-card__head">
              <h2>Thông tin phòng ban</h2>
            </header>
            <dl className="team-detail-card__dl">
              <div>
                <dt>Mô tả</dt>
                <dd>{team.description}</dd>
              </div>
              <div>
                <dt>Số thành viên</dt>
                <dd>{team.memberCount}</dd>
              </div>
              <div>
                <dt>Số dự án</dt>
                <dd>{team.projectCount}</dd>
              </div>
            </dl>
          </section>

          <section className="team-detail-card team-detail-card--leader" id="team-leader">
            <header className="team-detail-card__head">
              <h2>
                <Crown size={18} aria-hidden="true" />
                Trưởng nhóm
              </h2>
            </header>

            {team.leader ? (
              <div className="team-detail-leader">
                <UserAvatar fullName={team.leader.fullName} size="md" />
                <div>
                  <p className="team-detail-leader__name">{team.leader.fullName}</p>
                  <p className="team-detail-leader__role">Team Leader</p>
                </div>
              </div>
            ) : (
              <p className="team-detail-card__empty team-detail-card__empty--compact">
                Chưa có trưởng nhóm.
              </p>
            )}

            <PermissionGate permission={PERMISSIONS.TEAM_MANAGE}>
              {isActive ? (
                <AssignTeamLeaderForm
                  team={team}
                  onSave={(payload) => assignLeaderMutation.mutate({ team, payload })}
                  isSaving={assignLeaderMutation.isPending}
                  saveError={actionError}
                  showCurrentLeader={false}
                  submitLabel={team.leader ? 'Đổi trưởng nhóm' : 'Gán trưởng nhóm'}
                  inline
                />
              ) : (
                <p className="team-detail-card__empty team-detail-card__empty--compact">
                  Phòng ban đã giải thể — không thể thay đổi trưởng nhóm.
                </p>
              )}
            </PermissionGate>
          </section>
        </div>

        <section className="team-detail-card team-detail-card--members">
          <header className="team-detail-card__head">
            <h2>
              <Users size={18} aria-hidden="true" />
              Thành viên ({members.length})
            </h2>
          </header>

          {members.length === 0 ? (
            <p className="team-detail-card__empty">
              Chưa có nhân viên trong phòng ban.
            </p>
          ) : (
            <ul className="team-detail-members">
              {members.map((member) => (
                <li key={member.id} className="team-detail-members__item">
                  <UserAvatar fullName={member.fullName} size="sm" />
                  <div className="team-detail-members__info">
                    <span className="team-detail-members__name">
                      {member.fullName}
                      {member.isLeader && (
                        <span className="team-detail-members__badge">Trưởng nhóm</span>
                      )}
                    </span>
                    <span className="team-detail-members__meta">
                      {member.employeeCode || member.roleName}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {showEdit && (
          <EditTeamModal
            team={team}
            onClose={() => {
              setShowEdit(false)
              setActionError('')
            }}
            onSave={(_, payload) => updateTeamMutation.mutate(payload)}
            isSaving={updateTeamMutation.isPending}
            saveError={actionError}
          />
        )}

        {showAddMember && (
          <AddTeamMemberModal
            team={team}
            workspaceMembers={workspaceMembers}
            onClose={() => {
              setShowAddMember(false)
              setActionError('')
            }}
            onSave={({ workspaceMemberId }) =>
              addMemberMutation.mutate({ workspaceMemberId })
            }
            isSaving={addMemberMutation.isPending}
            saveError={actionError}
          />
        )}

        {showDisband && (
          <DisbandTeamModal
            team={team}
            onClose={() => {
              setShowDisband(false)
              setActionError('')
            }}
            onConfirm={() => disbandTeamMutation.mutate()}
            isSaving={disbandTeamMutation.isPending}
            saveError={actionError}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
