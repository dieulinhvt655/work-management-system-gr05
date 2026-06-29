import { TEAM_STATUS, TEAM_STATUS_LABELS } from '../../../constants/teams'

export default function TeamStatusBadge({ status }) {
  const isActive = status === TEAM_STATUS.ACTIVE

  return (
    <span
      className={`team-status-badge${isActive ? ' team-status-badge--active' : ' team-status-badge--inactive'}`}
    >
      {TEAM_STATUS_LABELS[status] ?? status}
    </span>
  )
}
