import { useMemo, useState } from 'react'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'
import SelectField from '../../../components/ui/SelectField'

export default function AssignTeamLeaderModal({
  team,
  workspaceMembers = [],
  onClose,
  onSave,
  isSaving = false,
  saveError = '',
}) {
  const existingMemberIds = useMemo(
    () => new Set((team.members ?? []).map((member) => member.workspaceMemberId)),
    [team.members],
  )

  const candidates = useMemo(
    () =>
      workspaceMembers.filter(
        (member) =>
          member.organizationStatus === 'ACTIVE' &&
          !existingMemberIds.has(member.id),
      ),
    [existingMemberIds, workspaceMembers],
  )

  const teamMemberOptions = team.members ?? []

  const [mode, setMode] = useState(
    teamMemberOptions.length > 0 ? 'existing' : 'new',
  )
  const [workspaceMemberId, setWorkspaceMemberId] = useState(
    candidates[0]?.id ?? '',
  )
  const [teamMemberId, setTeamMemberId] = useState(teamMemberOptions[0]?.id ?? '')
  const [validationError, setValidationError] = useState('')

  const handleSubmit = (event) => {
    event.preventDefault()
    setValidationError('')

    if (mode === 'existing') {
      if (!teamMemberId) {
        setValidationError('Vui lòng chọn thành viên trong phòng ban.')
        return
      }

      onSave({
        mode: 'existing',
        teamMemberId,
      })
      return
    }

    if (!workspaceMemberId) {
      setValidationError('Vui lòng chọn nhân viên workspace.')
      return
    }

    onSave({
      mode: 'new',
      workspaceMemberId,
    })
  }

  return (
    <Modal
      title="Gán Team Leader"
      description={`Phòng ban: ${team.name}`}
      onClose={onClose}
      size="md"
    >
      <form className="team-leader-form" onSubmit={handleSubmit} noValidate>
        {(validationError || saveError) && (
          <p className="team-leader-form__error" role="alert">
            {validationError || saveError}
          </p>
        )}

        {teamMemberOptions.length > 0 && (
          <div className="team-leader-form__mode">
            <label className="team-leader-form__mode-option">
              <input
                type="radio"
                name="assign-mode"
                value="existing"
                checked={mode === 'existing'}
                onChange={() => setMode('existing')}
              />
              Chọn từ thành viên hiện có
            </label>
            <label className="team-leader-form__mode-option">
              <input
                type="radio"
                name="assign-mode"
                value="new"
                checked={mode === 'new'}
                onChange={() => setMode('new')}
              />
              Thêm nhân viên workspace mới
            </label>
          </div>
        )}

        {mode === 'existing' ? (
          <SelectField
            id="team-member-leader"
            label="Thành viên phòng ban *"
            value={teamMemberId}
            onChange={(event) => setTeamMemberId(event.target.value)}
          >
            <option value="">Chọn thành viên</option>
            {teamMemberOptions.map((member) => (
              <option key={member.id} value={member.id}>
                {member.fullName}
                {member.employeeCode ? ` · ${member.employeeCode}` : ''}
                {member.isLeader ? ' (Trưởng nhóm hiện tại)' : ''}
              </option>
            ))}
          </SelectField>
        ) : (
          <SelectField
            id="workspace-member-leader"
            label="Nhân viên workspace *"
            value={workspaceMemberId}
            onChange={(event) => setWorkspaceMemberId(event.target.value)}
            disabled={candidates.length === 0}
          >
            <option value="">
              {candidates.length === 0
                ? 'Không còn nhân viên khả dụng'
                : 'Chọn nhân viên'}
            </option>
            {candidates.map((member) => (
              <option key={member.id} value={member.id}>
                {member.fullName}
                {member.employeeCode ? ` · ${member.employeeCode}` : ''}
              </option>
            ))}
          </SelectField>
        )}

        <p className="team-leader-form__hint">
          Nhân viên được chọn sẽ được gán vai trò <strong>Team Leader</strong>{' '}
          và trở thành trưởng nhóm của phòng ban.
        </p>

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang gán...' : 'Gán trưởng nhóm'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
