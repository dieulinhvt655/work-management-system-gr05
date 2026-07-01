import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  CircleAlert,
  Clock3,
  Search,
  ShieldCheck,
  UserCog,
  UserPlus,
  Users,
} from 'lucide-react'
import { fetchTeamMembers, fetchTeams } from '../../api/teamsApi'
import Button from '../../components/ui/Button'
import LoadingScreen from '../../components/common/LoadingScreen'
import UserAvatar from '../../components/common/UserAvatar'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { buildManagedTeamOption, getManagedTeamId } from '../../utils/teamLeaderScope'

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN').format(new Date(value))
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function statusLabel(status) {
  return status === 'ACTIVE' ? 'Đang hoạt động' : 'Vắng mặt'
}

function statusTone(status) {
  return status === 'ACTIVE' ? 'success' : 'muted'
}

function roleLabel(member) {
  if (member.isLeader) return 'Team Leader'
  return member.roleName || '—'
}

function roleTone(member) {
  if (member.isLeader) return 'primary'
  if (String(member.roleName ?? '').toLowerCase().includes('manager')) return 'primary'
  return 'neutral'
}

function sortMembers(members, sortBy) {
  const sorted = [...members]

  sorted.sort((a, b) => {
    if (sortBy === 'NAME_ASC') {
      return String(a.fullName ?? '').localeCompare(String(b.fullName ?? ''), 'vi')
    }

    if (sortBy === 'NAME_DESC') {
      return String(b.fullName ?? '').localeCompare(String(a.fullName ?? ''), 'vi')
    }

    if (sortBy === 'ROLE_ASC') {
      return String(a.roleName ?? '').localeCompare(String(b.roleName ?? ''), 'vi')
    }

    if (sortBy === 'JOINED_ASC') {
      return new Date(a.joinedAt ?? 0) - new Date(b.joinedAt ?? 0)
    }

    if (sortBy === 'JOINED_DESC') {
      return new Date(b.joinedAt ?? 0) - new Date(a.joinedAt ?? 0)
    }

    return 0
  })

  return sorted
}

