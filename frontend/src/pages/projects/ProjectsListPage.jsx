import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { fetchProjects } from '../../api/projectsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import PermissionGate from '../../components/common/PermissionGate'
import { PROJECT_STATUS_LABELS } from '../../constants/projects'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'

function StatusBadge({ status }) {
  return (
    <span className={`project-status project-status--${status.toLowerCase()}`}>
      {PROJECT_STATUS_LABELS[status] ?? status}
    </span>
  )
}

export default function ProjectsListPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const workspaceId = user?.workspaceId

  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects', workspaceId],
    queryFn: () => fetchProjects(workspaceId),
    enabled: isAuthenticated && !authLoading,
  })

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <div className="page page--wide">
        <PermissionGate permission={PERMISSIONS.PROJECT_CREATE}>
          <header className="page__header page__header--row page__header--actions-only">
            <button type="button" className="btn btn--primary" disabled>
              <Plus size={16} aria-hidden="true" />
              Tạo dự án
            </button>
          </header>
        </PermissionGate>

        {projects.length === 0 ? (
          <div className="project-list-empty">
            <p className="project-list-empty__title">Chưa có dự án</p>
            <p className="project-list-empty__text">
              Tạo team trước, sau đó thêm dự án vào team trong workspace.
            </p>
            <Link to="/teams" className="btn btn--ghost">
              Mở Teams
            </Link>
          </div>
        ) : (
          <div className="project-list">
            {projects.map((project) => (
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
