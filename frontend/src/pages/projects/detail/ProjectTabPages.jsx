import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  CalendarDays,
  CheckCircle2,
  FileText,
  FileUp,
  FolderKanban,
  ListChecks,
  Plus,
  Target,
  TrendingUp,
  UserCheck,
  Users,
} from 'lucide-react'
import {
  addProjectMember,
  createBacklogItem,
  createBacklogItemTask,
  fetchBacklogItemTasks,
  fetchProjectActivityLogs,
  fetchProjectAttachments,
  fetchProjectBacklog,
  fetchProjectBacklogItems,
  fetchProjectDashboard,
  fetchProjectMembers,
  updateProject,
  updateProjectMember,
  uploadProjectAttachment,
} from '../../../api/projectsApi'
import { fetchRoles } from '../../../api/usersApi'
import { fetchTeamMembers } from '../../../api/teamsApi'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'
import SelectField from '../../../components/ui/SelectField'
import TextField from '../../../components/ui/TextField'
import { PERMISSIONS } from '../../../constants/permissions'
import { PROJECT_STATUS, PROJECT_STATUS_LABELS } from '../../../constants/projects'
import { usePermission } from '../../../hooks/usePermission'
import { useProject } from '../../../hooks/useProject'
import PermissionRoute from '../../../routes/PermissionRoute'
import { getErrorMessage } from '../../../utils/getErrorMessage'

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN').format(new Date(value))
}

