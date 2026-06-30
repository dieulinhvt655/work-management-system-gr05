import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search, UserCog, Users } from 'lucide-react'
import { fetchTeamMembers, fetchTeams } from '../../api/teamsApi'
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

function statusLabel(status) {
  return status === 'ACTIVE' ? 'Đang hoạt động' : 'Vắng mặt'
}

export default function TeamMembersPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const [keyword, setKeyword] = useState('')
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
    if (!normalized) return members
    return members.filter((member) =>
      [member.fullName, member.email, member.roleName, member.employeeCode]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(normalized)),
    )
  }, [keyword, members])

  if (authLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <div className="page page--wide team-members-page">
        <header className="page__header page__header--row">
          <div>
            <h1 className="page__title">Thành viên của Team</h1>
            <p className="page__subtitle">
              Thành viên dự án chỉ được chọn từ danh sách Team do Leader quản lý.
            </p>
          </div>
        </header>

        {teamsLoading && !managedTeam ? (
          <div className="project-list-empty">
            <p className="project-list-empty__title">Đang tải danh sách Team</p>
            <p className="project-list-empty__text">
              Hệ thống đang lấy dữ liệu Team thật từ API để xác định Team do Leader quản lý.
            </p>
          </div>
        ) : teamsIsError && !managedTeam ? (
          <div className="project-list-empty">
            <p className="project-list-empty__title">Không tải được danh sách Team</p>
            <p className="project-list-empty__text">
              {getErrorMessage(teamsError, 'API Team chưa phản hồi. Vui lòng thử lại sau.')}
            </p>
          </div>
        ) : !managedTeam ? (
          <div className="project-list-empty">
            <p className="project-list-empty__title">Chưa có Team được quản lý</p>
            <p className="project-list-empty__text">
              Team Leader cần được phân công quản lý Team trước khi quản lý thành viên dự án.
            </p>
          </div>
        ) : (
          <>
            <section className="team-member-stats">
              <article>
                <span>Team</span>
                <strong>{managedTeam.name}</strong>
              </article>
              <article>
                <span>Team Leader</span>
                <strong>{user?.fullName ?? user?.name ?? '—'}</strong>
              </article>
              <article>
                <span>Thành viên</span>
                <strong>{members.length}</strong>
              </article>
              <article>
                <span>Đang hoạt động</span>
                <strong>{members.filter((member) => member.status === 'ACTIVE').length}</strong>
              </article>
            </section>

            <section className="team-member-panel">
              <div className="team-member-toolbar">
                <label className="team-member-search" htmlFor="team-member-search">
                  <Search size={16} aria-hidden="true" />
                  <input
                    id="team-member-search"
                    type="search"
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    placeholder="Tìm thành viên Team..."
                  />
                </label>
              </div>

              <div className="team-member-table-wrap">
                {membersLoading ? (
                  <p className="project-tab-empty">Đang tải thành viên Team từ API...</p>
                ) : membersIsError ? (
                  <p className="project-tab-empty">
                    {getErrorMessage(membersError, 'Không tải được thành viên Team.')}
                  </p>
                ) : (
                  <table className="team-member-table">
                    <thead>
                      <tr>
                        <th>Thành viên</th>
                        <th>Vai trò Team</th>
                        <th>Trạng thái</th>
                        <th>Ngày vào Team</th>
                        <th>Ghi chú</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredMembers.map((member) => (
                        <tr key={member.id}>
                          <td>
                            <div className="team-member-user">
                              <UserAvatar fullName={member.fullName} size="sm" />
                              <div>
                                <strong>{member.fullName}</strong>
                                <span>{member.email}</span>
                              </div>
                            </div>
                          </td>
                          <td>{member.isLeader ? 'Team Leader' : member.roleName}</td>
                          <td>
                            <span
                              className={`team-member-status team-member-status--${member.status.toLowerCase()}`}
                            />
                            {statusLabel(member.status)}
                          </td>
                          <td>{formatDate(member.joinedAt)}</td>
                          <td>
                            <span className="team-member-note">
                              {member.isLeader ? (
                                <>
                                  <UserCog size={14} aria-hidden="true" />
                                  Quản lý Team
                                </>
                              ) : (
                                <>
                                  <Users size={14} aria-hidden="true" />
                                  Có thể thêm vào dự án
                                </>
                              )}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                {!membersLoading && !membersIsError && filteredMembers.length === 0 && (
                  <p className="project-tab-empty">Không có thành viên phù hợp.</p>
                )}
              </div>
            </section>
          </>
        )}
      </div>
    </PermissionRoute>
  )
}
