import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { fetchProjects, fetchProjectsForCurrentTeam } from '../../api/projectsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PROJECT_STATUS_LABELS } from '../../constants/projects'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import { usePermission } from '../../hooks/usePermission'
import { isTeamLeaderUser } from '../../utils/userRoleUtils'
import PermissionRoute from '../../routes/PermissionRoute'
import { getManagedTeamId } from '../../utils/teamLeaderScope'

function StatusBadge({ status }) {
  return (
    <span className={`project-status project-status--${status.toLowerCase()}`}>
      {PROJECT_STATUS_LABELS[status] ?? status}
    </span>
  )
}

export default function ProjectsListPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const { can } = usePermission()
  const workspaceId = user?.workspaceId
  const managedTeamId = getManagedTeamId(user)
  const canCreateProject =
    can(PERMISSIONS.PROJECT_CREATE) || isTeamLeaderUser(user)

  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects', workspaceId, user?.id],
    queryFn: () =>
      managedTeamId
        ? fetchProjectsForCurrentTeam(workspaceId, managedTeamId, {}, user)
        : fetchProjects(workspaceId, {}, user),
    enabled: isAuthenticated && !authLoading,
  })

  const visibleProjects = useMemo(() => {
    if (user?.isSystemAdmin) return projects

    if (isTeamLeaderUser(user)) {
      const ledTeamIds = new Set(
        (user.ledTeamIds ?? []).map(String).filter(Boolean),
      )

      if (ledTeamIds.size === 0 && user.teamId) {
        ledTeamIds.add(String(user.teamId))
      }

      return projects.filter((project) => ledTeamIds.has(String(project.teamId)))
    }

    return projects.filter(
      (project) =>
        project.isCurrentUserTeamLeader ||
        project.isCurrentUserProjectManager ||
        project.currentMember,
    )
  }, [projects, user])

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <div className="page page--wide">
        {canCreateProject ? (
          <header className="page__header page__header--row project-page-header">
            <div>
              <h1 className="page__title">Danh sách dự án</h1>
              <p className="page__subtitle">
                {isTeamLeaderUser(user)
                  ? 'Tất cả dự án trong phòng ban bạn quản lý.'
                  : 'Theo dõi project theo vai trò của bạn trong team và dự án.'}
              </p>
            </div>
            <Link
              to="/projects/create"
              className="btn btn--primary page-header-btn"
            >
              <Plus size={16} aria-hidden="true" />
              Tạo dự án mới
            </Link>
          </header>
        ) : (
          <header className="page__header">
            <h1 className="page__title">Danh sách dự án</h1>
            <p className="page__subtitle">
              Tất cả dự án trong phòng ban bạn quản lý.
            </p>
          </header>
        )}

        {visibleProjects.length === 0 ? (
          <div className="project-list-empty">
            <p className="project-list-empty__title">Chưa có dự án</p>
            <p className="project-list-empty__text">
              {isTeamLeaderUser(user)
                ? 'Chưa có dự án nào trong phòng ban của bạn.'
                : 'Chưa có project nào thuộc team bạn quản lý hoặc project bạn tham gia.'}
            </p>
            {canCreateProject && (
              <Link to="/projects/create" className="btn btn--ghost">
                Tạo dự án mới
              </Link>
            )}
          </div>
        ) : (
          <div className="project-list">
            {visibleProjects.map((project) => (
              <Link
                key={project.id}
                to={`/projects/${project.id}`}
                className="project-card"
              >
                <div className="project-card__header">
                  <div>
                    <p className="project-card__code">{project.code}</p>
                    <h2 className="project-card__name">{project.name}</h2>
                  </div>
                  <StatusBadge status={project.status} />
                </div>

                <p className="project-card__desc">{project.description}</p>

                <div className="project-card__meta">
                  <span>{project.teamName}</span>
                  <span>{project.memberCount} thành viên</span>
                  {project.isCurrentUserTeamLeader && <span>Team Leader</span>}
                  {project.isCurrentUserProjectManager && (
                    <span>Project Manager</span>
                  )}
                  {project.managerName !== '—' && (
                    <span>PM: {project.managerName}</span>
                  )}
                </div>
              </Link>
            ))}
          </div>
        )}

      </div>
    </PermissionRoute>
  )
}
