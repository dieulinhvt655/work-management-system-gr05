import { useMemo } from 'react'
import { NavLink, useParams } from 'react-router-dom'
import { PROJECT_TABS } from '../../constants/navigation/projectTabs'
import { usePermission } from '../../hooks/usePermission'

export default function ProjectTabs() {
  const { projectId } = useParams()
  const { can } = usePermission()

  const visibleTabs = useMemo(
    () => PROJECT_TABS.filter((tab) => can(tab.permission)),
    [can],
  )

  if (!projectId || visibleTabs.length === 0) {
    return null
  }

  return (
    <nav className="project-tabs" aria-label="Project sections">
      {visibleTabs.map((tab) => (
        <NavLink
          key={tab.id}
          to={`/projects/${projectId}/${tab.path}`}
          className={({ isActive }) =>
            `project-tabs__link${isActive ? ' project-tabs__link--active' : ''}`
          }
          end
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}
