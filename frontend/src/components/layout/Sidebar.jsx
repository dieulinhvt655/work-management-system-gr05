import { useMemo } from 'react'
import { NavLink } from 'react-router-dom'
import { NAV_ITEMS } from '../../constants/navigation/navItems'
import { usePermission } from '../../hooks/usePermission'
import { useResizableSidebar } from '../../hooks/useResizableSidebar'
import { filterNavItems } from '../../utils/navUtils'
import SidebarGroup from './SidebarGroup'

export default function Sidebar() {
  const { can } = usePermission()
  const { width, isResizing, startResize, resetWidth } = useResizableSidebar()

  const visibleItems = useMemo(
    () => filterNavItems(NAV_ITEMS, can),
    [can],
  )

  return (
    <aside
      className={`sidebar${isResizing ? ' sidebar--resizing' : ''}`}
      style={{ width }}
    >
      <div className="sidebar__brand">
        <span className="sidebar__brand-icon">W</span>
        <span className="sidebar__brand-text">Work Management</span>
      </div>

      <nav className="sidebar__nav">
        {visibleItems.map((item) => {
          if (item.children?.length) {
            return <SidebarGroup key={item.id} item={item} />
          }

          const Icon = item.icon
          return (
            <NavLink
              key={item.id}
              to={item.to}
              className={({ isActive }) =>
                `sidebar__link${isActive ? ' sidebar__link--active' : ''}`
              }
            >
              {Icon && <Icon size={18} aria-hidden="true" />}
              <span className="sidebar__link-label">{item.label}</span>
            </NavLink>
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
