import { Link } from 'react-router-dom'
import MemberOrgStatusBadge from './MemberOrgStatusBadge'
import { USER_ROLE_LABELS } from '../../../constants/users'

function formatDateTime(value) {
  if (!value) return '—'

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatDate(value) {
  if (!value) return '—'

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value))
}

export default function MemberProjectsCard({ member }) {
  const projectHistory = member.projectHistory ?? []
  const currentProject = projectHistory.find((entry) => entry.isCurrent)

  return (
    <section className="member-detail-card">
      <header className="member-detail-card__head">
        <h2 className="member-detail-card__title">Các dự án đã tham gia</h2>
        <p className="member-detail-card__desc">
          {projectHistory.length === 0
            ? 'Chưa có dự án nào trong lịch sử tham gia'
            : `${projectHistory.length} dự án${
                currentProject ? ` · ${member.activeTaskCount} task đang mở` : ''
              }`}
        </p>
      </header>

      {projectHistory.length === 0 ? (
        <p className="member-detail-card__empty">
          Thành viên chưa tham gia dự án nào.
        </p>
      ) : (
        <ul className="member-projects-list">
          {projectHistory.map((project) => (
            <li
              key={`${project.id}-${project.joinedAt}`}
              className={`member-projects-list__item${
                project.isCurrent ? ' member-projects-list__item--current' : ''
              }`}
            >
              <div className="member-projects-list__main">
                <div className="member-projects-list__header">
                  <Link
                    to={`/projects/${project.id}`}
                    className="member-projects-list__name"
                  >
                    {project.name}
                  </Link>
                  {project.isCurrent && (
                    <span className="member-projects-list__current">Hiện tại</span>
                  )}
                </div>
                <p className="member-projects-list__role">{project.role}</p>
                <p className="member-projects-list__period">
                  {formatDate(project.joinedAt)}
                  {' → '}
                  {project.leftAt ? formatDate(project.leftAt) : 'Hiện tại'}
                </p>
              </div>
              <span className="member-projects-list__status">{project.status}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export function MemberOrganizationCard({ member }) {
  return (
    <section className="member-detail-card">
      <header className="member-detail-card__head">
        <h2 className="member-detail-card__title">Thông tin tổ chức</h2>
      </header>

      <dl className="member-detail-dl">
        <div className="member-detail-dl__row">
          <dt>Team / Department</dt>
          <dd>{member.teamName}</dd>
        </div>
        <div className="member-detail-dl__row">
          <dt>Trạng thái tham gia</dt>
          <dd>
            <MemberOrgStatusBadge status={member.organizationStatus} />
          </dd>
        </div>
        <div className="member-detail-dl__row">
          <dt>Vị trí / chức danh</dt>
          <dd>{member.position}</dd>
        </div>
        <div className="member-detail-dl__row">
          <dt>Vai trò hệ thống</dt>
          <dd>{USER_ROLE_LABELS[member.role] ?? member.role}</dd>
        </div>
        <div className="member-detail-dl__row">
          <dt>Ngày tham gia</dt>
          <dd>{formatDateTime(member.joinedAt)}</dd>
        </div>
        <div className="member-detail-dl__row">
          <dt>Cập nhật gần nhất</dt>
          <dd>{formatDateTime(member.updatedAt)}</dd>
        </div>
      </dl>
    </section>
  )
}

export function MemberOrgHistoryCard({ history = [] }) {
  return (
    <section className="member-detail-card">
      <header className="member-detail-card__head">
        <h2 className="member-detail-card__title">Lịch sử thay đổi tổ chức</h2>
      </header>

      {history.length === 0 ? (
        <p className="member-detail-card__empty">Chưa có lịch sử thay đổi.</p>
      ) : (
        <ul className="member-history-list">
          {history.map((entry) => (
            <li key={entry.id} className="member-history-list__item">
              <div className="member-history-list__content">
                <p className="member-history-list__message">{entry.message}</p>
                <p className="member-history-list__meta">
                  {entry.changedBy ?? 'Hệ thống'} · {formatDateTime(entry.createdAt)}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
