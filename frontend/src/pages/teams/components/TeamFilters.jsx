import { Search } from 'lucide-react'
import {
  LEADER_FILTER,
  LEADER_FILTER_LABELS,
  TEAM_STATUS_LABELS,
} from '../../../constants/teams'
import { FILTER_ALL } from '../utils/filterTeams'

export default function TeamFilters({ filters, onChange, resultCount }) {
  const set = (key, value) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <div className="team-filters">
      <div className="team-filters__search-wrap">
        <Search size={16} className="team-filters__search-icon" aria-hidden="true" />
        <input
          type="search"
          className="team-filters__search"
          placeholder="Tìm kiếm theo tên phòng ban, mã phòng ban..."
          value={filters.search}
          onChange={(event) => set('search', event.target.value)}
          aria-label="Tìm kiếm phòng ban / nhóm"
        />
      </div>

      <select
        className="team-filters__select"
        value={filters.status}
        onChange={(event) => set('status', event.target.value)}
        aria-label="Lọc theo trạng thái"
      >
        <option value={FILTER_ALL}>Tất cả trạng thái</option>
        {Object.entries(TEAM_STATUS_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      <select
        className="team-filters__select"
        value={filters.leader}
        onChange={(event) => set('leader', event.target.value)}
        aria-label="Lọc theo trưởng nhóm"
      >
        <option value={FILTER_ALL}>Tất cả trưởng nhóm</option>
        {Object.entries(LEADER_FILTER_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      <span className="team-filters__count">{resultCount} phòng ban / nhóm</span>
    </div>
  )
}

export { FILTER_ALL }
