import { Link } from 'react-router-dom'
import { getDefaultRoute } from '../../utils/navUtils'
import { useAuth } from '../../context/AuthContext'

export default function ForbiddenPage() {
  const { permissions } = useAuth()
  const fallback = getDefaultRoute(permissions)

  return (
    <div className="page page-forbidden">
      <p className="page-forbidden__message">
        Bạn không có quyền xem trang này. Liên hệ quản trị viên nếu bạn cho
        rằng đây là lỗi.
      </p>
      {fallback !== '/403' && (
        <Link to={fallback} className="btn btn--ghost">
          Quay về trang chính
        </Link>
      )}
    </div>
  )
}
