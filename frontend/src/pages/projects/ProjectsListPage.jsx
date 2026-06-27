import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { MOCK_PROJECTS, PROJECT_STATUS_LABELS } from '../../constants/mock/projects'
import { PERMISSIONS } from '../../constants/permissions'
import PermissionGate from '../../components/common/PermissionGate'
import PermissionRoute from '../../routes/PermissionRoute'

function StatusBadge({ status }) {
  return (
    <span className={`project-status project-status--${status.toLowerCase()}`}>
      {PROJECT_STATUS_LABELS[status] ?? status}
    </span>
  )
}

export default function ProjectsListPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <div className="page">
        <PermissionGate permission={PERMISSIONS.PROJECT_CREATE}>
          <header className="page__header page__header--row page__header--actions-only">
            <button type="button" className="btn btn--primary">
              <Plus size={16} aria-hidden="true" />
              Tạo dự án
            </button>
          </header>
        </PermissionGate>

        <div className="project-list">
          {MOCK_PROJECTS.map((project) => (
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
                <span>{project.taskCount} tasks</span>
              </div>

              <div className="project-card__progress">
                <div className="project-card__progress-bar">
                  <div
                    className="project-card__progress-fill"
                    style={{ width: `${project.progress}%` }}
                  />
                </div>
                <span className="project-card__progress-label">
                  {project.progress}%
                </span>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </PermissionRoute>
  )
}
