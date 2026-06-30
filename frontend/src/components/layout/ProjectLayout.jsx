import { useState } from 'react'
import { Link, Outlet } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Archive, ArrowLeft, CheckCircle2, Pencil, PlayCircle } from 'lucide-react'
import {
  activateProject,
  archiveProject,
  completeProject,
  updateProject,
} from '../../api/projectsApi'
import { fetchTeams } from '../../api/teamsApi'
import { PROJECT_STATUS, PROJECT_STATUS_LABELS } from '../../constants/projects'
import { PERMISSIONS } from '../../constants/permissions'
import { usePermission } from '../../hooks/usePermission'
import { getErrorMessage } from '../../utils/getErrorMessage'
import Toast from '../common/Toast'
import LoadingScreen from '../common/LoadingScreen'
import Button from '../ui/Button'
import ConfirmDialog from '../ui/ConfirmDialog'
import { useProject } from '../../hooks/useProject'
import ProjectFormModal from '../../pages/projects/components/ProjectFormModal'
import ProjectTabs from './ProjectTabs'

export default function ProjectLayout() {
  const { project, notFound, isLoading } = useProject()
  const { can } = usePermission()
  const queryClient = useQueryClient()
  const [showEdit, setShowEdit] = useState(false)
  const [pendingStatusAction, setPendingStatusAction] = useState(null)
  const [actionError, setActionError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  const { data: teams = [] } = useQuery({
    queryKey: ['teams', project?.workspaceId],
    queryFn: () => fetchTeams(project?.workspaceId),
    enabled: Boolean(project?.workspaceId) && showEdit,
  })

  const invalidateProject = () => {
    queryClient.invalidateQueries({ queryKey: ['projects'] })
  }

  const updateMutation = useMutation({
    mutationFn: (values) => updateProject(project, values),
    onSuccess: () => {
      setShowEdit(false)
      setActionError('')
      setToastMessage('Project đã được cập nhật.')
      invalidateProject()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật project.'))
    },
  })

  const statusMutation = useMutation({
    mutationFn: (action) => action.mutation(project),
    onSuccess: (_, action) => {
      setPendingStatusAction(null)
      setActionError('')
      setToastMessage(action.successMessage)
      invalidateProject()
    },
    onError: (error, action) => {
      setActionError(getErrorMessage(error, action.errorMessage))
    },
  })

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
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

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

        <div className="project-layout__actions">
          {(can(PERMISSIONS.PROJECT_MANAGE) || project.isCurrentUserTeamLeader) && (
            <Button
              type="button"
              variant="ghost"
              className="project-layout__action"
              onClick={() => {
                setActionError('')
                setShowEdit(true)
              }}
            >
              <Pencil size={16} aria-hidden="true" />
              Chỉnh sửa
            </Button>
          )}

          {(can(PERMISSIONS.PROJECT_MANAGE) || project.isCurrentUserTeamLeader) &&
            project.status === PROJECT_STATUS.DRAFT && (
              <Button
                type="button"
                variant="primary"
                className="project-layout__action"
                onClick={() =>
                  setPendingStatusAction({
                    title: 'Kích hoạt project?',
                    confirmLabel: 'Kích hoạt',
                    mutation: activateProject,
                    successMessage: 'Project đã được kích hoạt.',
                    errorMessage: 'Không thể kích hoạt project.',
                    tone: 'primary',
                  })
                }
              >
                <PlayCircle size={16} aria-hidden="true" />
                Kích hoạt
              </Button>
            )}

          {can(PERMISSIONS.PROJECT_MANAGE) &&
            project.status === PROJECT_STATUS.ACTIVE && (
              <Button
                type="button"
                variant="primary"
                className="project-layout__action"
                onClick={() =>
                  setPendingStatusAction({
                    title: 'Kết thúc project?',
                    confirmLabel: 'Kết thúc',
                    mutation: completeProject,
                    successMessage: 'Project đã được chuyển sang Completed.',
                    errorMessage: 'Không thể kết thúc project.',
                    tone: 'primary',
                  })
                }
              >
                <CheckCircle2 size={16} aria-hidden="true" />
                Kết thúc
              </Button>
            )}

          {(can(PERMISSIONS.PROJECT_CREATE) || project.isCurrentUserTeamLeader) &&
            project.status === PROJECT_STATUS.COMPLETED && (
              <Button
                type="button"
                variant="ghost"
                className="project-layout__action"
                onClick={() =>
                  setPendingStatusAction({
                    title: 'Lưu trữ project?',
                    confirmLabel: 'Lưu trữ',
                    mutation: archiveProject,
                    successMessage: 'Project đã được lưu trữ.',
                    errorMessage: 'Không thể lưu trữ project.',
                    tone: 'danger',
                  })
                }
              >
                <Archive size={16} aria-hidden="true" />
                Lưu trữ
              </Button>
            )}
        </div>

        <ProjectTabs />
      </div>

      <div className="project-layout__content">
        <Outlet />
      </div>

      {showEdit && (
        <ProjectFormModal
          title="Cập nhật project"
          description={`${project.code} · ${project.name}`}
          project={project}
          teams={teams}
          onClose={() => {
            if (!updateMutation.isPending) {
              setShowEdit(false)
              setActionError('')
            }
          }}
          onSave={(values) => updateMutation.mutate(values)}
          isSaving={updateMutation.isPending}
          saveError={actionError}
        />
      )}

      {pendingStatusAction && (
        <ConfirmDialog
          title={pendingStatusAction.title}
          confirmLabel={pendingStatusAction.confirmLabel}
          cancelLabel="Hủy"
          tone={pendingStatusAction.tone}
          isSaving={statusMutation.isPending}
          error={actionError}
          onCancel={() => {
            if (!statusMutation.isPending) {
              setPendingStatusAction(null)
              setActionError('')
            }
          }}
          onConfirm={() => statusMutation.mutate(pendingStatusAction)}
        />
      )}
    </div>
  )
}
