import { BriefcaseBusiness, UserCheck, UserMinus, Users } from 'lucide-react'

const stats = [
  {
    key: 'total',
    label: 'Tổng thành viên',
    icon: Users,
    tone: 'primary',
  },
  {
    key: 'active',
    label: 'Đang hoạt động',
    icon: UserCheck,
    tone: 'success',
  },
  {
    key: 'inactive',
    label: 'Tạm ngưng',
    icon: UserMinus,
    tone: 'warning',
  },
  {
    key: 'unassigned',
    label: 'Chưa phân nhóm',
    icon: BriefcaseBusiness,
    tone: 'muted',
  },
]

export default function MemberStatsCards({ summary }) {
  return (
    <section className="member-stats" aria-label="Thống kê thành viên">
      {stats.map(({ key, label, icon: Icon, tone }) => (
        <article key={key} className={`member-stat-card member-stat-card--${tone}`}>
          <span className="member-stat-card__icon-wrap" aria-hidden="true">
            <Icon size={18} />
          </span>
          <div>
            <p className="member-stat-card__label">{label}</p>
            <p className="member-stat-card__value">{summary?.[key] ?? 0}</p>
          </div>
        </article>
      ))}
    </section>
  )
}
