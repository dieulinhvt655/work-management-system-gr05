import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Briefcase, FolderKanban, Plus, Users } from 'lucide-react'
import { fetchProjects } from '../../api/projectsApi'
import { fetchTeamById } from '../../api/teamsApi'
import { fetchAccessibleWorkspaceInfo } from '../../api/workspacesApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'

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

export default function TeamLeaderDashboardPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const workspaceId = user?.workspaceId
  const primaryTeamId = user?.ledTeamIds?.[0] ?? user?.teamId

  const { data: workspace, isLoading: workspaceLoading } = useQuery({
    queryKey: ['workspace', 'accessible', workspaceId, user?.teamName],
    queryFn: () =>
      fetchAccessibleWorkspaceInfo(workspaceId, {
        teamName: user?.teamName,
      }),
    enabled: isAuthenticated && !authLoading && Boolean(workspaceId),
  })

  const { data: team, isLoading: teamLoading } = useQuery({
    queryKey: ['teams', workspaceId, primaryTeamId],
    queryFn: () => fetchTeamById(workspaceId, primaryTeamId),
    enabled: Boolean(workspaceId && primaryTeamId),
  })

  const { data: projects = [], isLoading: projectsLoading } = useQuery({
    queryKey: ['projects', workspaceId, user?.id, 'team-leader'],
    queryFn: () => fetchProjects(workspaceId, {}, user),
    enabled: isAuthenticated && !authLoading && Boolean(workspaceId),
  })

  const teamProjects = useMemo(() => {
    const ledTeamIds = new Set(
      (user?.ledTeamIds ?? []).map(String).filter(Boolean),
    )

    if (ledTeamIds.size === 0 && user?.teamId) {
      ledTeamIds.add(String(user.teamId))
    }

    return projects.filter((project) => ledTeamIds.has(String(project.teamId)))
  }, [projects, user?.ledTeamIds, user?.teamId])

  if (authLoading || workspaceLoading || teamLoading || projectsLoading) {
    return <LoadingScreen />
  }

  const teamLabel = user?.ledTeamNames?.join(', ') ?? user?.teamName ?? '—'
  const memberCount = team?.memberCount ?? team?.members?.length ?? 0

  return (
    <PermissionRoute permission={PERMISSIONS.DASHBOARD_TEAM_READ}>
      <div className="page page--wide workspace-dashboard-page">
        <header className="workspace-dashboard-page__header">
          <p className="workspace-dashboard-page__eyebrow">Team Leader</p>
          <h1 className="workspace-dashboard-page__title">Team Dashboard</h1>
          <p className="workspace-dashboard-page__subtitle">
            {workspace?.name
              ? `Tổng quan phòng ban ${teamLabel} trong ${workspace.name}.`
              : `Tổng quan phòng ban ${teamLabel} bạn đang quản lý.`}
          </p>
        </header>

        <section className="workspace-dashboard-page__stats" aria-label="Thống kê">
          <StatCard
            icon={Users}
            label="Thành viên team"
            value={String(memberCount).padStart(2, '0')}
            to="/projects"
            tone="primary"
          />
          <StatCard
            icon={FolderKanban}
            label="Dự án"
            value={String(teamProjects.length).padStart(2, '0')}
            to="/projects"
            tone="info"
          />
          <StatCard
            icon={Briefcase}
            label="Workspace"
            value={workspace?.memberCount != null ? String(workspace.memberCount).padStart(2, '0') : '—'}
            to="/workspace/info"
            tone="neutral"
          />
        </section>

        <section className="workspace-dashboard-page__grid">
          <div className="workspace-dashboard-page__actions">
            <ActionCard
              title="Thông tin Workspace"
              description="Xem thông tin workspace nơi bạn đang làm việc."
              to="/workspace/info"
              cta="Mở thông tin →"
            />
            <ActionCard
              title="Tạo dự án mới"
              description="Khởi tạo project mới trong phòng ban bạn quản lý."
              to="/projects/create"
              cta="Tạo dự án →"
            />
            <ActionCard
              title="Danh sách dự án"
              description="Theo dõi và mở chi tiết các project trong team."
              to="/projects"
              cta="Xem danh sách →"
            />
          </div>

          <aside className="workspace-dashboard-page__activity">
            <div className="workspace-dashboard-page__activity-header">
              <h2>Dự án gần đây</h2>
              <Link to="/projects">Xem tất cả</Link>
            </div>

            {teamProjects.length === 0 ? (
              <p className="workspace-dashboard-page__empty">
                Chưa có dự án nào trong team.{' '}
                <Link to="/projects/create" className="wo-action-card__link">
                  Tạo dự án đầu tiên
                </Link>
              </p>
            ) : (
              <ul className="workspace-dashboard-page__activity-list">
                {teamProjects.slice(0, 5).map((project) => (
                  <li key={project.id}>
                    <Link to={`/projects/${project.id}`}>{project.name}</Link>
                    <span>{project.code}</span>
                  </li>
                ))}
              </ul>
            )}
          </aside>
        </section>

        <div className="teams-page__actions" style={{ marginTop: '1.5rem' }}>
          <Link to="/projects/create" className="teams-page__cta">
            <Plus size={16} aria-hidden="true" />
            Tạo dự án mới
          </Link>
        </div>
      </div>
    </PermissionRoute>
  )
}