function formatBytes(value) {
  const size = Number(value ?? 0)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${Math.round(size / 1024)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function formatActivityAction(log) {
  if (!log?.action) return 'đã cập nhật dự án'
  return log.action
    .toLowerCase()
    .replaceAll('_', ' ')
}

function splitDetailText(value) {
  if (!value || value === '—') return []
  return value
    .split(/\n|;|,/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function EmptyState({ children }) {
  return <p className="project-tab-empty">{children}</p>
}

function ProjectTabShell({ title, description, actions, children }) {
  return (
    <div className="project-tab-page">
      <header className="project-tab-page__header project-tab-page__header--row">
        <div>
          <h2>{title}</h2>
          {description && (
            <p className="project-tab-page__desc">{description}</p>
          )}
        </div>
        {actions}
      </header>
      {children}
    </div>
  )
}

function AddProjectMemberModal({
  project,
  teamMembers,
  roles,
  existingMembers,
  onClose,
  onSave,
  isSaving,
  error,
}) {
  const availableMembers = useMemo(() => {
    const existingTeamMemberIds = new Set(
      existingMembers.map((member) => String(member.teamMemberId)),
    )
    return teamMembers.filter(
      (member) => !existingTeamMemberIds.has(String(member.id)),
    )
  }, [existingMembers, teamMembers])

  const [values, setValues] = useState({
    teamMemberId: availableMembers[0]?.id ?? '',
    roleId: roles[0]?.id ?? '',
  })

  const handleSubmit = (event) => {
    event.preventDefault()
    onSave(values)
  }

  return (
    <Modal
      title="Thêm thành viên project"
      description={`${project.code} · ${project.name}`}
      onClose={onClose}
      size="md"
    >
      <form className="project-form" onSubmit={handleSubmit}>
        {error && (
          <p className="modal__error" role="alert">
            {error}
          </p>
        )}
        <SelectField
          id="project-member"
          label="Team Member"
          value={values.teamMemberId}
          onChange={(event) =>
            setValues((current) => ({
              ...current,
              teamMemberId: event.target.value,
            }))
          }
        >
          <option value="">Chọn thành viên</option>
          {availableMembers.map((member) => (
            <option key={member.id} value={member.id}>
              {member.fullName} · {member.email}
            </option>
          ))}
        </SelectField>
        <SelectField
          id="project-role"
          label="Vai trò trong project"
          value={values.roleId}
          onChange={(event) =>
            setValues((current) => ({ ...current, roleId: event.target.value }))
          }
        >
          <option value="">Chọn vai trò</option>
          {roles.map((role) => (
            <option key={role.id} value={role.id}>
              {role.name}
            </option>
          ))}
        </SelectField>
        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={isSaving || !values.teamMemberId || !values.roleId}
          >
            {isSaving ? 'Đang thêm...' : 'Thêm thành viên'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function BacklogItemModal({ onClose, onSave, isSaving, error }) {
  const [values, setValues] = useState({
    title: '',
    description: '',
    type: 'FEATURE',
    priority: 'MEDIUM',
    desiredDueDate: '',
  })

  const set = (key, value) =>
    setValues((current) => ({ ...current, [key]: value }))

  return (
    <Modal title="Thêm backlog item" onClose={onClose} size="md">
      <form
        className="project-form"
        onSubmit={(event) => {
          event.preventDefault()
          onSave(values)
        }}
      >
        {error && <p className="modal__error">{error}</p>}
        <TextField
          id="backlog-title"
          label="Tiêu đề"
          value={values.title}
          onChange={(event) => set('title', event.target.value)}
        />
        <div className="project-form__grid">
          <SelectField
            id="backlog-type"
            label="Loại"
            value={values.type}
            onChange={(event) => set('type', event.target.value)}
          >
            {['FEATURE', 'BUG', 'IMPROVEMENT', 'TASK', 'OTHER'].map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </SelectField>
          <SelectField
            id="backlog-priority"
            label="Độ ưu tiên"
            value={values.priority}
            onChange={(event) => set('priority', event.target.value)}
          >
            {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </SelectField>
        </div>
        <TextField
          id="backlog-due"
          type="date"
          label="Ngày mong muốn"
          value={values.desiredDueDate}
          onChange={(event) => set('desiredDueDate', event.target.value)}
        />
        <div className="field">
          <label className="field__label" htmlFor="backlog-description">
            Mô tả
          </label>
          <textarea
            id="backlog-description"
            className="field__input project-form__textarea"
            rows={3}
            value={values.description}
            onChange={(event) => set('description', event.target.value)}
          />
        </div>
        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving || !values.title.trim()}>
            {isSaving ? 'Đang lưu...' : 'Thêm item'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function TaskModal({ members, onClose, onSave, isSaving, error }) {
  const [values, setValues] = useState({
    title: '',
    description: '',
    priority: 'MEDIUM',
    assigneeMemberId: '',
    deadline: '',
  })

  const set = (key, value) =>
    setValues((current) => ({ ...current, [key]: value }))

  return (
    <Modal title="Thêm task" onClose={onClose} size="md">
      <form
        className="project-form"
        onSubmit={(event) => {
          event.preventDefault()
          onSave(values)
        }}
      >
        {error && <p className="modal__error">{error}</p>}
        <TextField
          id="task-title"
          label="Tiêu đề"
          value={values.title}
          onChange={(event) => set('title', event.target.value)}
        />
        <div className="project-form__grid">
          <SelectField
            id="task-priority"
            label="Độ ưu tiên"
            value={values.priority}
            onChange={(event) => set('priority', event.target.value)}
          >
            {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </SelectField>
          <SelectField
            id="task-assignee"
            label="Người nhận"
            value={values.assigneeMemberId}
            onChange={(event) => set('assigneeMemberId', event.target.value)}
          >
            <option value="">Chưa gán</option>
            {members.map((member) => (
              <option key={member.id} value={member.id}>
                {member.fullName}
              </option>
            ))}
          </SelectField>
        </div>
        <TextField
          id="task-deadline"
          type="date"
          label="Deadline"
          value={values.deadline}
          onChange={(event) => set('deadline', event.target.value)}
        />
        <div className="field">
          <label className="field__label" htmlFor="task-description">
            Mô tả
          </label>
          <textarea
            id="task-description"
            className="field__input project-form__textarea"
            rows={3}
            value={values.description}
            onChange={(event) => set('description', event.target.value)}
          />
        </div>
        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving || !values.title.trim()}>
            {isSaving ? 'Đang lưu...' : 'Thêm task'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

export function ProjectOverviewPage() {
  const { project } = useProject()
  const { data: dashboard } = useQuery({
    queryKey: ['projects', project?.id, 'dashboard'],
    queryFn: () => fetchProjectDashboard(project),
    enabled: Boolean(project),
  })
  const { data: activityLogs = [] } = useQuery({
    queryKey: ['projects', project?.id, 'activity-logs', 'overview'],
    queryFn: () => fetchProjectActivityLogs(project, { page: 0, size: 4 }),
    enabled: Boolean(project),
  })

  const objectiveItems = splitDetailText(project?.objective)
  const scopeItems = splitDetailText(project?.scope)
  const projectManager = project?.members?.find(
    (member) => String(member.id) === String(project?.managerMemberId),
  )
  const recentMembers = project?.members?.slice(0, 3) ?? []
  const workflowPercent = dashboard?.completionPercent ?? 0

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <ProjectTabShell title="Tổng quan">
        <div className="project-detail-stats">
          <article className="project-detail-stat">
            <span className="project-detail-stat__icon">
              <Users size={18} aria-hidden="true" />
            </span>
            <div>
              <p>Tổng thành viên</p>
              <strong>{project?.memberCount ?? 0}</strong>
            </div>
          </article>
          <article className="project-detail-stat">
            <span className="project-detail-stat__icon">
              <CheckCircle2 size={18} aria-hidden="true" />
            </span>
            <div>
              <p>Số task</p>
              <strong>{dashboard?.totalTasks ?? 0}</strong>
            </div>
          </article>
          <article className="project-detail-stat">
            <span className="project-detail-stat__icon">
              <TrendingUp size={18} aria-hidden="true" />
            </span>
            <div>
              <p>Workflow Status</p>
              <strong>{workflowPercent}%</strong>
            </div>
          </article>
          <article className="project-detail-stat">
            <span className="project-detail-stat__icon project-detail-stat__icon--warm">
              <FolderKanban size={18} aria-hidden="true" />
            </span>
            <div>
              <p>Backlog items</p>
              <strong>{dashboard?.totalPbis ?? 0}</strong>
            </div>
          </article>
        </div>

        <div className="project-detail-grid">
          <section className="project-detail-card project-detail-card--main">
            <div className="project-detail-field-grid">
              <div>
                <span>Tên dự án</span>
                <strong>{project?.name}</strong>
              </div>
              <div>
                <span>Mã dự án</span>
                <strong>{project?.code || '—'}</strong>
              </div>
            </div>

            <div className="project-detail-block">
              <span>Mô tả dự án</span>
              <p>{project?.description || '—'}</p>
            </div>

            <div className="project-detail-two-cols">
              <div className="project-detail-list">
                <h3>
                  <Target size={17} aria-hidden="true" />
                  Mục tiêu chính
                </h3>
                {objectiveItems.length > 0 ? (
                  objectiveItems.map((item) => <p key={item}>{item}</p>)
                ) : (
                  <p>Chưa cập nhật mục tiêu.</p>
                )}
              </div>

              <div className="project-detail-list">
                <h3>
                  <ListChecks size={17} aria-hidden="true" />
                  Phạm vi dự án
                </h3>
                {scopeItems.length > 0 ? (
                  scopeItems.map((item) => <p key={item}>{item}</p>)
                ) : (
                  <p>Chưa cập nhật phạm vi.</p>
                )}
              </div>
            </div>
          </section>

          <aside className="project-detail-side">
            <section className="project-detail-card">
              <h3>Thông tin bổ sung</h3>
              <div className="project-detail-info-list">
                <span>Trạng thái</span>
                <strong>{PROJECT_STATUS_LABELS[project?.status] ?? project?.status ?? '—'}</strong>
                <span>Ngày bắt đầu</span>
                <strong>{formatDate(project?.startDate)}</strong>
                <span>Ngày dự kiến kết thúc</span>
                <strong>{formatDate(project?.endDate)}</strong>
              </div>
              <div className="project-detail-manager">
                <UserCheck size={18} aria-hidden="true" />
                <div>
                  <span>Project Manager</span>
                  <strong>{project?.managerName ?? 'Chưa gán'}</strong>
                  {projectManager?.email && <small>{projectManager.email}</small>}
                </div>
              </div>
            </section>

            <section className="project-detail-card">
              <div className="project-detail-card__header">
                <h3>Nhóm tham gia</h3>
                <Link to={`/projects/${project?.id}/members`}>Chi tiết</Link>
              </div>
              <div className="project-detail-member-list">
                {recentMembers.length > 0 ? (
                  recentMembers.map((member) => (
                    <div key={member.id}>
                      <span>{member.fullName}</span>
                      <small>{member.roleName}</small>
                    </div>
                  ))
                ) : (
                  <p>Chưa có thành viên dự án.</p>
                )}
              </div>
            </section>

            <section className="project-detail-card">
              <h3>Hoạt động gần đây</h3>
              <div className="project-detail-activity">
                {activityLogs.length > 0 ? (
                  activityLogs.map((log) => (
                    <article key={log.id}>
                      <CalendarDays size={14} aria-hidden="true" />
                      <div>
                        <strong>{log.actorName}</strong>
                        <p>{formatActivityAction(log)}</p>
                        <small>{formatDate(log.createdAt)}</small>
                      </div>
                    </article>
                  ))
                ) : (
                  <p>Chưa có hoạt động nào.</p>
                )}
              </div>
            </section>

            {project?.status === PROJECT_STATUS.DRAFT && (
              <Link
                to={`/projects/${project.id}/members`}
                className="project-detail-card project-detail-card--link"
              >
                <FileText size={18} aria-hidden="true" />
                Hoàn thiện phân công trước khi kích hoạt
              </Link>
            )}
          </aside>
        </div>
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectMyTasksPage() {
  const { project } = useProject()
  const { data: backlogItems = [] } = useQuery({
    queryKey: ['projects', project?.id, 'backlog-items-with-tasks'],
    queryFn: async () => {
      const items = await fetchProjectBacklogItems(project)
      const groups = await Promise.all(
        items.map(async (item) => ({
          item,
          tasks: await fetchBacklogItemTasks(project, item.id),
        })),
      )
      return groups
    },
    enabled: Boolean(project),
  })

  const myTasks = backlogItems
    .flatMap((group) => group.tasks)
    .filter(
      (task) =>
        task.assigneeMemberId &&
        task.assigneeMemberId === project?.currentMember?.id,
    )

  return (
    <PermissionRoute permission={PERMISSIONS.MYWORK_READ}>
      <ProjectTabShell title="My Tasks">
        {myTasks.length === 0 ? (
          <EmptyState>Chưa có task nào được giao cho bạn trong project này.</EmptyState>
        ) : (
          <div className="project-data-list">
            {myTasks.map((task) => (
              <article key={task.id} className="project-data-row">
                <div>
                  <h3>{task.title}</h3>
                  <p>{task.pbiTitle}</p>
                </div>
                <span>{task.status}</span>
              </article>
            ))}
          </div>
        )}
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectBacklogPage() {
  const { project } = useProject()
  const { can } = usePermission()
  const queryClient = useQueryClient()
  const [showItemModal, setShowItemModal] = useState(false)
  const [taskItem, setTaskItem] = useState(null)
  const [actionError, setActionError] = useState('')
  const canManageBacklog =
    can(PERMISSIONS.BACKLOG_MANAGE) &&
    (project?.isCurrentUserTeamLeader || project?.isCurrentUserProjectManager)
  const { data: backlog } = useQuery({
    queryKey: ['projects', project?.id, 'backlog'],
    queryFn: () => fetchProjectBacklog(project),
    enabled: Boolean(project),
  })
  const { data: items = [] } = useQuery({
    queryKey: ['projects', project?.id, 'backlog-items'],
    queryFn: () => fetchProjectBacklogItems(project),
    enabled: Boolean(project),
  })
  const { data: members = [] } = useQuery({
    queryKey: ['projects', project?.id, 'members'],
    queryFn: () => fetchProjectMembers(project),
    enabled: Boolean(project) && canManageBacklog,
  })

  const invalidateBacklog = () => {
    queryClient.invalidateQueries({ queryKey: ['projects', project?.id] })
  }

  const createItemMutation = useMutation({
    mutationFn: (values) =>
      createBacklogItem(project, {
        ...values,
        proposerMemberId: project?.currentMember?.id,
      }),
    onSuccess: () => {
      setShowItemModal(false)
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể thêm backlog item.'))
    },
  })

  const createTaskMutation = useMutation({
    mutationFn: ({ item, values }) =>
      createBacklogItemTask(project, item.id, values),
    onSuccess: () => {
      setTaskItem(null)
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể thêm task.'))
    },
  })

  return (
    <PermissionRoute permission={PERMISSIONS.BACKLOG_READ}>
      <ProjectTabShell
        title="Product Backlog"
        description={backlog?.description}
        actions={
          canManageBacklog ? (
            <Button
              type="button"
              variant="primary"
              onClick={() => {
                setActionError('')
                setShowItemModal(true)
              }}
            >
              <Plus size={16} aria-hidden="true" />
              Thêm item
            </Button>
          ) : null
        }
      >
        {actionError && (
          <p className="modal__error" role="alert">
            {actionError}
          </p>
        )}
        {items.length === 0 ? (
          <EmptyState>Backlog chưa có item nào.</EmptyState>
        ) : (
          <div className="project-data-list">
            {items.map((item) => (
              <article key={item.id} className="project-data-row">
                <div>
                  <h3>{item.title}</h3>
                  <p>{item.description || '—'}</p>
                </div>
                <span>{item.priority}</span>
                <span>{item.status}</span>
                {canManageBacklog && (
                  <button
                    type="button"
                    className="btn btn--ghost project-row-actions__text"
                    onClick={() => {
                      setActionError('')
                      setTaskItem(item)
                    }}
                  >
                    Thêm task
                  </button>
                )}
              </article>
            ))}
          </div>
        )}

        {showItemModal && (
          <BacklogItemModal
            onClose={() => {
              if (!createItemMutation.isPending) {
                setShowItemModal(false)
                setActionError('')
              }
            }}
            onSave={(values) => createItemMutation.mutate(values)}
            isSaving={createItemMutation.isPending}
            error={actionError}
          />
        )}

        {taskItem && (
          <TaskModal
            members={members}
            onClose={() => {
              if (!createTaskMutation.isPending) {
                setTaskItem(null)
                setActionError('')
              }
            }}
            onSave={(values) =>
              createTaskMutation.mutate({ item: taskItem, values })
            }
            isSaving={createTaskMutation.isPending}
            error={actionError}
          />
        )}
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectSprintPage() {
  return (
    <PermissionRoute permission={PERMISSIONS.SPRINT_READ}>
      <ProjectTabShell title="Sprint">
        <EmptyState>Sprint board sẽ hiển thị khi có dữ liệu sprint từ API.</EmptyState>
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectMembersPage() {
  const { project } = useProject()
  const { can } = usePermission()
  const queryClient = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const [actionError, setActionError] = useState('')

  const canManageMembers =
    can(PERMISSIONS.PROJECT_MEMBER_MANAGE) && project?.isCurrentUserTeamLeader

  const { data: members = [] } = useQuery({
    queryKey: ['projects', project?.id, 'members'],
    queryFn: () => fetchProjectMembers(project),
    enabled: Boolean(project),
  })
  const { data: teamMembers = [] } = useQuery({
    queryKey: ['teams', project?.workspaceId, project?.teamId, 'members'],
    queryFn: () => fetchTeamMembers(project.workspaceId, project.teamId),
    enabled: Boolean(project) && canManageMembers,
  })
  const { data: roles = [] } = useQuery({
    queryKey: ['admin', 'roles', 'project'],
    queryFn: async () => {
      const allRoles = await fetchRoles()
      return allRoles.filter((role) => role.scope === 'PROJECT')
    },
    enabled: canManageMembers,
  })

  const invalidateMembers = () => {
    queryClient.invalidateQueries({ queryKey: ['projects'] })
  }

  const addMutation = useMutation({
    mutationFn: (values) => addProjectMember(project, values),
    onSuccess: () => {
      setShowAdd(false)
      setActionError('')
      invalidateMembers()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể thêm thành viên.'))
    },
  })

  const setManagerMutation = useMutation({
    mutationFn: (member) =>
      updateProject(project, {
        ...project,
        projectManagerMemberId: member.teamMemberId,
      }),
    onSuccess: () => {
      setActionError('')
      invalidateMembers()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể gán Project Manager.'))
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (member) =>
      updateProjectMember(project, member.id, {
        roleId: member.roleId,
        status: 'INACTIVE',
      }),
    onSuccess: () => {
      setActionError('')
      invalidateMembers()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật thành viên.'))
    },
  })

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <ProjectTabShell
        title="Thành viên dự án"
        actions={
          canManageMembers ? (
            <Button type="button" variant="primary" onClick={() => setShowAdd(true)}>
              <Plus size={16} aria-hidden="true" />
              Thêm thành viên
            </Button>
          ) : null
        }
      >
        {actionError && (
          <p className="modal__error" role="alert">
            {actionError}
          </p>
        )}
        {members.length === 0 ? (
          <EmptyState>Project chưa có thành viên.</EmptyState>
        ) : (
          <div className="project-data-list">
            {members.map((member) => (
              <article key={member.id} className="project-data-row">
                <div>
                  <h3>{member.fullName}</h3>
                  <p>{member.email}</p>
                </div>
                <span>{member.roleName}</span>
                <span>{member.status}</span>
                {canManageMembers && (
                  <div className="project-row-actions">
                    <button
                      type="button"
                      className="icon-btn"
                      onClick={() => setManagerMutation.mutate(member)}
                      aria-label="Gán Project Manager"
                      disabled={setManagerMutation.isPending}
                    >
                      <UserCheck size={16} aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className="btn btn--ghost project-row-actions__text"
                      onClick={() => deactivateMutation.mutate(member)}
                      disabled={deactivateMutation.isPending}
                    >
                      Gỡ
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}

        {showAdd && (
          <AddProjectMemberModal
            project={project}
            teamMembers={teamMembers}
            roles={roles}
            existingMembers={members}
            onClose={() => {
              if (!addMutation.isPending) {
                setShowAdd(false)
                setActionError('')
              }
            }}
            onSave={(values) => addMutation.mutate(values)}
            isSaving={addMutation.isPending}
            error={actionError}
          />
        )}
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectDocsPage() {
  const { project } = useProject()
  const { can } = usePermission()
  const queryClient = useQueryClient()
  const [error, setError] = useState('')

  const canUpload =
    can(PERMISSIONS.PROJECT_DOC_READ) &&
    (project?.isCurrentUserTeamLeader || project?.isCurrentUserProjectManager)

  const { data: attachments = [] } = useQuery({
    queryKey: ['projects', project?.id, 'attachments'],
    queryFn: () => fetchProjectAttachments(project),
    enabled: Boolean(project),
  })

  const uploadMutation = useMutation({
    mutationFn: (file) => uploadProjectAttachment(project, file),
    onSuccess: () => {
      setError('')
      queryClient.invalidateQueries({
        queryKey: ['projects', project?.id, 'attachments'],
      })
    },
    onError: (uploadError) => {
      setError(getErrorMessage(uploadError, 'Không thể upload tài liệu.'))
    },
  })

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_DOC_READ}>
      <ProjectTabShell
        title="Tài liệu"
        actions={
          canUpload ? (
            <label className="btn btn--primary project-upload-btn">
              <FileUp size={16} aria-hidden="true" />
              Upload
              <input
                type="file"
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  if (file) uploadMutation.mutate(file)
                  event.target.value = ''
                }}
              />
            </label>
          ) : null
        }
      >
        {error && (
          <p className="modal__error" role="alert">
            {error}
          </p>
        )}
        {attachments.length === 0 ? (
          <EmptyState>Project chưa có tài liệu.</EmptyState>
        ) : (
          <div className="project-data-list">
            {attachments.map((file) => (
              <article key={file.id} className="project-data-row">
                <div>
                  <h3>{file.fileName}</h3>
                  <p>
                    {file.uploadedByName} · {formatDate(file.uploadedAt)}
                  </p>
                </div>
                <span>{file.fileType || 'file'}</span>
                <span>{formatBytes(file.fileSize)}</span>
              </article>
            ))}
          </div>
        )}
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectActivityPage() {
  const { project } = useProject()
  const { data: logs = [] } = useQuery({
    queryKey: ['projects', project?.id, 'activity'],
    queryFn: () => fetchProjectActivityLogs(project),
    enabled: Boolean(project),
  })

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_ACTIVITY_READ}>
      <ProjectTabShell title="Activity Log">
        {logs.length === 0 ? (
          <EmptyState>Chưa có lịch sử hoạt động.</EmptyState>
        ) : (
          <div className="project-data-list">
            {logs.map((log) => (
              <article key={log.id} className="project-data-row">
                <div>
                  <h3>{log.action}</h3>
                  <p>
                    {log.actorName} · {formatDate(log.createdAt)}
                  </p>
                </div>
                <span>{log.targetType}</span>
              </article>
            ))}
          </div>
        )}
      </ProjectTabShell>
    </PermissionRoute>
  )
}
