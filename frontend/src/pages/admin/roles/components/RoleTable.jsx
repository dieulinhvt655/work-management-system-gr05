import { Pencil, Shield, Trash2 } from 'lucide-react'
import { ROLE_SCOPE_LABELS } from '../../../../constants/roles'

export default function RoleTable({
  roles,
  onEdit,
  onDelete,
  emptyTitle = 'Chưa có vai trò',
  emptyText = 'Tạo vai trò đầu tiên để phân quyền hệ thống.',
}) {
  if (roles.length === 0) {
    return (
      <div className="roles-empty">
        <Shield size={32} aria-hidden="true" />
        <p className="roles-empty__title">{emptyTitle}</p>
        <p className="roles-empty__text">{emptyText}</p>
      </div>
    )
  }

  return (
    <div className="roles-table-wrap">
      <table className="roles-table">
        <thead>
          <tr>
            <th scope="col">Vai trò</th>
            <th scope="col">Phạm vi</th>
            <th scope="col">Mô tả</th>
            <th scope="col">Quyền</th>
            <th scope="col" className="roles-table__actions-col">
              Thao tác
            </th>
          </tr>
        </thead>
        <tbody>
          {roles.map((role) => (
            <tr key={role.id}>
              <td>
                <div className="roles-table__name-cell">
                  <span className="roles-table__name">{role.name}</span>
                  <span className="roles-table__id">ID: {role.id}</span>
                </div>
              </td>
              <td>
                <span className={`roles-scope-badge roles-scope-badge--${role.scope.toLowerCase()}`}>
                  {ROLE_SCOPE_LABELS[role.scope] ?? role.scope}
                </span>
              </td>
              <td className="roles-table__description">
                {role.description || '—'}
              </td>
              <td>
                <span className="roles-table__perm-count">
                  {role.permissions.length} quyền
                </span>
              </td>
              <td className="roles-table__actions">
                <button
                  type="button"
                  className="roles-table__action-btn"
                  onClick={() => onEdit(role)}
                  aria-label={`Chỉnh sửa ${role.name}`}
                >
                  <Pencil size={15} aria-hidden="true" />
                  Sửa
                </button>
                <button
                  type="button"
                  className="roles-table__action-btn roles-table__action-btn--danger"
                  onClick={() => onDelete(role)}
                  aria-label={`Xóa ${role.name}`}
                >
                  <Trash2 size={15} aria-hidden="true" />
                  Xóa
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
