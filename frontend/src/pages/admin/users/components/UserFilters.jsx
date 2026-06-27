import { Search } from 'lucide-react'
import { USER_ROLE_OPTIONS, USER_STATUS_LABELS } from '../../../../constants/users'

const ALL = ''

export default function UserFilters({
  filters,
  onChange,
  resultCount,
  departments = [],
  workspaces = [],
}) {
  const set = (key, value) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <div className="user-filters">
      <div className="user-filters__primary">
        <div className="user-filters__field user-filters__field--workspace">
          <label className="user-filters__label" htmlFor="filter-workspace">
            Workspace
          </label>
          <select
            id="filter-workspace"
            className="user-filters__control"
            value={filters.workspaceId}
            onChange={(event) => set('workspaceId', event.target.value)}
            disabled={workspaces.length === 0}
          >
            {workspaces.length === 0 ? (
              <option value="">Chưa có workspace</option>
            ) : (
              workspaces.map((workspace) => (
                <option key={workspace.id} value={workspace.id}>
                  {workspace.code
                    ? `${workspace.name} (${workspace.code})`
                    : workspace.name}
                </option>
              ))
            )}
          </select>
        </div>

        <div className="user-filters__search">
          <label className="user-filters__label" htmlFor="filter-search">
            Tìm kiếm
          </label>
          <div className="user-filters__search-wrap">
            <Search size={15} className="user-filters__search-icon" aria-hidden="true" />
            <input
              id="filter-search"
              type="search"
              className="user-filters__control user-filters__control--search"
              placeholder="Họ tên, email, mã NV..."
              value={filters.search}
              onChange={(event) => set('search', event.target.value)}
              aria-label="Tìm kiếm người dùng"
              disabled={!filters.workspaceId}
            />
          </div>
        </div>

        {filters.workspaceId && (
          <span className="user-filters__count-badge">
            {resultCount} người dùng
          </span>
        )}
      </div>

      <div className="user-filters__secondary">
        <div className="user-filters__field">
          <label className="user-filters__label" htmlFor="filter-role">
            Vai trò
          </label>
          <select
            id="filter-role"
            className="user-filters__control"
            value={filters.role}
            onChange={(event) => set('role', event.target.value)}
            disabled={!filters.workspaceId}
          >
            <option value={ALL}>Tất cả</option>
            {USER_ROLE_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        <div className="user-filters__field">
          <label className="user-filters__label" htmlFor="filter-department">
            Phòng ban
          </label>
          <select
            id="filter-department"
            className="user-filters__control"
            value={filters.departmentId}
            onChange={(event) => set('departmentId', event.target.value)}
            disabled={!filters.workspaceId || departments.length === 0}
          >
            <option value={ALL}>
              {departments.length === 0 ? 'Chưa có' : 'Tất cả'}
            </option>
            {departments.map((dept) => (
              <option key={dept.id} value={dept.id}>
                {dept.name}
              </option>
            ))}
          </select>
        </div>

        <div className="user-filters__field">
          <label className="user-filters__label" htmlFor="filter-status">
            Trạng thái
          </label>
          <select
            id="filter-status"
            className="user-filters__control"
            value={filters.status}
            onChange={(event) => set('status', event.target.value)}
            disabled={!filters.workspaceId}
          >
            <option value={ALL}>Tất cả</option>
            {Object.entries(USER_STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  )
}

export { ALL as FILTER_ALL }
