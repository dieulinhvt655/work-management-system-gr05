import { useEffect, useRef, useState } from 'react'
import { Bell } from 'lucide-react'

export default function HeaderNotifications() {
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)
  const notifications = []
  const unreadCount = notifications.filter((item) => item.unread).length

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

  return (
    <div className="header-menu" ref={rootRef}>
      <button
        type="button"
        className="header-icon-btn"
        onClick={() => setOpen((value) => !value)}
        aria-label="Thông báo"
        aria-expanded={open}
        aria-haspopup="true"
      >
        <Bell size={18} aria-hidden="true" />
        {unreadCount > 0 && (
          <span className="header-icon-btn__badge">{unreadCount}</span>
        )}
      </button>

      {open && (
        <div className="header-menu__panel header-menu__panel--notifications">
          <div className="header-menu__panel-header">
            <p className="header-menu__panel-title">Thông báo</p>
            {unreadCount > 0 && (
              <span className="header-menu__panel-meta">
                {unreadCount} chưa đọc
              </span>
            )}
          </div>

          <p className="header-menu__empty">Chưa có thông báo.</p>
        </div>
      )}
    </div>
  )
}
