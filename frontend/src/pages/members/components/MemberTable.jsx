import { Eye, Lock, Pencil } from 'lucide-react'
import IconButton from '../../../components/ui/IconButton'
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
  onInactive,
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
            <col className="member-table__col-index" />
            <col className="member-table__col-member" />
            <col className="member-table__col-code" />
            <col className="member-table__col-team" />
            <col className="member-table__col-role" />
            <col className="member-table__col-status" />
            <col className="member-table__col-updated" />
            <col className="member-table__col-actions" />
          </colgroup>
          <thead>
            <tr>
              <th scope="col">STT</th>
              <th scope="col">Thành viên</th>
              <th scope="col">Mã NV</th>
              <th scope="col">Phòng ban</th>
              <th scope="col">Vai trò</th>
              <th scope="col">Trạng thái</th>
              <th scope="col">Cập nhật</th>
              <th scope="col">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {members.map((member, index) => {
              const isSelected = selectedMemberId === member.id
              const rowNumber = (page - 1) * pageSize + index + 1

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
                  <td className="member-table__index">{rowNumber}</td>
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
                    <div className="member-table__action-group">
                      <IconButton
                        label="Xem chi tiết"
                        onClick={(event) => {
                          event.stopPropagation()
                          onSelect(member)
                        }}
                      >
                        <Eye size={15} aria-hidden="true" />
                      </IconButton>
                      <IconButton
                        label="Chỉnh sửa"
                        onClick={(event) => {
                          event.stopPropagation()
                          onEdit(member)
                        }}
                      >
                        <Pencil size={15} aria-hidden="true" />
                      </IconButton>
                      <IconButton
                        label="Khóa tài khoản"
                        variant="danger"
                        onClick={(event) => {
                          event.stopPropagation()
                          onInactive(member)
                        }}
                      >
                        <Lock size={15} aria-hidden="true" />
                      </IconButton>
                    </div>
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
