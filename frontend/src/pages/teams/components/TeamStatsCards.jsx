import { Building2, CheckCircle2, Crown, Trash2 } from 'lucide-react'

function StatCard({ label, value, icon: Icon, tone = 'default' }) {
  return (
    <article className={`team-stat-card team-stat-card--${tone}`}>
      <div className="team-stat-card__icon-wrap">
        <Icon size={18} aria-hidden="true" />
      </div>
      <div>
        <p className="team-stat-card__label">{label}</p>
        <p className="team-stat-card__value">{value}</p>
      </div>
    </article>
  )
}

export default function TeamStatsCards({ summary }) {
  const stats = summary ?? {
    total: 0,
    active: 0,
    disbanded: 0,
    withoutLeader: 0,
  }

  return (
    <section className="team-stats" aria-label="Thống kê phòng ban / nhóm">
      <StatCard
        label="Tổng phòng ban / nhóm"
        value={String(stats.total).padStart(2, '0')}
        icon={Building2}
        tone="primary"
      />
      <StatCard
        label="Đang hoạt động"
        value={String(stats.active).padStart(2, '0')}
        icon={CheckCircle2}
        tone="success"
      />
      <StatCard
        label="Đã giải thể"
        value={String(stats.disbanded).padStart(2, '0')}
        icon={Trash2}
        tone="danger"
      />
      <StatCard
        label="Chưa có trưởng nhóm"
        value={String(stats.withoutLeader).padStart(2, '0')}
        icon={Crown}
        tone="warning"
      />
    </section>
  )
}
