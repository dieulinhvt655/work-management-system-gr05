import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchOrganizationMemberById } from '../../../api/organizationMembersApi'
import UserAvatar from '../../../components/common/UserAvatar'
import Drawer from '../../../components/ui/Drawer'
import { USER_ROLE_LABELS } from '../../../constants/users'
import MemberOrgStatusBadge from './MemberOrgStatusBadge'
import {
  buildMemberRecentHistory,
  getProjectParticipationType,
  RECENT_HISTORY_TYPE_LABELS,
} from '../utils/memberRecentHistory'

function DrawerSection({ title, children }) {
  return (
    <section className="member-drawer__section">
      <h3 className="member-drawer__section-title">{title}</h3>
      {children}
    </section>
  )
}

function DrawerField({ label, value }) {
  return (
    <div className="member-drawer__field">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

export default function MemberDetailDrawer({ memberId, onClose }) {
  const { data: member, isLoading } = useQuery({
    queryKey: ['organization', 'members', memberId],
    queryFn: () => fetchOrganizationMemberById(memberId),
    enabled: Boolean(memberId),
  })

  if (isLoading || !member) {
    return (
      <Drawer title="Chi tiết thành viên" onClose={onClose}>
        <p className="member-drawer__empty">Đang tải thông tin...</p>
      </Drawer>
    )
  }

  const currentProject = member.currentProject
  const recentHistory = buildMemberRecentHistory(member)

  return (
    <Drawer
      title={member.fullName}
      subtitle={member.email}
      onClose={onClose}
      footer={
        <Link to={`/members/${member.id}`} className="member-drawer__detail-link">
          Mở trang chi tiết đầy đủ
        </Link>
      }
    >
      <div className="member-drawer__hero">
        <UserAvatar fullName={member.fullName} size="lg" />
        <div>
          <p className="member-drawer__code">{member.employeeCode}</p>
          <MemberOrgStatusBadge status={member.organizationStatus} />
        </div>
      </div>

      <DrawerSection title="Thông tin thành viên">
        <dl className="member-drawer__fields">
          <DrawerField label="Họ tên" value={member.fullName} />
          <DrawerField label="Email" value={member.email} />
          <DrawerField label="Mã nhân viên" value={member.employeeCode} />
          <DrawerField label="Team / Department" value={member.teamName} />
          <DrawerField label="Vị trí" value={member.position} />
          <DrawerField
            label="Vai trò tổ chức"
            value={USER_ROLE_LABELS[member.role] ?? member.role}
          />
          <DrawerField
            label="Trạng thái"
            value={<MemberOrgStatusBadge status={member.organizationStatus} />}
          />
        </dl>
      </DrawerSection>

      <DrawerSection title="Thông tin phân bổ">
        <dl className="member-drawer__fields">
          <DrawerField
            label="Dự án đang tham gia"
            value={currentProject?.name ?? 'Chưa phân bổ'}
          />
          <DrawerField
            label="Vai trò trong dự án"
            value={currentProject?.role ?? '—'}
          />
          <DrawerField
            label="Project Manager / Contributor"
            value={
              currentProject
                ? getProjectParticipationType(currentProject.role)
                : '—'
            }
          />
        </dl>
      </DrawerSection>

      <DrawerSection title="Lịch sử gần đây">
        {recentHistory.length === 0 ? (
          <p className="member-drawer__empty">Chưa có lịch sử thay đổi.</p>
        ) : (
          <ul className="member-drawer__history">
            {recentHistory.map((entry) => (
              <li key={entry.id} className="member-drawer__history-item">
                <span className="member-drawer__history-type">
                  {RECENT_HISTORY_TYPE_LABELS[entry.type] ?? entry.type}
                </span>
                <p className="member-drawer__history-message">{entry.message}</p>
                <p className="member-drawer__history-meta">{entry.meta}</p>
              </li>
            ))}
          </ul>
        )}
      </DrawerSection>
    </Drawer>
  )
}
