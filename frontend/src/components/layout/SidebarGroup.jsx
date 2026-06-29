import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ChevronDown } from 'lucide-react'
import { isNavItemActive } from '../../utils/navUtils'

export default function SidebarGroup({ item }) {
  const location = useLocation()
  const hasActiveChild = item.children.some((child) =>
    isNavItemActive(location.pathname, child.to, item.children),
  )
  const [open, setOpen] = useState(hasActiveChild)
  const Icon = item.icon

  return (
    <div className="sidebar__group">
      <button
        type="button"
        className={`sidebar__group-toggle${hasActiveChild ? ' sidebar__group-toggle--active' : ''}`}
        onClick={() => setOpen((prev) => !prev)}
        aria-expanded={open}
      >
        <span className="sidebar__link-content">
          {Icon && <Icon size={18} aria-hidden="true" />}
          <span className="sidebar__link-label" title={item.label}>{item.label}</span>
        </span>
        <ChevronDown
          size={16}
          className={`sidebar__chevron${open ? ' sidebar__chevron--open' : ''}`}
          aria-hidden="true"
        />
      </button>

      {open && (
        <div className="sidebar__subnav">
          {item.children.map((child) => {
            const active = isNavItemActive(
              location.pathname,
              child.to,
              item.children,
            )

            return (
              <Link
                key={child.id}
                to={child.to}
                title={child.label}
                className={`sidebar__sublink${active ? ' sidebar__sublink--active' : ''}`}
                aria-current={active ? 'page' : undefined}
              >
                {child.label}
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
