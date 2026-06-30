import { useEffect, useMemo, useState } from 'react'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'
import SelectField from '../../../components/ui/SelectField'
import TextField from '../../../components/ui/TextField'

function toDateInput(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

export default function ProjectFormModal({
  title,
  description,
  project = null,
  teams = [],
  initialTeamId = '',
  onClose,
  onSave,
  isSaving = false,
  saveError = '',
}) {
  const [values, setValues] = useState({
    teamId: project?.teamId ?? initialTeamId,
    code: project?.code ?? '',
    name: project?.name ?? '',
    description: project?.description === '—' ? '' : project?.description ?? '',
    objective: project?.objective ?? '',
    scope: project?.scope ?? '',
    startDate: toDateInput(project?.startDate),
    endDate: toDateInput(project?.endDate),
    projectManagerMemberId: project?.managerMemberId ?? '',
  })
  const [validationError, setValidationError] = useState('')

  const selectedTeam = useMemo(
    () => teams.find((team) => String(team.id) === String(values.teamId)),
    [teams, values.teamId],
  )

  const managerCandidates = useMemo(
    () =>
      (selectedTeam?.members ?? []).filter(
        (member) => member.status === 'ACTIVE',
      ),
    [selectedTeam],
  )

  useEffect(() => {
    if (
      !values.projectManagerMemberId &&
      managerCandidates.length > 0 &&
      !project
    ) {
      setValues((current) => ({
        ...current,
        projectManagerMemberId: managerCandidates[0].id,
      }))
    }
  }, [managerCandidates, project, values.projectManagerMemberId])

  const set = (key, value) => {
    setValidationError('')
    setValues((current) => ({ ...current, [key]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    if (!values.teamId) {
      setValidationError('Vui lòng chọn team.')
      return
    }

    if (!values.code.trim() && !project) {
      setValidationError('Vui lòng nhập mã project.')
      return
    }

    if (!values.name.trim()) {
      setValidationError('Vui lòng nhập tên project.')
      return
    }

    if (!values.projectManagerMemberId) {
      setValidationError('Vui lòng chọn Project Manager.')
      return
    }

    onSave(values)
  }

  return (
    <Modal title={title} description={description} onClose={onClose} size="lg">
      <form className="project-form" onSubmit={handleSubmit} noValidate>
        {(validationError || saveError) && (
          <p className="modal__error" role="alert">
            {validationError || saveError}
          </p>
        )}

        <div className="project-form__grid">
          <SelectField
            id="project-team"
            label="Team"
            value={values.teamId}
            onChange={(event) => {
              set('teamId', event.target.value)
              setValues((current) => ({
                ...current,
                projectManagerMemberId: '',
              }))
            }}
            disabled={Boolean(project)}
          >
            <option value="">Chọn team</option>
            {teams.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </SelectField>

          <TextField
            id="project-code"
            label="Mã project"
            value={values.code}
            onChange={(event) => set('code', event.target.value)}
            disabled={Boolean(project)}
            placeholder="VD: PRJ-CRM"
          />

          <TextField
            id="project-name"
            label="Tên project"
            value={values.name}
            onChange={(event) => set('name', event.target.value)}
            placeholder="VD: CRM Automation"
          />

          <SelectField
            id="project-manager"
            label="Project Manager"
            value={values.projectManagerMemberId}
            onChange={(event) =>
              set('projectManagerMemberId', event.target.value)
            }
            disabled={!values.teamId || managerCandidates.length === 0}
          >
            <option value="">
              {values.teamId
                ? 'Chọn Project Manager'
                : 'Chọn team trước'}
            </option>
            {managerCandidates.map((member) => (
              <option key={member.id} value={member.id}>
                {member.fullName} · {member.roleName}
              </option>
            ))}
          </SelectField>

          <TextField
            id="project-start-date"
            type="date"
            label="Ngày bắt đầu"
            value={values.startDate}
            onChange={(event) => set('startDate', event.target.value)}
          />

          <TextField
            id="project-end-date"
            type="date"
            label="Ngày kết thúc"
            value={values.endDate}
            onChange={(event) => set('endDate', event.target.value)}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="project-description">
            Mô tả
          </label>
          <textarea
            id="project-description"
            className="field__input project-form__textarea"
            rows={3}
            value={values.description}
            onChange={(event) => set('description', event.target.value)}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="project-objective">
            Mục tiêu
          </label>
          <textarea
            id="project-objective"
            className="field__input project-form__textarea"
            rows={3}
            value={values.objective}
            onChange={(event) => set('objective', event.target.value)}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="project-scope">
            Phạm vi
          </label>
          <textarea
            id="project-scope"
            className="field__input project-form__textarea"
            rows={3}
            value={values.scope}
            onChange={(event) => set('scope', event.target.value)}
          />
        </div>

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang lưu...' : 'Lưu project'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
