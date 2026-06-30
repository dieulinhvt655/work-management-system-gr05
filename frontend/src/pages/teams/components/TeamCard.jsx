import { Crown, UserPlus, Users } from 'lucide-react'
import UserAvatar from '../../../components/common/UserAvatar'
import PermissionGate from '../../../components/common/PermissionGate'
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

export default function TeamCard({
  team,
  onAddMember,
}) {
  const tone = getAvatarTone(team.id)
  const initials = getInitials(team.name) || team.code?.slice(0, 2) || '??'
  const members = team.members ?? []

  return (
    <article className="team-card">
      <header className="team-card__header">
        <div className={`team-card__avatar team-card__avatar--${tone}`}>
          {initials}
        </div>
        <div className="team-card__title-wrap">
          <div className="team-card__title-row">
            <div className="team-card__name-group">
              <h3 className="team-card__name">{team.name}</h3>
              {team.code && (
                <span className="team-card__code">{team.code}</span>
              )}
            </div>
            <TeamStatusBadge status={team.status} />
          </div>
        </div>
      </header>

      <p className="team-card__description">{team.description}</p>

      <dl className="team-card__metrics">
        <div>
          <dt>Thành viên</dt>
          <dd>{team.memberCount}</dd>
        </div>
        <div>
          <dt>Dự án</dt>
          <dd>{team.projectCount}</dd>
        </div>
        <div>
          <dt>Việc mở</dt>
          <dd>{team.openTaskCount}</dd>
        </div>
      </dl>

      <section className="team-card__members" aria-label="Nhân viên phòng ban">
        <div className="team-card__members-header">
          <Users size={15} aria-hidden="true" />
          <span>Nhân viên</span>
          <span className="team-card__members-count">{members.length}</span>
        </div>

        {members.length === 0 ? (
          <p className="team-card__members-empty">Chưa có nhân viên trong phòng ban.</p>
        ) : (
          <ul className="team-card__members-list">
            {members.map((member) => (
              <li key={member.id} className="team-card__member">
                <UserAvatar fullName={member.fullName} size="sm" />
                <div className="team-card__member-info">
                  <span className="team-card__member-name">{member.fullName}</span>
                  <span className="team-card__member-meta">
                    {member.employeeCode || member.roleName}
                  </span>
                </div>
                {member.isLeader && (
                  <span className="team-card__member-badge">
                    <Crown size={12} aria-hidden="true" />
                    Trưởng nhóm
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <footer className="team-card__footer">
        <div className="team-card__leader">
          {team.leader ? (
            <>
              <UserAvatar fullName={team.leader.fullName} size="sm" />
              <div className="team-card__leader-text">
                <span className="team-card__leader-label">Trưởng nhóm</span>
                <span className="team-card__leader-name">{team.leader.fullName}</span>
              </div>
            </>
          ) : (
            <span className="team-card__leader-empty">Chưa có trưởng nhóm</span>
          )}
        </div>

        <PermissionGate permission={PERMISSIONS.TEAM_MANAGE}>
          <div className="team-card__actions">
            <button
              type="button"
              className="team-card__action-btn"
              onClick={() => onAddMember?.(team)}
            >
              <UserPlus size={14} aria-hidden="true" />
              Thêm NV
            </button>
          </div>
        </PermissionGate>
      </footer>
    </article>
  )
}
