import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronDown, LogOut, User } from 'lucide-react'
import UserAvatar from '../common/UserAvatar'

export default function HeaderUserMenu({ user, onLogout }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)
  const displayName = user?.fullName || user?.email || 'User'

  useEffect(() => {
    if (!open) return

    const handleClickOutside = (event) => {
      if (!rootRef.current?.contains(event.target)) {
        setOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [open])

  if (!user) return null

  return (
    <div className="header-menu header-menu--user" ref={rootRef}>
      <button
        type="button"
        className="header-user-btn"
        onClick={() => setOpen((value) => !value)}
        aria-label="Menu tài khoản"
        aria-expanded={open}
        aria-haspopup="true"
      >
        <UserAvatar fullName={displayName} size="sm" />
        <span className="header-user-btn__name">{displayName}</span>
        <ChevronDown
          size={16}
          className={`header-user-btn__chevron${open ? ' header-user-btn__chevron--open' : ''}`}
          aria-hidden="true"
        />
      </button>

      {open && (
        <div className="header-menu__panel">
          <div className="header-menu__user-info">
            <UserAvatar fullName={displayName} size="md" />
            <div>
              <p className="header-menu__user-name">{displayName}</p>
              <p className="header-menu__user-email">{user.email}</p>
            </div>
          </div>

          <div className="header-menu__divider" />

          <Link
            to="/profile"
            className="header-menu__item"
            onClick={() => setOpen(false)}
          >
            <User size={16} aria-hidden="true" />
            Hồ sơ cá nhân
          </Link>

          <button
            type="button"
            className="header-menu__item header-menu__item--danger"
            onClick={() => {
              setOpen(false)
              onLogout()
            }}
          >
            <LogOut size={16} aria-hidden="true" />
            Đăng xuất
          </button>
        </div>
      )}
    </div>
  )
}
