import { Layers } from 'lucide-react'
import UserTable from './UserTable'

export default function WorkspaceUsersSection({
  workspaceId,
  workspaceName,
  workspaceCode,
  users,
  onView,
  onEdit,
  onChangeRole,
  onToggleLock,
  hasActiveFilters,
}) {
  const title = workspaceName || 'Workspace'

  return (
    <section className="workspace-users-section" aria-labelledby={`workspace-${workspaceId}`}>
      <header className="workspace-users-section__header">
        <span className="workspace-users-section__icon" aria-hidden="true">
          <Layers size={16} strokeWidth={1.75} />
        </span>
        <div className="workspace-users-section__heading">
          <h2 id={`workspace-${workspaceId}`} className="workspace-users-section__title">
            {title}
            {workspaceCode && (
              <span className="workspace-users-section__code">{workspaceCode}</span>
            )}
          </h2>
          <span className="workspace-users-section__meta">
            {users.length} tài khoản
          </span>
        </div>
      </header>

      <div className="workspace-users-section__body">
        {users.length === 0 ? (
          <div className="user-table-empty user-table-empty--inline">
            <p className="user-table-empty__title">
              {hasActiveFilters ? 'Không tìm thấy kết quả' : 'Chưa có người dùng'}
            </p>
            <p className="user-table-empty__text">
              {hasActiveFilters
                ? 'Thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.'
                : 'Tài khoản trong workspace này sẽ hiển thị tại đây.'}
            </p>
          </div>
        ) : (
          <UserTable
            users={users}
            onView={onView}
            onEdit={onEdit}
            onChangeRole={onChangeRole}
            onToggleLock={onToggleLock}
          />
        )}
      </div>
    </section>
  )
}
