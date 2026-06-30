import { useAuth } from '../../context/AuthContext'
import HeaderNotifications from './HeaderNotifications'
import HeaderUserMenu from './HeaderUserMenu'

export default function Header() {
  const { user, logout } = useAuth()

  return (
    <header className="header">
      <p className="header__title">Hệ thống Quản lý Công việc</p>

      <div className="header__actions">
        <HeaderNotifications />
        <HeaderUserMenu user={user} onLogout={logout} />
      </div>
    </header>
  )
}
