import { USER_STATUS_LABELS } from '../../../../constants/users'

export default function UserStatusBadge({ status }) {
  return (
    <span className={`user-status user-status--${status.toLowerCase()}`}>
      <span className="user-status__dot" aria-hidden="true" />
      {USER_STATUS_LABELS[status] ?? status}
    </span>
  )
}
