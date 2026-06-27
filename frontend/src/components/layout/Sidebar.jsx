import { NavLink } from 'react-router-dom'
import {
  Briefcase,
  FolderKanban,
  LayoutDashboard,
  ListTodo,
  Rocket,
  User,
} from 'lucide-react'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/workspace', label: 'Workspace', icon: Briefcase },
  { to: '/projects', label: 'Dự án', icon: FolderKanban },
  { to: '/backlog', label: 'Product Backlog', icon: ListTodo },
  { to: '/sprints', label: 'Sprint', icon: Rocket },
  { to: '/profile', label: 'Hồ sơ cá nhân', icon: User },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <span className="sidebar__brand-icon">W</span>
        <span>Work Management</span>
      </div>

      <nav className="sidebar__nav">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `sidebar__link${isActive ? ' sidebar__link--active' : ''}`
            }
          >
            <Icon size={18} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
