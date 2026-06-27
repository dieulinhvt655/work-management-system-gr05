import { useAuth } from '../../context/AuthContext'

export default function Header() {
  const { user, logout } = useAuth()

  return (
    <header className="header">
      <p className="header__title">Hệ thống Quản lý Công việc</p>

      <div className="header__actions">
        {user?.email && <span className="header__user">{user.email}</span>}
        <button type="button" className="btn btn--ghost" onClick={logout}>
          Đăng xuất
        </button>
      </div>
    </header>
  )
}
