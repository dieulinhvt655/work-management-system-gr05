import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronRight, Search, UserPlus, UserSearch } from 'lucide-react'
import { Link } from 'react-router-dom'
import { fetchOrganizationMembers } from '../../api/organizationMembersApi'
import { assignMembersToTeam, fetchTeams } from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import Toast from '../../components/common/Toast'
import UserAvatar from '../../components/common/UserAvatar'
import Button from '../../components/ui/Button'
import SelectField from '../../components/ui/SelectField'
import { PERMISSIONS } from '../../constants/permissions'
import { TEAM_STATUS } from '../../constants/teams'
import { useAuth } from '../../context/AuthContext'
import { useWorkspaceScope } from '../../hooks/useWorkspaceScope'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import {
  ASSIGN_MEMBER_FILTER,
  ASSIGN_MEMBER_FILTER_OPTIONS,
  buildAssignmentCandidates,
  buildAssignmentFilterCounts,
  buildMemberTeamIndex,
  formatCurrentTeamsLabel,
} from './utils/buildAssignmentCandidates'

export default function AssignMembersToTeamPage() {
  const queryClient = useQueryClient()
  const { user, isLoading: authLoading } = useAuth()
  const { workspaceId, isSystemScope } = useWorkspaceScope()

  const [teamId, setTeamId] = useState('')
  const [filterMode, setFilterMode] = useState(ASSIGN_MEMBER_FILTER.UNASSIGNED)
  const [search, setSearch] = useState('')
  const [selectedMemberIds, setSelectedMemberIds] = useState([])
  const [toastMessage, setToastMessage] = useState('')
  const [formError, setFormError] = useState('')

  const teamsQueryKey = ['teams', workspaceId ?? 'auto']
  const membersQueryKey = ['organization', 'members', workspaceId ?? 'auto']
  const canFetch = Boolean(user) && !authLoading

  const {
    data: teams = [],
    isLoading: teamsLoading,
    isError: teamsError,
    error: teamsQueryError,
    refetch: refetchTeams,
  } = useQuery({
    queryKey: teamsQueryKey,
    queryFn: () => fetchTeams(workspaceId ?? undefined),
    enabled: canFetch,
    staleTime: 30_000,
  })

  const {
    data: members = [],
    isLoading: membersLoading,
    isError: membersError,
    error: membersQueryError,
    refetch: refetchMembers,
  } = useQuery({
    queryKey: membersQueryKey,
    queryFn: () => fetchOrganizationMembers(workspaceId ?? undefined),
    enabled: canFetch,
    staleTime: 30_000,
  })

  const assignableTeams = useMemo(
    () =>
      teams
        .filter((team) => team.status === TEAM_STATUS.ACTIVE)
        .map((team) => ({
          id: team.id,
          name: team.name,
          workspaceId: team.workspaceId,
        })),
    [teams],
  )

  const memberTeamIndex = useMemo(() => buildMemberTeamIndex(teams), [teams])

  const selectedTeam = useMemo(
    () => assignableTeams.find((team) => String(team.id) === String(teamId)),
    [assignableTeams, teamId],
  )

  const filterCounts = useMemo(
    () =>
      buildAssignmentFilterCounts({
        members,
        memberTeamIndex,
        targetTeamId: teamId,
        search,
      }),
    [members, memberTeamIndex, teamId, search],
  )

  const candidates = useMemo(
    () =>
      buildAssignmentCandidates({
        members,
        memberTeamIndex,
        targetTeamId: teamId,
        filterMode,
        search,
      }),
    [members, memberTeamIndex, teamId, filterMode, search],
  )

  const assignMutation = useMutation({
    mutationFn: ({ targetTeamId, workspaceMemberIds }) => {
      const targetWorkspaceId =
        selectedTeam?.workspaceId ?? workspaceId ?? teams[0]?.workspaceId

      if (!targetWorkspaceId) {
        throw new Error('Không xác định được workspace cho phòng ban đích.')
      }

      return assignMembersToTeam(
        targetWorkspaceId,
        targetTeamId,
        workspaceMemberIds,
      )
    },
    onSuccess: (results) => {
      const successCount = results.filter((item) => item.success).length
      const failedCount = results.length - successCount

      setSelectedMemberIds([])
      setFormError('')
      queryClient.invalidateQueries({ queryKey: teamsQueryKey })
      queryClient.invalidateQueries({ queryKey: membersQueryKey })
      queryClient.invalidateQueries({ queryKey: ['teams', 'summary', workspaceId] })

      const teamLabel = selectedTeam?.name ?? 'phòng ban'

      if (failedCount === 0) {
        setToastMessage(
          `Đã phân công ${successCount} nhân viên vào ${teamLabel}.`,
        )
      } else {
        setToastMessage(
          `Phân công thành công ${successCount} nhân viên, thất bại ${failedCount}.`,
        )
      }
    },
    onError: (error) => {
      setFormError(
        getErrorMessage(error, 'Không thể phân công nhân viên vào phòng ban.'),
      )
    },
  })

  const toggleMember = (memberId) => {
    setSelectedMemberIds((current) =>
      current.includes(memberId)
        ? current.filter((id) => id !== memberId)
        : [...current, memberId],
    )
  }

  const toggleAllVisible = () => {
    const visibleIds = candidates.map((member) => member.id)
    const allSelected = visibleIds.every((id) => selectedMemberIds.includes(id))

    if (allSelected) {
      setSelectedMemberIds((current) =>
        current.filter((id) => !visibleIds.includes(id)),
      )
      return
    }

    setSelectedMemberIds((current) => [...new Set([...current, ...visibleIds])])
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    setFormError('')

    if (!teamId) {
      setFormError('Vui lòng chọn phòng ban đích.')
      return
    }

    if (selectedMemberIds.length === 0) {
      setFormError('Vui lòng chọn ít nhất một nhân viên.')
      return
    }

    assignMutation.mutate({
      targetTeamId: teamId,
      workspaceMemberIds: selectedMemberIds,
    })
  }

  if (authLoading && !user) {
    return <LoadingScreen />
  }

  const dataLoading =
    canFetch &&
    ((teamsLoading && teams.length === 0) || (membersLoading && members.length === 0))

  if (dataLoading) {
    return <LoadingScreen />
  }

  const noWorkspaceContext =
    !isSystemScope &&
    !workspaceId &&
    teams.length === 0 &&
    members.length === 0 &&
    !teamsLoading &&
    !membersLoading

  if (noWorkspaceContext) {
    return (
      <PermissionRoute permission={PERMISSIONS.TEAM_MANAGE}>
        <div className="page page--wide teams-page assign-members-page">
          <div className="teams-empty">
            <p className="teams-empty__title">Chưa có workspace</p>
            <p className="teams-empty__text">
              Tài khoản chưa được gán workspace quản lý.
            </p>
          </div>
        </div>
      </PermissionRoute>
    )
  }

  const loadError = teamsError || membersError
  const loadErrorMessage = teamsError
    ? getErrorMessage(teamsQueryError, 'Không thể tải danh sách phòng ban.')
    : getErrorMessage(membersQueryError, 'Không thể tải danh sách nhân viên.')

  const visibleAllSelected =
    candidates.length > 0 &&
    candidates.every((member) => selectedMemberIds.includes(member.id))

  const showEmptyState = !teamId || candidates.length === 0
  const transferSelectedCount = candidates.filter(
    (member) => selectedMemberIds.includes(member.id) && member.isTransfer,
  ).length

  return (
    <PermissionRoute permission={PERMISSIONS.TEAM_MANAGE}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide teams-page assign-members-page">
        <header className="assign-accounts-page__header">
          <nav className="assign-accounts-page__breadcrumbs" aria-label="Breadcrumb">
            <Link to="/teams">Phòng ban</Link>
            <ChevronRight size={14} aria-hidden="true" />
            <span aria-current="page">Phân công nhân viên</span>
          </nav>
          <h1 className="assign-accounts-page__title">
            Phân công nhân viên vào phòng ban
          </h1>
          <p className="assign-accounts-page__subtitle">
            Chọn phòng ban đích, tick các nhân viên workspace cần phân công và
            xác nhận. Nhân viên đã thuộc phòng ban đích sẽ không hiển thị trong
            danh sách.
          </p>
        </header>

        <form className="assign-accounts-card" onSubmit={handleSubmit}>
          {loadError && (
            <p className="assign-accounts-form__error" role="alert">
              {loadErrorMessage}{' '}
              <button
                type="button"
                className="create-user-page__retry-link"
                onClick={() => {
                  if (teamsError) refetchTeams()
                  if (membersError) refetchMembers()
                }}
              >
                Thử lại
              </button>
            </p>
          )}

          {formError && (
            <p className="assign-accounts-form__error" role="alert">
              {formError}
            </p>
          )}

          <section className="assign-accounts-form__controls">
            <SelectField
              id="assign-team"
              label="Phòng ban đích"
              className="assign-accounts-field"
              value={teamId}
              onChange={(event) => {
                setTeamId(event.target.value)
                setSelectedMemberIds([])
              }}
              disabled={assignableTeams.length === 0 || teamsLoading}
            >
              <option value="">
                {teamsLoading
                  ? 'Đang tải phòng ban...'
                  : assignableTeams.length === 0
                    ? 'Chưa có phòng ban đang hoạt động'
                    : 'Chọn phòng ban'}
              </option>
              {assignableTeams.map((team) => (
                <option key={team.id} value={team.id}>
                  {team.name}
                </option>
              ))}
            </SelectField>

            <SelectField
              id="assign-filter"
              label="Lọc nhân viên"
              className="assign-accounts-field"
              value={filterMode}
              onChange={(event) => {
                setFilterMode(event.target.value)
                setSelectedMemberIds([])
              }}
              disabled={!teamId}
            >
              {ASSIGN_MEMBER_FILTER_OPTIONS.map(({ value, label }) => (
                <option key={value} value={value}>
                  {teamId ? `${label} (${filterCounts[value] ?? 0})` : label}
                </option>
              ))}
            </SelectField>
          </section>

          {assignableTeams.length === 0 && (
            <p className="assign-members-page__hint">
              Chưa có phòng ban hoạt động.{' '}
              <Link to="/teams/create">Tạo phòng ban mới</Link>
            </p>
          )}

          <section className="assign-accounts-form__list-panel">
            <div className="assign-accounts-form__list-toolbar">
              <div className="assign-accounts-form__search">
                <Search size={16} aria-hidden="true" />
                <input
                  type="search"
                  placeholder="Tìm theo tên, email, mã NV..."
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  disabled={!teamId}
                />
              </div>
              <label className="assign-accounts-form__select-all">
                <input
                  type="checkbox"
                  checked={visibleAllSelected}
                  onChange={toggleAllVisible}
                  disabled={!teamId || candidates.length === 0}
                />
                Chọn tất cả ({candidates.length})
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
                  {!teamId
                    ? 'Hãy chọn phòng ban đích để xem danh sách nhân viên có thể phân công.'
                    : 'Không có nhân viên phù hợp với bộ lọc hiện tại.'}
                </p>
              </div>
            ) : (
              <ul className="assign-accounts-list">
                {candidates.map((member) => {
                  const checked = selectedMemberIds.includes(member.id)
                  const teamLabel = formatCurrentTeamsLabel(member.currentTeams)

                  return (
                    <li key={member.id}>
                      <label
                        className={`assign-accounts-list__item${checked ? ' assign-accounts-list__item--selected' : ''}`}
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleMember(member.id)}
                        />
                        <UserAvatar fullName={member.fullName} size="sm" />
                        <span className="assign-accounts-list__info">
                          <strong>{member.fullName}</strong>
                          <span>
                            {member.email}
                            {member.employeeCode ? ` · ${member.employeeCode}` : ''}
                          </span>
                        </span>
                        <span className="assign-accounts-list__meta">
                          {member.isTransfer ? (
                            <span className="assign-members-page__transfer-badge">
                              {teamLabel}
                            </span>
                          ) : (
                            teamLabel
                          )}
                        </span>
                      </label>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>

          {transferSelectedCount > 0 && (
            <p className="assign-members-page__transfer-note" role="status">
              {transferSelectedCount} nhân viên đang thuộc phòng ban khác — hệ
              thống sẽ thêm vào phòng ban mới. Điều chuyển hoàn toàn cần gỡ khỏi
              phòng ban cũ (sắp có).
            </p>
          )}

          <footer className="assign-accounts-form__footer">
            <p className="assign-accounts-form__selected-count">
              <strong>{selectedMemberIds.length}</strong> nhân viên được chọn
            </p>
            <div className="assign-members-page__footer-actions">
              <Link to="/teams" className="btn btn--ghost">
                Hủy
              </Link>
              <Button
                type="submit"
                variant="primary"
                className="assign-accounts-form__submit"
                disabled={
                  assignMutation.isPending ||
                  !teamId ||
                  selectedMemberIds.length === 0
                }
              >
                <UserPlus size={16} aria-hidden="true" />
                {assignMutation.isPending
                  ? 'Đang phân công...'
                  : 'Phân công vào phòng ban'}
              </Button>
            </div>
          </footer>
        </form>
      </div>
    </PermissionRoute>
  )
}
