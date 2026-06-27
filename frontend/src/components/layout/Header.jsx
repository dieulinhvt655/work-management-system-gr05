import { useAuth } from '../../context/AuthContext'
import HeaderNotifications from './HeaderNotifications'
import HeaderUserMenu from './HeaderUserMenu'
import MockRoleSwitcher from './MockRoleSwitcher'

export default function Header() {
  const { user, logout } = useAuth()

  return (
    <header className="header">
      <p className="header__title">Hệ thống Quản lý Công việc</p>

      <div className="header__actions">
        <MockRoleSwitcher />
        <HeaderNotifications />
        <HeaderUserMenu user={user} onLogout={logout} />
      </div>
    </header>
  )
}
