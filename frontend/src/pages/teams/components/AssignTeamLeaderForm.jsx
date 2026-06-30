import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../../components/ui/Button'
import SelectField from '../../../components/ui/SelectField'
import UserAvatar from '../../../components/common/UserAvatar'

export default function AssignTeamLeaderForm({
  team,
  onSave,
  isSaving = false,
  saveError = '',
  showCurrentLeader = true,
  onCancel,
  submitLabel = 'Gán trưởng nhóm',
  inline = false,
}) {
  const teamMemberOptions = useMemo(
    () => (team.members ?? []).filter((member) => !member.isLeader),
    [team.members],
  )

  const [teamMemberId, setTeamMemberId] = useState('')
  const [validationError, setValidationError] = useState('')

  useEffect(() => {
    const eligible = (team.members ?? []).filter((member) => !member.isLeader)
    setTeamMemberId(eligible[0]?.id ?? '')
    setValidationError('')
  }, [team.id, team.members])

  const handleSubmit = (event) => {
    event.preventDefault()
    setValidationError('')

    if (!teamMemberId) {
      setValidationError('Vui lòng chọn thành viên trong phòng ban.')
      return
    }

    onSave({ teamMemberId })
  }

  const hasEligibleMembers = teamMemberOptions.length > 0

  return (
    <form
      className={`team-leader-form${inline ? ' team-leader-form--inline' : ''}`}
      onSubmit={handleSubmit}
      noValidate
    >
      {(validationError || saveError) && (
        <p className="team-leader-form__error" role="alert">
          {validationError || saveError}
        </p>
      )}

      {showCurrentLeader && (
        <div className="assign-team-leader-page__current">
          <p className="assign-team-leader-page__current-label">Trưởng nhóm hiện tại</p>
          {team.leader ? (
            <div className="assign-team-leader-page__current-leader">
              <UserAvatar fullName={team.leader.fullName} size="sm" />
              <span>{team.leader.fullName}</span>
            </div>
          ) : (
            <p className="assign-team-leader-page__current-empty">Chưa có trưởng nhóm</p>
          )}
        </div>
      )}

      {!hasEligibleMembers ? (
        <p className="team-leader-form__hint team-leader-form__hint--notice">
          Chỉ có thể gán trưởng nhóm từ thành viên đã thuộc phòng ban này.{' '}
          <Link to="/teams/assign-members">Phân công nhân viên</Link> vào phòng ban
          trước khi gán Team Leader.
        </p>
      ) : (
        <>
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
              </option>
            ))}
          </SelectField>

          <p className="team-leader-form__hint">
            Chỉ chọn từ thành viên hiện có trong phòng ban. Nhân viên được chọn sẽ
            được gán vai trò <strong>Team Leader</strong>. Nếu đã có trưởng nhóm,
            quyền sẽ được chuyển sang thành viên mới.
          </p>

          <div className={inline ? 'team-leader-form__footer' : 'modal__footer'}>
            {onCancel && (
              <Button type="button" variant="ghost" onClick={onCancel}>
                Hủy
              </Button>
            )}
            <Button type="submit" variant="primary" disabled={isSaving}>
              {isSaving ? 'Đang gán...' : submitLabel}
            </Button>
          </div>
        </>
      )}
    </form>
  )
}
