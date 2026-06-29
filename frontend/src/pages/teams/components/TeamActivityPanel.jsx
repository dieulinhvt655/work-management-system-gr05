import { Activity } from 'lucide-react'
import { Link } from 'react-router-dom'

function formatActivityTime(isoString) {
  if (!isoString) return ''
  return new Date(isoString).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

export default function TeamActivityPanel({ activities = [], newMembersCount = 0 }) {
  return (
    <section className="team-activity-panel">
      <header className="team-activity-panel__header">
        <div className="team-activity-panel__title-wrap">
          <Activity size={18} aria-hidden="true" />
          <h2 className="team-activity-panel__title">Hoạt động tổ chức gần đây</h2>
        </div>
      </header>

      {activities.length > 0 ? (
        <ul className="team-activity-panel__list">
          {activities.map((item) => (
            <li key={item.id} className="team-activity-panel__item">
              <p className="team-activity-panel__message">{item.message}</p>
              <p className="team-activity-panel__meta">
                {item.actorName} · {formatActivityTime(item.createdAt)}
              </p>
            </li>
          ))}
        </ul>
      ) : (
        <div className="team-activity-panel__empty">
          Chưa có hoạt động tổ chức nào.
        </div>
      )}

      <footer className="team-activity-panel__footer">
        <span className="team-activity-panel__stat">
          Thành viên mới: {String(newMembersCount).padStart(2, '0')}
        </span>
        <Link
          to="/workspace/activity"
          className="team-activity-panel__link"
          onClick={(event) => event.preventDefault()}
          title="Sắp có"
        >
          Xem tất cả nhật ký →
        </Link>
      </footer>
    </section>
  )
}
