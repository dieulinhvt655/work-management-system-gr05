import { Search } from 'lucide-react'
import {
  MEMBER_ORG_STATUS_OPTIONS,
} from '../../../constants/members'
import { FILTER_ALL } from '../utils/filterMembers'

export default function MemberFilters({
  filters,
  onChange,
  resultCount,
  teams = [],
}) {
  const set = (key, value) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <section className="member-filters" aria-label="Bộ lọc thành viên">
      <div className="member-filters__search-wrap">
        <Search
          size={15}
          className="member-filters__search-icon"
          aria-hidden="true"
        />
        <input
          type="search"
          className="member-filters__search"
          placeholder="Tìm theo tên, email, mã NV, phòng ban..."
          value={filters.search}
          onChange={(event) => set('search', event.target.value)}
          aria-label="Tìm kiếm thành viên"
        />
      </div>

      <select
        className="member-filters__select"
        value={filters.teamId}
        onChange={(event) => set('teamId', event.target.value)}
        aria-label="Lọc theo Team hoặc Department"
      >
        <option value={FILTER_ALL}>Tất cả Team / Department</option>
        <option value="unassigned">Chưa phân nhóm</option>
        {teams.map((team) => (
          <option key={team.id} value={team.id}>
            {team.name}
          </option>
        ))}
      </select>

      <select
        className="member-filters__select"
        value={filters.status}
        onChange={(event) => set('status', event.target.value)}
        aria-label="Lọc theo trạng thái tổ chức"
      >
        <option value={FILTER_ALL}>Đang hoạt động</option>
        {MEMBER_ORG_STATUS_OPTIONS.map(({ value, label }) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      <select
        className="member-filters__select"
        value={filters.availability}
        onChange={(event) => set('availability', event.target.value)}
        aria-label="Lọc theo phân bổ dự án"
      >
        <option value={FILTER_ALL}>Tất cả phân bổ</option>
        <option value="allocated">Đang tham gia dự án</option>
        <option value="available">Chưa tham gia dự án</option>
      </select>

      <span className="member-filters__count">{resultCount} thành viên</span>
    </section>
  )
}

export { FILTER_ALL }
