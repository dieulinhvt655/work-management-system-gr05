import { Pencil } from 'lucide-react'
import PermissionGate from '../../../components/common/PermissionGate'
import UserAvatar from '../../../components/common/UserAvatar'
import IconButton from '../../../components/ui/IconButton'
import { PERMISSIONS } from '../../../constants/permissions'
import { USER_ROLE_LABELS } from '../../../constants/users'
import MemberOrgStatusBadge from './MemberOrgStatusBadge'
import MemberTableFooter from './MemberTableFooter'

function formatDate(value) {
  if (!value) return '—'

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value))
}

export default function MemberTable({
  members,
  selectedMemberId,
  onSelect,
  onEdit,
  page,
  pageSize,
  totalCount,
  onPageChange,
  onPageSizeChange,
}) {
  return (
    <div className="member-table-wrap">
      <div className="member-table-scroll">
        <table className="member-table">
          <colgroup>
            <col className="member-table__col-avatar" />
            <col className="member-table__col-member" />
            <col className="member-table__col-code" />
            <col className="member-table__col-team" />
            <col className="member-table__col-position" />
            <col className="member-table__col-role" />
            <col className="member-table__col-status" />
            <col className="member-table__col-updated" />
            <col className="member-table__col-actions" />
          </colgroup>
          <thead>
            <tr>
              <th scope="col">
                <span className="sr-only">Avatar</span>
              </th>
              <th scope="col">Thành viên</th>
              <th scope="col">Mã NV</th>
              <th scope="col">Team / Department</th>
              <th scope="col">Vị trí</th>
              <th scope="col">Vai trò</th>
              <th scope="col">Trạng thái</th>
              <th scope="col">Cập nhật</th>
              <th scope="col">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {members.map((member) => {
              const isSelected = selectedMemberId === member.id

              return (
                <tr
                  key={member.id}
                  className={`member-table__row${
                    isSelected ? ' member-table__row--selected' : ''
                  }`}
                  onClick={() => onSelect(member)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault()
                      onSelect(member)
                    }
                  }}
                  tabIndex={0}
                  role="button"
                  aria-pressed={isSelected}
                  aria-label={`Xem chi tiết ${member.fullName}`}
                >
                  <td>
                    <UserAvatar
                      fullName={member.fullName}
                      className="user-avatar--table"
                    />
                  </td>
                  <td>
                    <div className="member-table__identity">
                      <span
                        className="member-table__name member-table__truncate"
                        title={member.fullName}
                      >
                        {member.fullName}
                      </span>
                      <span
                        className="member-table__email member-table__truncate"
                        title={member.email}
                      >
                        {member.email}
                      </span>
                    </div>
                  </td>
                  <td>
                    <code className="member-table__code">{member.employeeCode}</code>
                  </td>
                  <td className="member-table__truncate" title={member.teamName}>
                    {member.teamName}
                  </td>
                  <td className="member-table__truncate" title={member.position}>
                    {member.position}
                  </td>
                  <td
                    className="member-table__truncate"
                    title={USER_ROLE_LABELS[member.role]}
                  >
                    {USER_ROLE_LABELS[member.role] ?? member.role}
                  </td>
                  <td>
                    <MemberOrgStatusBadge status={member.organizationStatus} />
                  </td>
                  <td className="member-table__muted">
                    {formatDate(member.updatedAt)}
                  </td>
                  <td className="member-table__actions-cell">
                    <PermissionGate permission={PERMISSIONS.MEMBER_MANAGE}>
                      <IconButton
                        label="Cập nhật thông tin tổ chức"
                        onClick={(event) => {
                          event.stopPropagation()
                          onEdit(member)
                        }}
                      >
                        <Pencil size={15} aria-hidden="true" />
                      </IconButton>
                    </PermissionGate>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <MemberTableFooter
        page={page}
        pageSize={pageSize}
        totalCount={totalCount}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
      />
    </div>
  )
}
