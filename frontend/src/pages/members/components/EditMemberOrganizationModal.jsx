import { useState } from 'react'
import Button from '../../../components/ui/Button'
import Modal from '../../../components/ui/Modal'
import SelectField from '../../../components/ui/SelectField'
import TextField from '../../../components/ui/TextField'
import {
  MEMBER_ORG_STATUS,
  MEMBER_ORG_STATUS_OPTIONS,
} from '../../../constants/members'

export default function EditMemberOrganizationModal({
  member,
  teams = [],
  onClose,
  onSave,
  isSaving = false,
  saveError = '',
}) {
  const [values, setValues] = useState({
    teamId: member.teamId ?? '',
    organizationStatus: member.organizationStatus,
    position: member.position === '—' ? '' : member.position,
    note: '',
  })
  const [validationError, setValidationError] = useState('')

  const set = (key, value) => {
    setValidationError('')
    setValues((current) => ({ ...current, [key]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    if (
      values.organizationStatus === MEMBER_ORG_STATUS.ACTIVE &&
      teams.length > 0 &&
      !values.teamId
    ) {
      setValidationError(
        'Thành viên đang hoạt động cần thuộc một Team hoặc Department.',
      )
      return
    }

    onSave(member.id, {
      teamId: values.teamId,
      organizationStatus: values.organizationStatus,
      position: values.position,
      note: values.note,
    })
  }

  return (
    <Modal
      title="Cập nhật thông tin tổ chức"
      description={`${member.fullName} · ${member.employeeCode}`}
      onClose={onClose}
      size="md"
    >
      <form className="member-form" onSubmit={handleSubmit} noValidate>
        {(validationError || saveError) && (
          <p className="member-form__error" role="alert">
            {validationError || saveError}
          </p>
        )}

        <SelectField
          id="member-team"
          label="Team / Department"
          value={values.teamId}
          onChange={(event) => set('teamId', event.target.value)}
          disabled={teams.length === 0}
        >
          <option value="">
            {teams.length === 0
              ? 'Chưa có Team / Department hoạt động'
              : 'Chọn Team / Department'}
          </option>
          {teams.map((team) => (
            <option key={team.id} value={team.id}>
              {team.name}
            </option>
          ))}
        </SelectField>

        <SelectField
          id="member-status"
          label="Trạng thái tham gia tổ chức"
          value={values.organizationStatus}
          onChange={(event) => set('organizationStatus', event.target.value)}
        >
          {MEMBER_ORG_STATUS_OPTIONS.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>

        <TextField
          id="member-position"
          label="Vị trí / chức danh"
          value={values.position}
          onChange={(event) => set('position', event.target.value)}
          placeholder="Ví dụ: Backend Engineer"
        />

        <div className="field">
          <label className="field__label" htmlFor="member-note">
            Ghi chú thay đổi
          </label>
          <textarea
            id="member-note"
            className="field__input member-form__textarea"
            value={values.note}
            onChange={(event) => set('note', event.target.value)}
            placeholder="Lý do chuyển nhóm hoặc cập nhật trạng thái"
            rows={3}
          />
        </div>

        <div className="modal__footer">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={isSaving}>
            {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
