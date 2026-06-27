import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { ChevronDown } from 'lucide-react'

export default function SidebarGroup({ item }) {
  const location = useLocation()
  const hasActiveChild = item.children.some(
    (child) =>
      location.pathname === child.to ||
      location.pathname.startsWith(`${child.to}/`),
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
          <span className="sidebar__link-label">{item.label}</span>
        </span>
        <ChevronDown
          size={16}
          className={`sidebar__chevron${open ? ' sidebar__chevron--open' : ''}`}
          aria-hidden="true"
        />
      </button>

      {open && (
        <div className="sidebar__subnav">
          {item.children.map((child) => (
            <NavLink
              key={child.id}
              to={child.to}
              className={({ isActive }) =>
                `sidebar__sublink${isActive ? ' sidebar__sublink--active' : ''}`
              }
            >
              {child.label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  )
}