export default function TeamMembersPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sortBy, setSortBy] = useState('JOINED_DESC')
  const managedTeamId = getManagedTeamId(user)

  const {
    data: teams = [],
    isLoading: teamsLoading,
    isError: teamsIsError,
    error: teamsError,
  } = useQuery({
    queryKey: ['teams', user?.workspaceId],
    queryFn: () => fetchTeams(user?.workspaceId),
    enabled: isAuthenticated && !authLoading && !managedTeamId,
  })

  const managedTeam = useMemo(() => {
    const sessionTeam = buildManagedTeamOption(user)

    if (managedTeamId) {
      const byUserTeam = teams.find((team) => String(team.id) === String(managedTeamId))
      if (byUserTeam) return byUserTeam
    }

    return teams.find((team) =>
      (team.members ?? []).some(
        (member) =>
          member.isLeader && String(member.userId) === String(user?.id),
      ),
    ) ?? sessionTeam
  }, [managedTeamId, teams, user])

  const {
    data: members = [],
    isLoading: membersLoading,
    isError: membersIsError,
    error: membersError,
  } = useQuery({
    queryKey: ['teams', managedTeam?.workspaceId, managedTeam?.id, 'members'],
    queryFn: () => fetchTeamMembers(managedTeam.workspaceId, managedTeam.id),
    enabled: Boolean(managedTeam),
  })

  const filteredMembers = useMemo(() => {
    const normalized = keyword.trim().toLowerCase()
    const byKeyword = !normalized
      ? members
      : members.filter((member) =>
      [member.fullName, member.email, member.roleName, member.employeeCode]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(normalized)),
      )
    const byStatus =
      statusFilter === 'ALL'
        ? byKeyword
        : byKeyword.filter((member) => member.status === statusFilter)

    return sortMembers(byStatus, sortBy)
  }, [keyword, members, sortBy, statusFilter])

  const memberStats = useMemo(() => {
    const total = members.length
    const active = members.filter((member) => member.status === 'ACTIVE').length
    const inactive = total - active
    const leaders = members.filter(
      (member) => member.isLeader || String(member.roleName ?? '').includes('Manager'),
    ).length

    return { total, active, inactive, leaders }
  }, [members])

  if (authLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <div className="page page--wide team-members-page project-members-page">
        <header className="project-members-hero">
          <div>
            <p className="project-members-hero__eyebrow">Dự án</p>
            <h1 className="project-members-hero__title">Thành viên dự án</h1>
            <p className="project-members-hero__subtitle">
              Quản lý nhân sự, vai trò và trạng thái tham gia trong project hiện tại.
            </p>
          </div>

          {managedTeam && (
            <div className="project-members-hero__team">
              <span>Team đang quản lý</span>
              <strong>{managedTeam.name}</strong>
              <small>
                Leader: {user?.fullName ?? user?.name ?? '—'}
              </small>
            </div>
          )}
        </header>

        {teamsLoading && !managedTeam ? (
          <div className="project-members-empty">
            <p className="project-members-empty__title">Đang tải dữ liệu</p>
            <p className="project-members-empty__text">
              Hệ thống đang lấy dữ liệu Team thật từ API để xác định Team do Leader quản lý.
            </p>
          </div>
        ) : teamsIsError && !managedTeam ? (
          <div className="project-members-empty">
            <p className="project-members-empty__title">Không tải được danh sách Team</p>
            <p className="project-members-empty__text">
              {getErrorMessage(teamsError, 'API Team chưa phản hồi. Vui lòng thử lại sau.')}
            </p>
          </div>
        ) : !managedTeam ? (
          <div className="project-members-empty">
            <p className="project-members-empty__title">Chưa có Team được quản lý</p>
            <p className="project-members-empty__text">
              Team Leader cần được phân công quản lý Team trước khi quản lý thành viên dự án.
            </p>
          </div>
        ) : (
          <>
            <section className="project-members-kpis" aria-label="Thống kê thành viên dự án">
              <article className="project-members-kpi">
                <span className="project-members-kpi__icon project-members-kpi__icon--blue">
                  <Users size={18} aria-hidden="true" />
                </span>
                <div>
                  <p>Tổng thành viên</p>
                  <strong>{memberStats.total}</strong>
                </div>
              </article>
              <article className="project-members-kpi">
                <span className="project-members-kpi__icon project-members-kpi__icon--green">
                  <ShieldCheck size={18} aria-hidden="true" />
                </span>
                <div>
                  <p>Đang hoạt động</p>
                  <strong>{memberStats.active}</strong>
                </div>
              </article>
              <article className="project-members-kpi">
                <span className="project-members-kpi__icon project-members-kpi__icon--orange">
                  <UserCog size={18} aria-hidden="true" />
                </span>
                <div>
                  <p>Vai trò quản lý</p>
                  <strong>{memberStats.leaders}</strong>
                </div>
              </article>
              <article className="project-members-kpi">
                <span className="project-members-kpi__icon project-members-kpi__icon--gray">
                  <CircleAlert size={18} aria-hidden="true" />
                </span>
                <div>
                  <p>Vắng mặt</p>
                  <strong>{memberStats.inactive}</strong>
                </div>
              </article>
            </section>

            <section className="project-members-card">
              <div className="project-members-toolbar">
                <label className="project-members-search" htmlFor="team-member-search">
                  <Search size={16} aria-hidden="true" />
                  <input
                    id="team-member-search"
                    type="search"
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    placeholder="Tìm kiếm thành viên, vai trò, email..."
                  />
                </label>

                <div className="project-members-toolbar__actions">
                  <label className="project-members-select" htmlFor="team-member-status">
                    <span>Lọc</span>
                    <select
                      id="team-member-status"
                      value={statusFilter}
                      onChange={(event) => setStatusFilter(event.target.value)}
                    >
                      <option value="ALL">Tất cả trạng thái</option>
                      <option value="ACTIVE">Đang hoạt động</option>
                      <option value="INACTIVE">Vắng mặt</option>
                    </select>
                  </label>

                  <label className="project-members-select" htmlFor="team-member-sort">
                    <span>Sắp xếp</span>
                    <select
                      id="team-member-sort"
                      value={sortBy}
                      onChange={(event) => setSortBy(event.target.value)}
                    >
                      <option value="JOINED_DESC">Ngày tham gia mới nhất</option>
                      <option value="JOINED_ASC">Ngày tham gia cũ nhất</option>
                      <option value="NAME_ASC">Tên A-Z</option>
                      <option value="NAME_DESC">Tên Z-A</option>
                      <option value="ROLE_ASC">Vai trò</option>
                    </select>
                  </label>

                  {canManageMembers && (
                    <Button type="button" variant="primary" onClick={() => setShowAdd(true)}>
                      <UserPlus size={16} aria-hidden="true" />
                      Thêm thành viên
                    </Button>
                  )}
                </div>
              </div>

              <div className="project-members-table-wrap">
                {membersLoading ? (
                  <p className="project-members-empty project-members-empty--inline">
                    Đang tải thành viên dự án...
                  </p>
                ) : membersIsError ? (
                  <p className="project-members-empty project-members-empty--inline">
                    {getErrorMessage(membersError, 'Không tải được danh sách thành viên dự án.')}
                  </p>
                ) : filteredMembers.length === 0 ? (
                  <div className="project-members-empty project-members-empty--inline">
                    <p className="project-members-empty__title">Không có thành viên phù hợp</p>
                    <p className="project-members-empty__text">
                      Thử đổi từ khóa tìm kiếm hoặc bộ lọc trạng thái.
                    </p>
                    {canManageMembers && (
                      <Button type="button" variant="primary" onClick={() => setShowAdd(true)}>
                        <UserPlus size={16} aria-hidden="true" />
                        Thêm thành viên đầu tiên
                      </Button>
                    )}
                  </div>
                ) : (
                  <table className="project-members-table">
                    <thead>
                      <tr>
                        <th>Thành viên</th>
                        <th>Vai trò</th>
                        <th>Trạng thái</th>
                        <th>Ngày tham gia</th>
                        <th>Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredMembers.map((member) => {
                        const isManager =
                          member.isLeader ||
                          String(member.roleName ?? '').toLowerCase().includes('manager')

                        return (
                          <tr key={member.id}>
                            <td>
                              <div className="project-member-person">
                                <UserAvatar fullName={member.fullName} size="md" />
                                <div>
                                  <strong>{member.fullName}</strong>
                                  <span>{member.email}</span>
                                  {member.employeeCode && (
                                    <small>Mã NV: {member.employeeCode}</small>
                                  )}
                                </div>
                              </div>
                            </td>
                            <td>
                              <span className={`project-member-badge project-member-badge--${roleTone(member)}`}>
                                {roleLabel(member)}
                              </span>
                            </td>
                            <td>
                              <span className={`project-member-status project-member-status--${statusTone(member.status)}`}>
                                <span className="project-member-status__dot" />
                                {statusLabel(member.status)}
                              </span>
                            </td>
                            <td>{formatDate(member.joinedAt)}</td>
                            <td>
                              <div className="project-member-actions">
                                {isManager ? (
                                  <span className="project-member-actions__hint">
                                    <ShieldCheck size={16} aria-hidden="true" />
                                    Quản lý dự án
                                  </span>
                                ) : canManageMembers ? (
                                  <>
                                    <button
                                      type="button"
                                      className="project-member-action"
                                      disabled
                                      title="Chức năng gán PM sẽ được bổ sung sau"
                                    >
                                      <UserCog size={16} aria-hidden="true" />
                                      Gán PM
                                    </button>
                                    <button
                                      type="button"
                                      className="project-member-action project-member-action--danger"
                                      disabled
                                      title="Chức năng gỡ thành viên sẽ được bổ sung sau"
                                    >
                                      <CircleAlert size={16} aria-hidden="true" />
                                      Gỡ
                                    </button>
                                  </>
                                ) : (
                                  <span className="project-member-actions__hint">
                                    <Clock3 size={16} aria-hidden="true" />
                                    Chỉ xem
                                  </span>
                                )}
                              </div>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                )}
              </div>

              <footer className="project-members-footer">
                <p>
                  Hiển thị <strong>{filteredMembers.length}</strong> / {members.length} thành viên
                </p>
                <p>Cập nhật gần nhất: {formatDateTime(project?.updatedAt)}</p>
              </footer>
            </section>
          </>
        )}
      </div>
    </PermissionRoute>
  )
}
