import { Link, Outlet } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { PROJECT_STATUS_LABELS } from '../../constants/projects'
import LoadingScreen from '../common/LoadingScreen'
import { useProject } from '../../hooks/useProject'
import ProjectTabs from './ProjectTabs'

export default function ProjectLayout() {
  const { project, notFound, isLoading } = useProject()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (notFound) {
    return (
      <div className="page">
        <p className="page-placeholder__text">
          Dự án không tồn tại hoặc bạn không có quyền truy cập.
        </p>
        <Link to="/projects" className="btn btn--ghost">
          Quay lại danh sách dự án
        </Link>
      </div>
    )
  }

  if (!project) {
    return null
  }

  return (
    <div className="project-layout">
      <div className="project-layout__header">
        <Link to="/projects" className="project-layout__back">
          <ArrowLeft size={16} aria-hidden="true" />
          Projects
        </Link>

        <div className="project-layout__title-row">
          <div>
            <p className="project-layout__code">{project.code}</p>
            <h1 className="project-layout__title">{project.name}</h1>
            <p className="project-layout__meta">
              {project.teamName} · {project.managerName}
            </p>
          </div>
          <span className={`project-status project-status--${project.status.toLowerCase()}`}>
            {PROJECT_STATUS_LABELS[project.status] ?? project.status}
          </span>
        </div>

        <ProjectTabs />
      </div>

      <div className="project-layout__content">
        <Outlet />
      </div>
    </div>
  )
}
