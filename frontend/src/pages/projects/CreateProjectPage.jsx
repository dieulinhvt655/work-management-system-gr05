import { useMemo, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Info, LayoutPanelTop, ListTodo, Rocket, UserRound, Users, Workflow } from 'lucide-react'
import { createProject } from '../../api/projectsApi'
import { fetchTeams } from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import { usePermission } from '../../hooks/usePermission'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { getManagedTeamOptions } from '../../utils/teamLeaderScope'
import { isTeamLeaderUser } from '../../utils/userRoleUtils'

const initialValues = {
  teamId: '',
  name: '',
  description: '',
  objective: '',
  scope: '',
  startDate: '',
  endDate: '',
}

export default function CreateProjectPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const { can } = usePermission()
  const [values, setValues] = useState(initialValues)
  const [formError, setFormError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const managedTeams = useMemo(() => getManagedTeamOptions(user), [user])
  const isTeamLeader = isTeamLeaderUser(user)
  const canCreateProject = can(PERMISSIONS.PROJECT_CREATE) || isTeamLeader

  const {
    data: teams = [],
    isLoading: teamsLoading,
    isError: teamsIsError,
    error: teamsError,
  } = useQuery({
    queryKey: ['teams', user?.workspaceId],
    queryFn: () => fetchTeams(user?.workspaceId),
    enabled:
      isAuthenticated &&
      !authLoading &&
      !isTeamLeader &&
      managedTeams.length === 0,
  })

  const teamOptions = useMemo(() => {
    if (user?.isSystemAdmin) return teams
    if (managedTeams.length > 0) return managedTeams

    const ledTeams = teams.filter((team) => {
      return (team.members ?? []).some(
        (member) =>
          member.isLeader && String(member.userId) === String(user?.id),
      )
    })

    return ledTeams
  }, [managedTeams, teams, user])

  const selectedTeamId = values.teamId || teamOptions[0]?.id || ''

  const createMutation = useMutation({
    mutationFn: (payload) => {
      const team = teamOptions.find(
        (entry) => String(entry.id) === String(payload.teamId),
      )
      return createProject(
        team?.workspaceId ?? user?.workspaceId,
        payload.teamId,
        payload,
      )
    },
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      navigate(project?.id ? `/projects/${project.id}/members` : '/projects', {
        replace: true,
      })
    },
    onError: (error) => {
      setFormError(getErrorMessage(error, 'Không thể tạo dự án.'))
    },
  })

  const set = (key, value) => {
    setFormError('')
    setFieldErrors((current) => ({ ...current, [key]: '' }))
    setValues((current) => ({ ...current, [key]: value }))
  }

  const validate = () => {
    const nextErrors = {}
    const payload = { ...values, teamId: selectedTeamId }

    if (!payload.teamId) nextErrors.teamId = 'Vui lòng chọn Team.'
    if (!payload.name.trim()) nextErrors.name = 'Vui lòng nhập tên dự án.'
    if (!payload.objective.trim()) nextErrors.objective = 'Vui lòng nhập mục tiêu dự án.'
    if (!payload.scope.trim()) nextErrors.scope = 'Vui lòng nhập phạm vi dự án.'
    if (!payload.startDate) nextErrors.startDate = 'Vui lòng chọn ngày bắt đầu.'
    if (
      payload.endDate &&
      payload.startDate &&
      new Date(payload.endDate) < new Date(payload.startDate)
    ) {
      nextErrors.endDate = 'Ngày kết thúc phải sau ngày bắt đầu.'
    }

    setFieldErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    if (!validate()) return
    createMutation.mutate({ ...values, teamId: selectedTeamId })
  }

  if (authLoading) {
    return <LoadingScreen />
  }

  if (!canCreateProject) {
    return <Navigate to="/403" replace />
  }

  return (
    <div className="page page--wide create-project-page">
      <header className="page__header">
        <h1 className="page__title">Tạo dự án mới</h1>
        <p className="page__subtitle">
          Tạo dự án mới trong Team và thiết lập thông tin ban đầu để bắt đầu công việc.
        </p>
      </header>

      {!teamsLoading && teamOptions.length === 0 ? (
        <div className="project-list-empty">
          <p className="project-list-empty__title">Chưa có Team để tạo dự án</p>
          <p className="project-list-empty__text">
            {teamsIsError
              ? getErrorMessage(teamsError, 'Không tải được danh sách Team từ API.')
              : 'Tài khoản Team Leader cần được phân công quản lý ít nhất một Team trước khi tạo dự án.'}
          </p>
          <Link to="/projects" className="btn btn--ghost">
            Quay lại danh sách dự án
          </Link>
        </div>
      ) : (
        <form className="create-project-layout" onSubmit={handleSubmit} noValidate>
            <section className="create-project-card">
              {(formError || createMutation.isError) && (
                <p className="modal__error" role="alert">
                  {formError}
                </p>
              )}
              <div className="field">
                <label className="field__label" htmlFor="project-team">
                  Team quản lý <span className="field__required">*</span>
                </label>
                <select
                  id="project-team"
                  className="field__input"
                  value={selectedTeamId}
                  onChange={(event) => set('teamId', event.target.value)}
                  disabled={teamsLoading || teamOptions.length <= 1}
                >
                  {teamsLoading && <option value="">Đang tải Team từ API...</option>}
                  {teamOptions.map((team) => (
                    <option key={team.id} value={team.id}>
                      {team.name}
                    </option>
                  ))}
                </select>
                {fieldErrors.teamId && <p className="field__error">{fieldErrors.teamId}</p>}
              </div>

              <div className="field">
                <label className="field__label" htmlFor="project-name">
                  Tên dự án <span className="field__required">*</span>
                </label>
                <input
                  id="project-name"
                  className="field__input"
                  value={values.name}
                  onChange={(event) => set('name', event.target.value)}
                  placeholder="Nhập tên dự án duy nhất..."
                />
                <p className="field__hint">Hệ thống sẽ kiểm tra tính duy nhất khi bạn lưu.</p>
                {fieldErrors.name && <p className="field__error">{fieldErrors.name}</p>}
              </div>

              <div className="field">
                <label className="field__label" htmlFor="project-description">
                  Mô tả dự án
                </label>
                <textarea
                  id="project-description"
                  className="field__input project-form__textarea"
                  rows={4}
                  value={values.description}
                  onChange={(event) => set('description', event.target.value)}
                  placeholder="Mô tả tóm tắt về mục đích dự án..."
                />
              </div>

              <div className="project-form__grid">
                <div className="field">
                  <label className="field__label" htmlFor="project-objective">
                    Mục tiêu dự án <span className="field__required">*</span>
                  </label>
                  <input
                    id="project-objective"
                    className="field__input"
                    value={values.objective}
                    onChange={(event) => set('objective', event.target.value)}
                    placeholder="KPIs, kết quả đầu ra..."
                  />
                  {fieldErrors.objective && <p className="field__error">{fieldErrors.objective}</p>}
                </div>

                <div className="field">
                  <label className="field__label" htmlFor="project-scope">
                    Phạm vi dự án <span className="field__required">*</span>
                  </label>
                  <input
                    id="project-scope"
                    className="field__input"
                    value={values.scope}
                    onChange={(event) => set('scope', event.target.value)}
                    placeholder="Giới hạn công việc..."
                  />
                  {fieldErrors.scope && <p className="field__error">{fieldErrors.scope}</p>}
                </div>
              </div>

              <div className="project-form__grid">
                <div className="field">
                  <label className="field__label" htmlFor="project-start-date">
                    Ngày bắt đầu <span className="field__required">*</span>
                  </label>
                  <input
                    id="project-start-date"
                    className="field__input"
                    type="date"
                    value={values.startDate}
                    onChange={(event) => set('startDate', event.target.value)}
                  />
                  {fieldErrors.startDate && <p className="field__error">{fieldErrors.startDate}</p>}
                </div>

                <div className="field">
                  <label className="field__label" htmlFor="project-end-date">
                    Ngày dự kiến kết thúc
                  </label>
                  <input
                    id="project-end-date"
                    className="field__input"
                    type="date"
                    value={values.endDate}
                    onChange={(event) => set('endDate', event.target.value)}
                  />
                  {fieldErrors.endDate && <p className="field__error">{fieldErrors.endDate}</p>}
                </div>
              </div>

              <div className="create-project-note">
                <Info size={18} aria-hidden="true" />
                <div>
                  <strong>Thiết lập tự động</strong>
                  <p>
                    Mã dự án, Workflow mặc định và Product Backlog sẽ được hệ thống tự động khởi tạo.
                  </p>
                </div>
              </div>
            </section>

            <aside className="create-project-side">
              <section className="create-project-preview">
                <div className="create-project-preview__hero">
                  <LayoutPanelTop size={34} aria-hidden="true" />
                </div>
                <div className="create-project-preview__body">
                  <div className="create-project-preview__title">
                    <h2>{values.name.trim() || 'Tên dự án mới'}</h2>
                    <span className="project-status project-status--draft">Draft</span>
                  </div>
                  <PreviewRow icon={UserRound} label="PM" value="Chưa gán" />
                  <PreviewRow icon={Users} label="Thành viên" value="0" />
                  <PreviewRow icon={Workflow} label="Workflow" value="Mặc định" />
                  <PreviewRow icon={ListTodo} label="Backlog" value="Tự khởi tạo" />
                  <div className="create-project-preview__code">
                    Project code
                    <strong>TF-XXXX (Auto)</strong>
                  </div>
                </div>
              </section>

              <section className="create-project-tip">
                <h3>Gợi ý</h3>
                <p>
                  Mục tiêu và phạm vi rõ ràng giúp các thành viên dễ nắm bắt kỳ vọng và tiến độ của dự án.
                </p>
              </section>
            </aside>

            <footer className="create-project-actions">
              <Link to="/projects" className="btn btn--ghost">
                Hủy
              </Link>
              <button
                type="submit"
                className="btn btn--primary"
                disabled={teamsLoading || createMutation.isPending}
              >
                <Rocket size={16} aria-hidden="true" />
                {teamsLoading
                  ? 'Đang tải Team...'
                  : createMutation.isPending
                    ? 'Đang tạo...'
                    : 'Tạo dự án'}
              </button>
            </footer>
        </form>
      )}
    </div>
  )
}

function PreviewRow({ icon: Icon, label, value }) {
  return (
    <div className="create-project-preview__row">
      <span>
        <Icon size={16} aria-hidden="true" />
        {label}
      </span>
      <strong>{value}</strong>
    </div>
  )
}
