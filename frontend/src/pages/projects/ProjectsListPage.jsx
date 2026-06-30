import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { createProject, fetchProjects } from '../../api/projectsApi'
import { fetchTeams } from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import PermissionGate from '../../components/common/PermissionGate'
import Toast from '../../components/common/Toast'
import { PROJECT_STATUS_LABELS } from '../../constants/projects'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import { usePermission } from '../../hooks/usePermission'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import ProjectFormModal from './components/ProjectFormModal'

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
  const queryClient = useQueryClient()
  const workspaceId = user?.workspaceId
  const [showCreate, setShowCreate] = useState(false)
  const [formError, setFormError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects', workspaceId, user?.id],
    queryFn: () => fetchProjects(workspaceId, {}, user),
    enabled: isAuthenticated && !authLoading,
  })

  const { data: teams = [], isLoading: teamsLoading } = useQuery({
    queryKey: ['teams', workspaceId],
    queryFn: () => fetchTeams(workspaceId),
    enabled: isAuthenticated && !authLoading && can(PERMISSIONS.PROJECT_CREATE),
  })

  const visibleProjects = useMemo(() => {
    if (user?.isSystemAdmin) return projects

    return projects.filter(
      (project) =>
        project.isCurrentUserTeamLeader ||
        project.isCurrentUserProjectManager ||
        project.currentMember,
    )
  }, [projects, user?.isSystemAdmin])

  const teamOptions = useMemo(() => {
    if (user?.isSystemAdmin) return teams
    return teams.filter((team) =>
      (team.members ?? []).some(
        (member) =>
          member.isLeader && String(member.userId) === String(user?.id),
      ),
    )
  }, [teams, user])

  const createMutation = useMutation({
    mutationFn: (values) => {
      const team = teamOptions.find(
        (entry) => String(entry.id) === String(values.teamId),
      )
      return createProject(team?.workspaceId ?? workspaceId, values.teamId, values)
    },
    onSuccess: () => {
      setShowCreate(false)
      setFormError('')
      setToastMessage('Project đã được tạo.')
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (error) => {
      setFormError(getErrorMessage(error, 'Không thể tạo project.'))
    },
  })

  const handleCreate = (values) => {
    setFormError('')
    createMutation.mutate(values)
  }

  if (authLoading || isLoading || teamsLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide">
        <PermissionGate permission={PERMISSIONS.PROJECT_CREATE}>
          <header className="page__header page__header--row project-page-header">
            <div>
              <h1 className="page__title">Projects</h1>
              <p className="page__subtitle">
                Theo dõi project theo vai trò của bạn trong team và dự án.
              </p>
            </div>
            <button
              type="button"
              className="btn btn--primary page-header-btn"
              onClick={() => {
                setFormError('')
                setShowCreate(true)
              }}
              disabled={teamOptions.length === 0}
            >
              <Plus size={16} aria-hidden="true" />
              Tạo dự án
            </button>
          </header>
        </PermissionGate>

        {visibleProjects.length === 0 ? (
          <div className="project-list-empty">
            <p className="project-list-empty__title">Chưa có dự án</p>
            <p className="project-list-empty__text">
              Chưa có project nào thuộc team bạn quản lý hoặc project bạn tham
              gia.
            </p>
            <PermissionGate permission={PERMISSIONS.TEAM_READ}>
              <Link to="/teams" className="btn btn--ghost">
                Mở Teams
              </Link>
            </PermissionGate>
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

        {showCreate && (
          <ProjectFormModal
            title="Tạo project mới"
            description="Team Leader tạo project trong team mình quản lý."
            teams={teamOptions}
            initialTeamId={teamOptions[0]?.id ?? ''}
            onClose={() => {
              if (!createMutation.isPending) {
                setShowCreate(false)
                setFormError('')
              }
            }}
            onSave={handleCreate}
            isSaving={createMutation.isPending}
            saveError={formError}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
