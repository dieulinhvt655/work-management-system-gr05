import { Search } from 'lucide-react'
import { USER_STATUS_LABELS } from '../../../../constants/users'

const ALL = ''

export default function UserFilters({
  filters,
  onChange,
  resultCount,
  departments = [],
  roles = [],
}) {
  const set = (key, value) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <div className="user-filters">
      <div className="user-filters__primary">
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
            />
          </div>
        </div>

        <span className="user-filters__count-badge">
          {resultCount} tài khoản
        </span>
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
          >
            <option value={ALL}>Tất cả</option>
            {roles.map((role) => (
              <option key={role.id} value={role.id}>
                {role.name}
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
            disabled={departments.length === 0}
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
