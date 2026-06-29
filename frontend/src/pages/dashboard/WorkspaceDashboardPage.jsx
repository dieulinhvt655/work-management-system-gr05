import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Briefcase,
  Shield,
  Users,
  UserSquare2,
} from 'lucide-react'
import {
  fetchOrganizationMemberSummary,
} from '../../api/organizationMembersApi'
import { fetchRoles } from '../../api/rolesApi'
import { fetchTeamSummary } from '../../api/teamsApi'
import {
  fetchWorkspaceActivityLogs,
  fetchWorkspaces,
} from '../../api/workspacesApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PERMISSIONS } from '../../constants/permissions'
import { ROLE_SCOPE } from '../../constants/roles'
import { WORKSPACE_STATUS_LABELS } from '../../constants/workspaces'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'
import { formatLastActivity } from '../admin/users/utils/formatUserDate'

function StatCard({ icon: Icon, label, value, to, tone = 'default' }) {
  const content = (
    <>
      <span className={`wo-stat-card__icon wo-stat-card__icon--${tone}`} aria-hidden="true">
        <Icon size={20} />
      </span>
      <div>
        <p className="wo-stat-card__label">{label}</p>
        <p className="wo-stat-card__value">{value}</p>
      </div>
    </>
  )

  if (to) {
    return (
      <Link to={to} className={`wo-stat-card wo-stat-card--${tone}`}>
        {content}
      </Link>
    )
  }

  return <article className={`wo-stat-card wo-stat-card--${tone}`}>{content}</article>
}

function ActionCard({ title, description, to, cta }) {
  return (
    <article className="wo-action-card">
      <h2 className="wo-action-card__title">{title}</h2>
      <p className="wo-action-card__text">{description}</p>
      <Link to={to} className="wo-action-card__link">
        {cta}
      </Link>
    </article>
  )
}

export default function WorkspaceDashboardPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const workspaceId = user?.workspaceId

  const { data: workspaces = [], isLoading: workspacesLoading } = useQuery({
    queryKey: ['workspace-owner', 'workspaces'],
    queryFn: fetchWorkspaces,
    enabled: isAuthenticated && !authLoading,
  })

  const workspace = workspaces.find((item) => item.id === workspaceId) ?? workspaces[0]

  const { data: memberSummary } = useQuery({
    queryKey: ['organization', 'members', 'summary', workspace?.id],
    queryFn: () => fetchOrganizationMemberSummary(workspace?.id),
    enabled: Boolean(workspace?.id),
  })

  const { data: teamSummary } = useQuery({
    queryKey: ['teams', 'summary', workspace?.id],
    queryFn: () => fetchTeamSummary(workspace?.id),
    enabled: Boolean(workspace?.id),
  })

  const { data: workspaceRoles = [] } = useQuery({
    queryKey: ['workspace', 'roles', 'count'],
    queryFn: async () => {
      const roles = await fetchRoles()
      return roles.filter((role) => role.scope === ROLE_SCOPE.WORKSPACE)
    },
    enabled: Boolean(workspace?.id),
  })

  const { data: activityPayload } = useQuery({
    queryKey: ['workspace', 'activity', workspace?.id],
    queryFn: () => fetchWorkspaceActivityLogs(workspace.id, { size: 5 }),
    enabled: Boolean(workspace?.id),
  })

  if (authLoading || workspacesLoading) {
    return <LoadingScreen />
  }

  const activities = activityPayload?.items ?? []

  return (
    <PermissionRoute permission={PERMISSIONS.DASHBOARD_WORKSPACE_READ}>
      <div className="page page--wide workspace-dashboard-page">
        <header className="workspace-dashboard-page__header">
          <p className="workspace-dashboard-page__eyebrow">Workspace Owner</p>
          <h1 className="workspace-dashboard-page__title">
            Workspace Dashboard
          </h1>
          <p className="workspace-dashboard-page__subtitle">
            {workspace?.name
              ? `Tổng quan ${workspace.name} — thành viên, phòng ban và vai trò.`
              : 'Tổng quan workspace bạn đang quản lý.'}
          </p>
        </header>

        {workspace && (
          <section className="workspace-dashboard-page__meta">
            <span className="workspace-dashboard-page__badge">
              {WORKSPACE_STATUS_LABELS[workspace.status] ?? workspace.status}
            </span>
            {workspace.description && (
              <p className="workspace-dashboard-page__description">
                {workspace.description}
              </p>
            )}
          </section>
        )}

        <section className="workspace-dashboard-page__stats" aria-label="Thống kê">
          <StatCard
            icon={Users}
            label="Thành viên"
            value={String(memberSummary?.total ?? workspace?.memberCount ?? 0).padStart(2, '0')}
            to="/members"
            tone="primary"
          />
          <StatCard
            icon={UserSquare2}
            label="Đang hoạt động"
            value={String(memberSummary?.active ?? 0).padStart(2, '0')}
            to="/members"
            tone="success"
          />
          <StatCard
            icon={Briefcase}
            label="Phòng ban"
            value={String(teamSummary?.total ?? 0).padStart(2, '0')}
            to="/teams"
            tone="info"
          />
          <StatCard
            icon={Shield}
            label="Vai trò"
            value={String(workspaceRoles.length).padStart(2, '0')}
            to="/workspace/roles"
            tone="neutral"
          />
        </section>

        <section className="workspace-dashboard-page__grid">
          <div className="workspace-dashboard-page__actions">
            <ActionCard
              title="Teams"
              description="Xem danh sách phòng ban hoặc tạo phòng ban mới trong workspace."
              to="/teams"
              cta="Mở Teams →"
            />
            <ActionCard
              title="Members"
              description="Quản lý thành viên, phân bổ phòng ban và trạng thái tham gia."
              to="/members"
              cta="Quản lý Members →"
            />
            <ActionCard
              title="Account Management"
              description="Tạo và quản lý tài khoản trong phạm vi workspace."
              to="/admin/users"
              cta="Quản lý Account →"
            />
            <ActionCard
              title="Roles & Permissions"
              description="Cấu hình vai trò và quyền truy cập trong phạm vi workspace."
              to="/workspace/roles"
              cta="Mở Roles →"
            />
          </div>

          <aside className="workspace-dashboard-page__activity">
            <div className="workspace-dashboard-page__activity-header">
              <h2>Hoạt động gần đây</h2>
              <Link to="/workspace/activity">Xem tất cả</Link>
            </div>

            {activities.length === 0 ? (
              <p className="workspace-dashboard-page__empty">
                Chưa có hoạt động nào trong workspace.
              </p>
            ) : (
              <ul className="workspace-dashboard-page__activity-list">
                {activities.map((activity) => (
                  <li key={activity.id}>
                    <p>{activity.action ?? activity.description ?? 'Hoạt động'}</p>
                    <time dateTime={activity.createdAt}>
                      {formatLastActivity(activity.createdAt)}
                    </time>
                  </li>
                ))}
              </ul>
            )}
          </aside>
        </section>
      </div>
    </PermissionRoute>
  )
}
