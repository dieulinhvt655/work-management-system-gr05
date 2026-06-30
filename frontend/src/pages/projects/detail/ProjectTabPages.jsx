import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  CalendarDays,
  CheckCircle2,
  Edit3,
  FileText,
  FileUp,
  Filter,
  FolderKanban,
  ListChecks,
  ListTodo,
  Plus,
  Search,
  Target,
  Trash2,
  TrendingUp,
  UserCheck,
} from 'lucide-react'
import {
  addProjectMember,
  createBacklogItem,
  createBacklogItemTask,
  createProjectSprint,
  deleteBacklogItem,
  deleteBacklogItemTask,
  fetchBacklogItemTasks,
  fetchProjectActivityLogs,
  fetchProjectAttachments,
  fetchProjectBacklog,
  fetchProjectBacklogItems,
  fetchProjectDashboard,
  fetchProjectMembers,
  fetchProjectSprints,
  updateBacklogItem,
  updateBacklogItemTask,
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

const PBI_TYPES = ['FEATURE', 'BUG', 'IMPROVEMENT', 'TASK', 'OTHER']
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const PBI_STATUSES = ['NEW', 'READY', 'IN_SPRINT', 'ON_HOLD', 'DONE', 'COMPLETED']
const TASK_STATUSES = ['TO_DO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'REOPENED', 'CANCELLED']

function BacklogItemModal({ item = null, onClose, onSave, isSaving, error }) {
  const [values, setValues] = useState({
    title: item?.title ?? '',
    description: item?.description ?? '',
    type: item?.type ?? 'FEATURE',
    priority: item?.priority ?? 'MEDIUM',
    status: item?.status ?? 'NEW',
    desiredDueDate: item?.desiredDueDate ?? '',
  })

  const set = (key, value) =>
    setValues((current) => ({ ...current, [key]: value }))

  return (
    <Modal
      title={item ? 'Cập nhật Product Backlog Item' : 'Tạo Product Backlog Item'}
      description={item ? item.title : 'Ghi nhận yêu cầu, lỗi, cải tiến hoặc hạng mục công việc mới.'}
      onClose={onClose}
      size="md"
    >
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
            {PBI_TYPES.map((type) => (
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
            {PRIORITIES.map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="project-form__grid">
          <SelectField
            id="backlog-status"
            label="Trạng thái"
            value={values.status}
            onChange={(event) => set('status', event.target.value)}
          >
            {PBI_STATUSES.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </SelectField>
          <TextField
            id="backlog-due"
            type="date"
            label="Ngày mong muốn"
            value={values.desiredDueDate}
            onChange={(event) => set('desiredDueDate', event.target.value)}
          />
        </div>
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
            {isSaving ? 'Đang lưu...' : item ? 'Cập nhật PBI' : 'Tạo PBI'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function TaskModal({ task = null, members, onClose, onSave, isSaving, error }) {
  const [values, setValues] = useState({
    title: task?.title ?? '',
    description: task?.description ?? '',
    priority: task?.priority ?? 'MEDIUM',
    status: task?.status ?? 'TO_DO',
    assigneeMemberId: task?.assigneeMemberId ?? '',
    deadline: task?.deadline ?? '',
  })

  const set = (key, value) =>
    setValues((current) => ({ ...current, [key]: value }))

  return (
    <Modal
      title={task ? 'Cập nhật task' : 'Thêm task'}
      description={task ? task.title : 'Phân rã PBI thành công việc chuẩn bị cho Sprint.'}
      onClose={onClose}
      size="md"
    >
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
            {PRIORITIES.map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </SelectField>
          <SelectField
            id="task-status"
            label="Trạng thái"
            value={values.status}
            onChange={(event) => set('status', event.target.value)}
          >
            {TASK_STATUSES.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="project-form__grid">
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
          <TextField
            id="task-deadline"
            type="date"
            label="Deadline"
            value={values.deadline}
            onChange={(event) => set('deadline', event.target.value)}
          />
        </div>
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
            {isSaving ? 'Đang lưu...' : task ? 'Cập nhật task' : 'Thêm task'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function SprintModal({ onClose, onSave, isSaving, error }) {
  const [values, setValues] = useState({
    name: '',
    goal: '',
    startDate: '',
    endDate: '',
  })
  const [validationError, setValidationError] = useState('')

  const set = (key, value) => {
    setValidationError('')
    setValues((current) => ({ ...current, [key]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    if (!values.name.trim()) {
      setValidationError('Vui lòng nhập tên Sprint.')
      return
    }

    if (!values.startDate || !values.endDate) {
      setValidationError('Vui lòng chọn thời gian bắt đầu và kết thúc Sprint.')
      return
    }

    if (new Date(values.endDate) < new Date(values.startDate)) {
      setValidationError('Ngày kết thúc phải sau ngày bắt đầu.')
      return
    }

    onSave(values)
  }

  return (
    <Modal
      title="Tạo mới Sprint"
      description="Thiết lập chu kỳ làm việc, mục tiêu và thời gian triển khai."
      onClose={onClose}
      size="md"
    >
      <form className="project-form" onSubmit={handleSubmit} noValidate>
        {(validationError || error) && (
          <p className="modal__error" role="alert">
            {validationError || error}
          </p>
        )}
        <TextField
          id="sprint-name"
          label="Tên Sprint"
          value={values.name}
          onChange={(event) => set('name', event.target.value)}
        />
        <div className="project-form__grid">
          <TextField
            id="sprint-start"
            type="date"
            label="Ngày bắt đầu"
            value={values.startDate}
            onChange={(event) => set('startDate', event.target.value)}
          />
          <TextField
            id="sprint-end"
            type="date"
            label="Ngày kết thúc"
            value={values.endDate}
            onChange={(event) => set('endDate', event.target.value)}
          />
        </div>
        <div className="field">
          <label className="field__label" htmlFor="sprint-goal">
            Mục tiêu Sprint
          </label>
          <textarea
            id="sprint-goal"
            className="field__input project-form__textarea"
            rows={3}
            value={values.goal}
            onChange={(event) => set('goal', event.target.value)}
          />
        </div>
        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={isSaving || !values.name.trim()}
          >
            {isSaving ? 'Đang tạo...' : 'Tạo Sprint'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function statusText(value) {
  return String(value ?? '—').replaceAll('_', ' ')
}

function BacklogBadge({ value, tone = 'neutral' }) {
  return (
    <span className={`backlog-badge backlog-badge--${tone}`}>
      {statusText(value)}
    </span>
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
  const { data: backlogGroups = [] } = useQuery({
    queryKey: ['projects', project?.id, 'overview-task-board'],
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

  const objectiveItems = splitDetailText(project?.objective)
  const scopeItems = splitDetailText(project?.scope)
  const projectManager = project?.members?.find(
    (member) => String(member.id) === String(project?.managerMemberId),
  )
  const allTasks = useMemo(
    () =>
      backlogGroups.flatMap((group) =>
        group.tasks.map((task) => ({
          ...task,
          pbiTitle: task.pbiTitle || group.item.title,
          pbiType: group.item.type,
        })),
      ),
    [backlogGroups],
  )
  const tasksByStatus = useMemo(() => {
    const groups = {
      TO_DO: [],
      IN_PROGRESS: [],
      REVIEW: [],
      DONE: [],
    }

    for (const task of allTasks) {
      if (task.status === 'DONE') {
        groups.DONE.push(task)
      } else if (task.status === 'REVIEW') {
        groups.REVIEW.push(task)
      } else if (task.status === 'IN_PROGRESS') {
        groups.IN_PROGRESS.push(task)
      } else {
        groups.TO_DO.push(task)
      }
    }

    return groups
  }, [allTasks])
  const taskBreakdown = dashboard?.taskBreakdown ?? {}
  const totalTasks = dashboard?.totalTasks ?? allTasks.length
  const completedTasks = taskBreakdown.done ?? tasksByStatus.DONE.length
  const inProgressTasks = taskBreakdown.inProgress ?? tasksByStatus.IN_PROGRESS.length
  const todoTasks = taskBreakdown.todo ?? tasksByStatus.TO_DO.length
  const weeklyPoints = [12, 19, 15, 25, 22, 18, 24]

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_READ}>
      <ProjectTabShell
        title="Dashboard"
        description="Welcome back! Đây là tình hình vận hành hiện tại của dự án."
        actions={
          <Link to={`/projects/${project?.id}/backlog`} className="btn btn--primary">
            <Plus size={16} aria-hidden="true" />
            Create Task
          </Link>
        }
      >
        <div className="project-dashboard-search">
          <Search size={16} aria-hidden="true" />
          <input type="search" placeholder="Search tasks, projects, or people..." />
        </div>

        <div className="project-dashboard-kpis">
          <article>
            <span className="project-dashboard-kpi__icon project-dashboard-kpi__icon--blue">
              <ListTodo size={18} aria-hidden="true" />
            </span>
            <strong>{totalTasks}</strong>
            <p>Total Tasks</p>
            <small>+12%</small>
          </article>
          <article>
            <span className="project-dashboard-kpi__icon project-dashboard-kpi__icon--green">
              <CheckCircle2 size={18} aria-hidden="true" />
            </span>
            <strong>{completedTasks}</strong>
            <p>Completed</p>
            <small>+8%</small>
          </article>
          <article>
            <span className="project-dashboard-kpi__icon project-dashboard-kpi__icon--orange">
              <TrendingUp size={18} aria-hidden="true" />
            </span>
            <strong>{inProgressTasks}</strong>
            <p>In Progress</p>
            <small>{tasksByStatus.REVIEW.length}</small>
          </article>
          <article>
            <span className="project-dashboard-kpi__icon project-dashboard-kpi__icon--purple">
              <FolderKanban size={18} aria-hidden="true" />
            </span>
            <strong>{todoTasks}</strong>
            <p>To-do</p>
            <small>+2</small>
          </article>
        </div>

        <div className="project-dashboard-charts">
          <section>
            <header>
              <h3>Task Completion Rate</h3>
              <select aria-label="Khoảng thời gian">
                <option>Last 7 days</option>
                <option>Last 30 days</option>
              </select>
            </header>
            <div className="project-dashboard-rate">
              <strong>{dashboard?.completionPercent ?? 0}%</strong>
              <span style={{ width: `${dashboard?.completionPercent ?? 0}%` }} />
            </div>
            <p>{project?.name}</p>
          </section>

          <section>
            <header>
              <h3>Task Distribution</h3>
              <button type="button" className="icon-btn" aria-label="Tùy chọn">
                ...
              </button>
            </header>
            <div className="project-dashboard-distribution">
              {[
                ['To Do', todoTasks],
                ['In Progress', inProgressTasks],
                ['Review', tasksByStatus.REVIEW.length],
                ['Done', completedTasks],
              ].map(([label, value]) => (
                <div key={label}>
                  <span>{label}</span>
                  <strong>{value}</strong>
                </div>
              ))}
            </div>
          </section>
        </div>

        <section className="project-task-board">
          <header>
            <h3>Active Tasks Board</h3>
            <div>
              <button type="button" className="btn btn--ghost">
                <Filter size={16} aria-hidden="true" />
                Filter
              </button>
              <button type="button" className="btn btn--ghost">
                Sort
              </button>
            </div>
          </header>

          <div className="project-task-board__columns">
            {[
              ['TO_DO', 'To Do', 'project-task-board__dot--gray'],
              ['IN_PROGRESS', 'In Progress', 'project-task-board__dot--blue'],
              ['REVIEW', 'Review', 'project-task-board__dot--orange'],
              ['DONE', 'Done', 'project-task-board__dot--green'],
            ].map(([status, label, dotClass]) => (
              <section key={status} className="project-task-column">
                <header>
                  <h4>
                    <span className={`project-task-board__dot ${dotClass}`} />
                    {label}
                    <small>{tasksByStatus[status].length}</small>
                  </h4>
                  <Link to={`/projects/${project?.id}/backlog`}>+</Link>
                </header>
                <div className="project-task-column__cards">
                  {tasksByStatus[status].slice(0, 4).map((task) => (
                    <article key={task.id} className="project-task-card">
                      <h5>{task.title}</h5>
                      <p>{task.description || task.pbiTitle}</p>
                      <div>
                        <BacklogBadge value={task.pbiType || task.priority} tone="info" />
                        <span>{task.assigneeName || 'Chưa gán'}</span>
                      </div>
                    </article>
                  ))}
                  {tasksByStatus[status].length === 0 && (
                    <p className="project-task-column__empty">Chưa có task.</p>
                  )}
                </div>
              </section>
            ))}
          </div>
        </section>

        <div className="project-dashboard-bottom">
          <section className="project-detail-card">
            <h3>Recent Activity</h3>
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

          <section className="project-detail-card">
            <h3>Weekly Progress</h3>
            <div className="project-weekly-chart">
              {weeklyPoints.map((point, index) => (
                <span
                  key={`${point}-${index}`}
                  style={{ height: `${point * 4}px` }}
                  title={`${point} tasks`}
                />
              ))}
            </div>
            <div className="project-weekly-labels">
              {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day) => (
                <span key={day}>{day}</span>
              ))}
            </div>
          </section>
        </div>

        <div className="project-detail-grid project-dashboard-info">
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
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [itemModal, setItemModal] = useState(null)
  const [taskModal, setTaskModal] = useState(null)
  const [selectedItemId, setSelectedItemId] = useState('')
  const [actionError, setActionError] = useState('')
  const canManageBacklog =
    project?.isCurrentUserProjectManager ||
    (can(PERMISSIONS.BACKLOG_MANAGE) && project?.isCurrentUserTeamLeader)
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
  const selectedItem = useMemo(() => {
    if (selectedItemId) {
      const byId = items.find((item) => item.id === selectedItemId)
      if (byId) return byId
    }
    return items[0] ?? null
  }, [items, selectedItemId])
  const { data: selectedTasks = [], isLoading: tasksLoading } = useQuery({
    queryKey: ['projects', project?.id, 'backlog-items', selectedItem?.id, 'tasks'],
    queryFn: () => fetchBacklogItemTasks(project, selectedItem.id),
    enabled: Boolean(project && selectedItem),
  })
  const { data: members = [] } = useQuery({
    queryKey: ['projects', project?.id, 'members'],
    queryFn: () => fetchProjectMembers(project),
    enabled: Boolean(project) && canManageBacklog,
  })

  const invalidateBacklog = () => {
    queryClient.invalidateQueries({ queryKey: ['projects', project?.id] })
    queryClient.invalidateQueries({ queryKey: ['projects', project?.id, 'backlog-items'] })
    queryClient.invalidateQueries({ queryKey: ['projects', project?.id, 'dashboard'] })
  }

  const filteredItems = useMemo(() => {
    const normalized = keyword.trim().toLowerCase()
    return items.filter((item) => {
      const matchesKeyword =
        !normalized ||
        [item.title, item.description, item.id]
          .filter(Boolean)
          .some((value) => value.toLowerCase().includes(normalized))
      const matchesStatus = !statusFilter || item.status === statusFilter
      const matchesPriority = !priorityFilter || item.priority === priorityFilter
      const matchesType = !typeFilter || item.type === typeFilter
      return matchesKeyword && matchesStatus && matchesPriority && matchesType
    })
  }, [items, keyword, priorityFilter, statusFilter, typeFilter])

  const createItemMutation = useMutation({
    mutationFn: (values) =>
      createBacklogItem(project, {
        ...values,
        proposerMemberId: project?.currentMember?.id,
      }),
    onSuccess: () => {
      setItemModal(null)
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể thêm backlog item.'))
    },
  })

  const updateItemMutation = useMutation({
    mutationFn: ({ item, values }) => updateBacklogItem(project, item.id, values),
    onSuccess: () => {
      setItemModal(null)
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật backlog item.'))
    },
  })

  const markReadyMutation = useMutation({
    mutationFn: (item) =>
      updateBacklogItem(project, item.id, {
        ...item,
        status: 'READY',
      }),
    onSuccess: () => {
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể xác nhận PBI Ready.'))
    },
  })

  const deleteItemMutation = useMutation({
    mutationFn: (item) => deleteBacklogItem(project, item.id),
    onSuccess: (_, item) => {
      setActionError('')
      if (selectedItem?.id === item.id) {
        setSelectedItemId('')
      }
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể xóa backlog item.'))
    },
  })

  const createTaskMutation = useMutation({
    mutationFn: ({ item, values }) =>
      createBacklogItemTask(project, item.id, values),
    onSuccess: () => {
      setTaskModal(null)
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể thêm task.'))
    },
  })

  const updateTaskMutation = useMutation({
    mutationFn: ({ item, task, values }) =>
      updateBacklogItemTask(project, item.id, task.id, values),
    onSuccess: () => {
      setTaskModal(null)
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể cập nhật task.'))
    },
  })

  const deleteTaskMutation = useMutation({
    mutationFn: ({ item, task }) => deleteBacklogItemTask(project, item.id, task.id),
    onSuccess: () => {
      setActionError('')
      invalidateBacklog()
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể xóa task.'))
    },
  })

  const saveItem = (values) => {
    if (itemModal?.item) {
      updateItemMutation.mutate({ item: itemModal.item, values })
      return
    }
    createItemMutation.mutate(values)
  }

  const saveTask = (values) => {
    if (!taskModal?.item) return
    if (taskModal.task) {
      updateTaskMutation.mutate({
        item: taskModal.item,
        task: taskModal.task,
        values,
      })
      return
    }
    createTaskMutation.mutate({ item: taskModal.item, values })
  }

  const itemSaving = createItemMutation.isPending || updateItemMutation.isPending
  const taskSaving = createTaskMutation.isPending || updateTaskMutation.isPending

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
                setItemModal({ item: null })
              }}
            >
              <Plus size={16} aria-hidden="true" />
              Tạo PBI
            </Button>
          ) : null
        }
      >
        {actionError && (
          <p className="modal__error" role="alert">
            {actionError}
          </p>
        )}

        <div className="backlog-workspace">
          <section className="backlog-main">
            <div className="backlog-toolbar">
              <label className="backlog-search" htmlFor="backlog-search">
                <Search size={16} aria-hidden="true" />
                <input
                  id="backlog-search"
                  type="search"
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Tìm PBI..."
                />
              </label>
              <SelectField
                id="backlog-status-filter"
                label="Trạng thái"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
              >
                <option value="">Tất cả</option>
                {PBI_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {statusText(status)}
                  </option>
                ))}
              </SelectField>
              <SelectField
                id="backlog-priority-filter"
                label="Độ ưu tiên"
                value={priorityFilter}
                onChange={(event) => setPriorityFilter(event.target.value)}
              >
                <option value="">Tất cả</option>
                {PRIORITIES.map((priority) => (
                  <option key={priority} value={priority}>
                    {priority}
                  </option>
                ))}
              </SelectField>
              <SelectField
                id="backlog-type-filter"
                label="Loại"
                value={typeFilter}
                onChange={(event) => setTypeFilter(event.target.value)}
              >
                <option value="">Tất cả</option>
                {PBI_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </SelectField>
              <Filter size={18} aria-hidden="true" className="backlog-toolbar__icon" />
            </div>

            {filteredItems.length === 0 ? (
              <EmptyState>Không có PBI phù hợp.</EmptyState>
            ) : (
              <div className="backlog-table-wrap">
                <table className="backlog-table">
                  <thead>
                    <tr>
                      <th>Mã PBI</th>
                      <th>Tiêu đề</th>
                      <th>Loại</th>
                      <th>Ưu tiên</th>
                      <th>SP</th>
                      <th>Trạng thái</th>
                      <th>Tasks</th>
                      <th>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredItems.map((item) => (
                      <tr
                        key={item.id}
                        className={selectedItem?.id === item.id ? 'backlog-table__row--active' : ''}
                        onClick={() => setSelectedItemId(item.id)}
                      >
                        <td>
                          <button type="button" className="backlog-code">
                            PBI-{item.id}
                          </button>
                        </td>
                        <td>
                          <strong>{item.title}</strong>
                          <small>{item.description || '—'}</small>
                        </td>
                        <td>
                          <BacklogBadge
                            value={item.type}
                            tone={item.type === 'BUG' ? 'danger' : 'purple'}
                          />
                        </td>
                        <td>
                          <BacklogBadge
                            value={item.priority}
                            tone={item.priority === 'HIGH' || item.priority === 'URGENT' ? 'danger' : 'warning'}
                          />
                        </td>
                        <td>{item.storyPoints ?? '—'}</td>
                        <td>
                          <BacklogBadge value={item.status} tone="info" />
                        </td>
                        <td>
                          <span className="backlog-task-count">
                            <ListTodo size={14} aria-hidden="true" />
                            {selectedItem?.id === item.id ? selectedTasks.length : '—'}
                          </span>
                        </td>
                        <td>
                          {canManageBacklog && (
                            <div className="project-row-actions" onClick={(event) => event.stopPropagation()}>
                              <button
                                type="button"
                                className="icon-btn"
                                onClick={() => {
                                  setActionError('')
                                  setItemModal({ item })
                                }}
                                aria-label="Sửa PBI"
                              >
                                <Edit3 size={16} aria-hidden="true" />
                              </button>
                              <button
                                type="button"
                                className="icon-btn"
                                onClick={() => {
                                  if (window.confirm('Xóa Product Backlog Item này?')) {
                                    deleteItemMutation.mutate(item)
                                  }
                                }}
                                aria-label="Xóa PBI"
                                disabled={deleteItemMutation.isPending}
                              >
                                <Trash2 size={16} aria-hidden="true" />
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <aside className="backlog-detail-panel">
            {selectedItem ? (
              <>
                <div className="backlog-detail-panel__top">
                  <BacklogBadge value={`PBI-${selectedItem.id}`} tone="info" />
                  {canManageBacklog && (
                    <div className="project-row-actions">
                      <button
                        type="button"
                        className="icon-btn"
                        onClick={() => setItemModal({ item: selectedItem })}
                        aria-label="Sửa PBI đang chọn"
                      >
                        <Edit3 size={16} aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        className="icon-btn"
                        onClick={() => {
                          if (window.confirm('Xóa Product Backlog Item này?')) {
                            deleteItemMutation.mutate(selectedItem)
                          }
                        }}
                        aria-label="Xóa PBI đang chọn"
                        disabled={deleteItemMutation.isPending}
                      >
                        <Trash2 size={16} aria-hidden="true" />
                      </button>
                    </div>
                  )}
                </div>

                <h3 className="backlog-detail-panel__title">{selectedItem.title}</h3>
                <div className="backlog-detail-meta">
                  <span>Trạng thái</span>
                  <BacklogBadge value={selectedItem.status} tone="info" />
                  <span>Loại</span>
                  <BacklogBadge value={selectedItem.type} tone="purple" />
                  <span>Độ ưu tiên</span>
                  <BacklogBadge value={selectedItem.priority} tone="warning" />
                  <span>Ngày mong muốn</span>
                  <strong>{formatDate(selectedItem.desiredDueDate)}</strong>
                </div>

                <section className="backlog-detail-section">
                  <h4>Mô tả chi tiết</h4>
                  <p>{selectedItem.description || 'Chưa có mô tả.'}</p>
                </section>

                {canManageBacklog && selectedItem.status !== 'READY' && (
                  <button
                    type="button"
                    className="btn btn--primary backlog-ready-btn"
                    onClick={() => markReadyMutation.mutate(selectedItem)}
                    disabled={markReadyMutation.isPending || selectedTasks.length === 0}
                  >
                    <CheckCircle2 size={16} aria-hidden="true" />
                    {markReadyMutation.isPending
                      ? 'Đang xác nhận...'
                      : 'Xác nhận PBI Ready'}
                  </button>
                )}

                <section className="backlog-detail-section">
                  <div className="backlog-detail-section__header">
                    <h4>Phân rã task</h4>
                    <span>{selectedTasks.length} task</span>
                  </div>
                  {tasksLoading ? (
                    <p className="project-tab-empty">Đang tải task...</p>
                  ) : selectedTasks.length === 0 ? (
                    <p className="project-tab-empty">PBI chưa có task.</p>
                  ) : (
                    <div className="backlog-task-list">
                      {selectedTasks.map((task) => (
                        <article key={task.id} className="backlog-task-card">
                          <div>
                            <span>{task.id}</span>
                            <strong>{task.title}</strong>
                            <small>{task.assigneeName || 'Chưa gán'} · {statusText(task.status)}</small>
                          </div>
                          {canManageBacklog && (
                            <div className="project-row-actions">
                              <select
                                className="backlog-task-assignee"
                                value={task.assigneeMemberId ?? ''}
                                onChange={(event) =>
                                  updateTaskMutation.mutate({
                                    item: selectedItem,
                                    task,
                                    values: {
                                      ...task,
                                      assigneeMemberId: event.target.value,
                                    },
                                  })
                                }
                                aria-label="Phân assignee dự kiến"
                                disabled={updateTaskMutation.isPending}
                              >
                                <option value="">Chưa gán</option>
                                {members.map((member) => (
                                  <option key={member.id} value={member.id}>
                                    {member.fullName}
                                  </option>
                                ))}
                              </select>
                              <button
                                type="button"
                                className="icon-btn"
                                onClick={() => setTaskModal({ item: selectedItem, task })}
                                aria-label="Sửa task"
                              >
                                <Edit3 size={16} aria-hidden="true" />
                              </button>
                              <button
                                type="button"
                                className="icon-btn"
                                onClick={() => {
                                  if (window.confirm('Xóa task này?')) {
                                    deleteTaskMutation.mutate({ item: selectedItem, task })
                                  }
                                }}
                                aria-label="Xóa task"
                                disabled={deleteTaskMutation.isPending}
                              >
                                <Trash2 size={16} aria-hidden="true" />
                              </button>
                            </div>
                          )}
                        </article>
                      ))}
                    </div>
                  )}
                  {canManageBacklog && (
                    <button
                      type="button"
                      className="btn btn--ghost backlog-add-task"
                      onClick={() => {
                        setActionError('')
                        setTaskModal({ item: selectedItem, task: null })
                      }}
                    >
                      <Plus size={16} aria-hidden="true" />
                      Thêm task
                    </button>
                  )}
                </section>
              </>
            ) : (
              <EmptyState>Chọn một PBI để xem chi tiết và phân rã task.</EmptyState>
            )}
          </aside>
        </div>

        {itemModal && (
          <BacklogItemModal
            item={itemModal.item}
            onClose={() => {
              if (!itemSaving) {
                setItemModal(null)
                setActionError('')
              }
            }}
            onSave={saveItem}
            isSaving={itemSaving}
            error={actionError}
          />
        )}

        {taskModal && (
          <TaskModal
            task={taskModal.task}
            members={members}
            onClose={() => {
              if (!taskSaving) {
                setTaskModal(null)
                setActionError('')
              }
            }}
            onSave={saveTask}
            isSaving={taskSaving}
            error={actionError}
          />
        )}
      </ProjectTabShell>
    </PermissionRoute>
  )
}

export function ProjectSprintPage() {
  const { project } = useProject()
  const { can } = usePermission()
  const queryClient = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [actionError, setActionError] = useState('')
  const canManageSprint =
    project?.isCurrentUserProjectManager ||
    (can(PERMISSIONS.SPRINT_MANAGE) && project?.isCurrentUserTeamLeader)

  const { data: sprints = [], isLoading } = useQuery({
    queryKey: ['projects', project?.id, 'sprints'],
    queryFn: () => fetchProjectSprints(project),
    enabled: Boolean(project),
  })

  const createSprintMutation = useMutation({
    mutationFn: (values) => createProjectSprint(project, values),
    onSuccess: () => {
      setShowCreate(false)
      setActionError('')
      queryClient.invalidateQueries({ queryKey: ['projects', project?.id, 'sprints'] })
      queryClient.invalidateQueries({ queryKey: ['projects', project?.id, 'dashboard'] })
    },
    onError: (error) => {
      setActionError(getErrorMessage(error, 'Không thể tạo Sprint.'))
    },
  })

  return (
    <PermissionRoute permission={PERMISSIONS.SPRINT_READ}>
      <ProjectTabShell
        title="Sprint"
        description="Quản lý chu kỳ làm việc, mục tiêu và thời gian triển khai của dự án."
        actions={
          canManageSprint ? (
            <Button
              type="button"
              variant="primary"
              onClick={() => {
                setActionError('')
                setShowCreate(true)
              }}
            >
              <Plus size={16} aria-hidden="true" />
              Tạo Sprint
            </Button>
          ) : null
        }
      >
        {actionError && (
          <p className="modal__error" role="alert">
            {actionError}
          </p>
        )}

        {isLoading ? (
          <EmptyState>Đang tải danh sách Sprint...</EmptyState>
        ) : sprints.length === 0 ? (
          <EmptyState>Chưa có Sprint nào trong dự án.</EmptyState>
        ) : (
          <div className="sprint-workspace">
            <section className="sprint-list">
              {sprints.map((sprint) => (
                <article key={sprint.id} className="sprint-card">
                  <div>
                    <BacklogBadge value={sprint.status} tone="info" />
                    <h3>{sprint.name}</h3>
                    <p>{sprint.goal || 'Chưa có mục tiêu Sprint.'}</p>
                  </div>
                  <div className="sprint-card__meta">
                    <span>{formatDate(sprint.startDate)}</span>
                    <span>{formatDate(sprint.endDate)}</span>
                    <strong>{sprint.completionPercent}%</strong>
                  </div>
                </article>
              ))}
            </section>

            <section className="sprint-timeline">
              <div className="sprint-timeline__weeks">
                <span>Week 1</span>
                <span>Week 2</span>
                <span>Week 3</span>
                <span>Week 4</span>
              </div>
              {sprints.map((sprint, index) => (
                <div key={sprint.id} className="sprint-timeline__row">
                  <span>{sprint.name}</span>
                  <div>
                    <i style={{ width: `${Math.max(25, 95 - index * 12)}%` }}>
                      {sprint.goal || sprint.name}
                    </i>
                  </div>
                </div>
              ))}
            </section>
          </div>
        )}

        {showCreate && (
          <SprintModal
            onClose={() => {
              if (!createSprintMutation.isPending) {
                setShowCreate(false)
                setActionError('')
              }
            }}
            onSave={(values) => createSprintMutation.mutate(values)}
            isSaving={createSprintMutation.isPending}
            error={actionError}
          />
        )}
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
