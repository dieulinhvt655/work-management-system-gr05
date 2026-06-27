import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

export default function AuthBackLink({ to = '/login', children = 'Quay lại đăng nhập' }) {
  return (
    <Link to={to} className="auth-back-link">
      <ArrowLeft size={16} aria-hidden="true" />
      {children}
    </Link>
  )
}
