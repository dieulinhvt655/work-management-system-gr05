import { useMemo } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { getNavItemsForUser } from '../../constants/navigation/navItems'
import { useAuth } from '../../context/AuthContext'
import { usePermission } from '../../hooks/usePermission'
import { useResizableSidebar } from '../../hooks/useResizableSidebar'
import { filterNavItems, collectNavRoutes, isNavItemActive } from '../../utils/navUtils'
import SidebarGroup from './SidebarGroup'

export default function Sidebar() {
  const { user } = useAuth()
  const { can } = usePermission()
  const location = useLocation()
  const { width, isResizing, startResize, resetWidth } = useResizableSidebar()

  const visibleItems = useMemo(
    () => filterNavItems(getNavItemsForUser(user), can),
    [user, can],
  )

  const allRoutes = useMemo(
    () => collectNavRoutes(visibleItems),
    [visibleItems],
  )

  return (
    <aside
      className={`sidebar${isResizing ? ' sidebar--resizing' : ''}`}
      style={{ width }}
    >
      <div className="sidebar__brand">
        <span className="sidebar__brand-icon">W</span>
        <span className="sidebar__brand-text" title="Work Management">
          Work Management
        </span>
      </div>

      <nav className="sidebar__nav">
        {visibleItems.map((item) => {
          if (item.children?.length) {
            return <SidebarGroup key={item.id} item={item} />
          }

          const Icon = item.icon
          const siblingRoutes = allRoutes
            .filter((to) => to !== item.to)
            .map((to) => ({ to }))
          const active = isNavItemActive(location.pathname, item.to, siblingRoutes)

          return (
            <Link
              key={item.id}
              to={item.to}
              title={item.label}
              className={`sidebar__link${active ? ' sidebar__link--active' : ''}`}
              aria-current={active ? 'page' : undefined}
            >
              {Icon && <Icon size={18} aria-hidden="true" />}
              <span className="sidebar__link-label">{item.label}</span>
            </Link>
          )
        })}
      </nav>

      <button
        type="button"
        className="sidebar__resize-handle"
        aria-label="Kéo để thay đổi độ rộng sidebar"
        onMouseDown={startResize}
        onDoubleClick={resetWidth}
      />
    </aside>
  )
}
