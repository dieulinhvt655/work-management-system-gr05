import { MEMBER_ORG_STATUS_LABELS } from '../../../constants/members'
import { getMemberStatusTone } from '../utils/filterMembers'

export default function MemberOrgStatusBadge({ status }) {
  return (
    <span className={`member-status member-status--${getMemberStatusTone(status)}`}>
      <span className="member-status__dot" aria-hidden="true" />
      {MEMBER_ORG_STATUS_LABELS[status] ?? status}
    </span>
  )
}
