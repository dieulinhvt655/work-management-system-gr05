import { ChevronLeft, ChevronRight } from 'lucide-react'
import Button from '../../../components/ui/Button'

const PAGE_SIZE_OPTIONS = [5, 10, 25]

export default function MemberTableFooter({
  page,
  pageSize,
  totalCount,
  onPageChange,
  onPageSizeChange,
}) {
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize))
  const safePage = Math.min(page, totalPages)
  const start = totalCount === 0 ? 0 : (safePage - 1) * pageSize + 1
  const end = Math.min(safePage * pageSize, totalCount)

  return (
    <footer className="member-table-footer">
      <div className="member-table-footer__summary">
        Hiển thị {start}–{end} / {totalCount} thành viên
      </div>

      <div className="member-table-footer__controls">
        <label className="member-table-footer__page-size">
          <span>Số dòng</span>
          <select
            value={pageSize}
            onChange={(event) => onPageSizeChange(Number(event.target.value))}
            aria-label="Số dòng mỗi trang"
          >
            {PAGE_SIZE_OPTIONS.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </label>

        <div className="member-table-footer__pager">
          <Button
            type="button"
            variant="ghost"
            className="member-table-footer__pager-btn"
            onClick={() => onPageChange(safePage - 1)}
            disabled={safePage <= 1}
            aria-label="Trang trước"
          >
            <ChevronLeft size={16} aria-hidden="true" />
          </Button>
          <span className="member-table-footer__page-indicator">
            {safePage} / {totalPages}
          </span>
          <Button
            type="button"
            variant="ghost"
            className="member-table-footer__pager-btn"
            onClick={() => onPageChange(safePage + 1)}
            disabled={safePage >= totalPages}
            aria-label="Trang sau"
          >
            <ChevronRight size={16} aria-hidden="true" />
          </Button>
        </div>
      </div>
    </footer>
  )
}
