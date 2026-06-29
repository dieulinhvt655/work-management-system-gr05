import PermissionRoute from '../../../routes/PermissionRoute'
import { PERMISSIONS } from '../../../constants/permissions'
import { useProject } from '../../../hooks/useProject'

function ProjectTabShell({ title, children }) {
  const { project } = useProject()

  return (
    <div className="project-tab-page">
      {children ?? (
        <p className="project-tab-page__placeholder">
          Nội dung tab <strong>{title}</strong> cho dự án{' '}
          <strong>{project?.name}</strong> sẽ được triển khai tiếp.
        </p>
      )}
    </div>
  )
}

export function ProjectOverviewPage() {
  const { project } = useProject()

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <ProjectTabShell title="Tổng quan">
        <div className="project-overview-grid">
          <div className="project-stat-card">
            <p className="project-stat-card__label">Trạng thái</p>
            <p className="project-stat-card__value">{project?.status ?? '—'}</p>
          </div>
          <div className="project-stat-card">
            <p className="project-stat-card__label">Thành viên</p>
            <p className="project-stat-card__value">{project?.memberCount ?? 0}</p>
          </div>
          <div className="project-stat-card">
            <p className="project-stat-card__label">Project Manager</p>
            <p className="project-stat-card__value project-stat-card__text">
              {project?.managerName ?? '—'}
            </p>
          </div>
          <div className="project-stat-card project-stat-card--wide">
            <p className="project-stat-card__label">Mô tả</p>
            <p className="project-stat-card__text">{project?.description}</p>
          </div>
          {project?.objective && (
            <div className="project-stat-card project-stat-card--wide">
              <p className="project-stat-card__label">Mục tiêu</p>
              <p className="project-stat-card__text">{project.objective}</p>
            </div>
          )}
        </div>
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectMyTasksPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.MYWORK_READ}>
      <ProjectTabShell title="My Tasks" />
    </PermissionRoute>
  )
}

export function ProjectBacklogPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.BACKLOG_READ}>
      <ProjectTabShell title="Product Backlog" />
    </PermissionRoute>
  )
}

export function ProjectSprintPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.SPRINT_READ}>
      <ProjectTabShell title="Sprint" />
    </PermissionRoute>
  )
}

export function ProjectMembersPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <ProjectTabShell title="Thành viên dự án" />
    </PermissionRoute>
  )
}

export function ProjectDocsPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_DOC_READ}>
      <ProjectTabShell title="Docs" />
    </PermissionRoute>
  )
}

export function ProjectActivityPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_ACTIVITY_READ}>
      <ProjectTabShell title="Activity Log" />
    </PermissionRoute>
  )
}
