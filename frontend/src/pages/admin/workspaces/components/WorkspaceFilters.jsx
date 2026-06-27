import { Search } from 'lucide-react'
import { WORKSPACE_STATUS_LABELS } from '../../../../constants/workspaces'

const ALL = ''

export default function WorkspaceFilters({
  filters,
  onChange,
  resultCount,
  owners = [],
}) {
  const set = (key, value) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <div className="workspace-filters">
      <div className="workspace-filters__primary">
        <div className="workspace-filters__search">
          <label className="workspace-filters__label" htmlFor="filter-workspace-search">
            Tìm kiếm
          </label>
          <div className="workspace-filters__search-wrap">
            <Search
              size={15}
              className="workspace-filters__search-icon"
              aria-hidden="true"
            />
            <input
              id="filter-workspace-search"
              type="search"
              className="workspace-filters__control workspace-filters__control--search"
              placeholder="Tên, mã, Workspace Owner..."
              value={filters.search}
              onChange={(event) => set('search', event.target.value)}
              aria-label="Tìm kiếm workspace"
            />
          </div>
        </div>

        <span className="workspace-filters__count-badge">
          {resultCount} workspace
        </span>
      </div>

      <div className="workspace-filters__secondary">
        <div className="workspace-filters__field">
          <label className="workspace-filters__label" htmlFor="filter-workspace-status">
            Trạng thái
          </label>
          <select
            id="filter-workspace-status"
            className="workspace-filters__control"
            value={filters.status}
            onChange={(event) => set('status', event.target.value)}
          >
            <option value={ALL}>Tất cả</option>
            {Object.entries(WORKSPACE_STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        <div className="workspace-filters__field">
          <label className="workspace-filters__label" htmlFor="filter-workspace-created">
            Ngày tạo
          </label>
          <input
            id="filter-workspace-created"
            type="date"
            className="workspace-filters__control"
            value={filters.createdDate}
            onChange={(event) => set('createdDate', event.target.value)}
          />
        </div>

        <div className="workspace-filters__field">
          <label className="workspace-filters__label" htmlFor="filter-workspace-owner">
            Owner
          </label>
          <select
            id="filter-workspace-owner"
            className="workspace-filters__control"
            value={filters.ownerId}
            onChange={(event) => set('ownerId', event.target.value)}
            disabled={owners.length === 0}
          >
            <option value={ALL}>Tất cả</option>
            {owners.map((owner) => (
              <option key={owner.id} value={owner.id}>
                {owner.fullName}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  )
}

export { ALL as FILTER_ALL }
