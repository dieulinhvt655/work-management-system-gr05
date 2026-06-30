import { Pencil } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import PermissionGate from '../../../components/common/PermissionGate'
import UserAvatar from '../../../components/common/UserAvatar'
import IconButton from '../../../components/ui/IconButton'
import { PERMISSIONS } from '../../../constants/permissions'
import TeamStatusBadge from './TeamStatusBadge'

const AVATAR_TONES = ['violet', 'amber', 'teal', 'rose', 'blue', 'slate']

function getAvatarTone(teamId) {
  const hash = [...teamId].reduce((sum, char) => sum + char.charCodeAt(0), 0)
  return AVATAR_TONES[hash % AVATAR_TONES.length]
}

function getInitials(name) {
  return (name ?? '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

export default function TeamTable({ teams, onEdit }) {
  const navigate = useNavigate()

  const openTeamDetail = (team) => {
    navigate(`/teams/${team.id}`, { state: { workspaceId: team.workspaceId } })
  }

  const handleRowKeyDown = (event, team) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      openTeamDetail(team)
    }
  }

  return (
    <div className="team-table-wrap">
      <div className="team-table-scroll">
        <table className="team-table">
          <colgroup>
            <col className="team-table__col-avatar" />
            <col className="team-table__col-name" />
            <col className="team-table__col-code" />
            <col className="team-table__col-desc" />
            <col className="team-table__col-leader" />
            <col className="team-table__col-count" />
            <col className="team-table__col-status" />
            <col className="team-table__col-actions" />
          </colgroup>
          <thead>
            <tr>
              <th scope="col">
                <span className="sr-only">Avatar</span>
              </th>
              <th scope="col">Tên phòng ban / nhóm</th>
              <th scope="col">Mã</th>
              <th scope="col">Mô tả</th>
              <th scope="col">Trưởng nhóm</th>
              <th scope="col">SL</th>
              <th scope="col">Trạng thái</th>
              <th scope="col" className="team-table__actions-col">
                <span className="sr-only">Thao tác</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {teams.map((team) => {
              const tone = getAvatarTone(team.id)
              const initials = getInitials(team.name) || team.code?.slice(0, 2) || '??'

              return (
                <tr
                  key={team.id}
                  className="team-table__row team-table__row--clickable"
                  onClick={() => openTeamDetail(team)}
                  onKeyDown={(event) => handleRowKeyDown(event, team)}
                  tabIndex={0}
                  role="link"
                  aria-label={`Xem chi tiết phòng ban ${team.name}`}
                >
                  <td>
                    <span
                      className={`team-table__avatar team-table__avatar--${tone}`}
                      aria-hidden="true"
                    >
                      {initials}
                    </span>
                  </td>
                  <td>
                    <span className="team-table__name" title={team.name}>
                      {team.name}
                    </span>
                  </td>
                  <td className="team-table__code">{team.code}</td>
                  <td className="team-table__desc team-table__truncate" title={team.description}>
                    {team.description}
                  </td>
                  <td>
                    {team.leader ? (
                      <div className="team-table__leader">
                        <UserAvatar fullName={team.leader.fullName} size="sm" />
                        <span className="team-table__leader-name" title={team.leader.fullName}>
                          {team.leader.fullName}
                        </span>
                      </div>
                    ) : (
                      <span className="team-table__muted">Chưa có</span>
                    )}
                  </td>
                  <td className="team-table__count">{team.memberCount}</td>
                  <td>
                    <TeamStatusBadge status={team.status} />
                  </td>
                  <td className="team-table__actions">
                    <PermissionGate permission={PERMISSIONS.TEAM_MANAGE}>
                      <div className="team-table__action-group">
                        <IconButton
                          label="Chỉnh sửa"
                          onClick={(event) => {
                            event.stopPropagation()
                            onEdit?.(team)
                          }}
                        >
                          <Pencil size={15} aria-hidden="true" />
                        </IconButton>
                      </div>
                    </PermissionGate>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
