import { useEffect, useRef, useState } from 'react'
import { Bell } from 'lucide-react'

const MOCK_NOTIFICATIONS = [
  {
    id: 'n1',
    title: 'Task được giao mới',
    message: 'Bạn được giao task "Thiết kế API auth"',
    time: '5 phút trước',
    unread: true,
  },
  {
    id: 'n2',
    title: 'Sprint sắp kết thúc',
    message: 'Sprint 12 kết thúc trong 2 ngày',
    time: '1 giờ trước',
    unread: true,
  },
  {
    id: 'n3',
    title: 'Thành viên mới',
    message: 'Hoàng Đức Anh đã tham gia dự án WMS',
    time: 'Hôm qua',
    unread: false,
  },
]

export default function HeaderNotifications() {
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)
  const unreadCount = MOCK_NOTIFICATIONS.filter((item) => item.unread).length

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

          <ul className="header-notifications">
            {MOCK_NOTIFICATIONS.map((item) => (
              <li key={item.id}>
                <button
                  type="button"
                  className={`header-notification${item.unread ? ' header-notification--unread' : ''}`}
                >
                  <span className="header-notification__title">{item.title}</span>
                  <span className="header-notification__message">
                    {item.message}
                  </span>
                  <span className="header-notification__time">{item.time}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
