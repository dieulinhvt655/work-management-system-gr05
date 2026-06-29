import { useMemo, useState } from 'react'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'
import SelectField from '../../../components/ui/SelectField'

export default function AddTeamMemberModal({
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

  const [workspaceMemberId, setWorkspaceMemberId] = useState(candidates[0]?.id ?? '')
  const [validationError, setValidationError] = useState('')

  const handleSubmit = (event) => {
    event.preventDefault()
    setValidationError('')

    if (!workspaceMemberId) {
      setValidationError('Vui lòng chọn nhân viên.')
      return
    }

    onSave({ workspaceMemberId })
  }

  return (
    <Modal
      title="Thêm nhân viên vào phòng ban"
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

        <SelectField
          id="workspace-member-add"
          label="Nhân viên workspace *"
          value={workspaceMemberId}
          onChange={(event) => setWorkspaceMemberId(event.target.value)}
          disabled={candidates.length === 0}
        >
          <option value="">
            {candidates.length === 0
              ? 'Tất cả nhân viên đã thuộc phòng ban này'
              : 'Chọn nhân viên'}
          </option>
          {candidates.map((member) => (
            <option key={member.id} value={member.id}>
              {member.fullName}
              {member.employeeCode ? ` · ${member.employeeCode}` : ''}
            </option>
          ))}
        </SelectField>

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang thêm...' : 'Thêm nhân viên'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
