import { NavLink, useParams } from 'react-router-dom'
import { PROJECT_TABS } from '../../constants/navigation/projectTabs'

export default function ProjectTabs() {
  const { projectId } = useParams()

  if (!projectId || PROJECT_TABS.length === 0) {
    return null
  }

  return (
    <nav className="project-tabs" aria-label="Project sections">
      {PROJECT_TABS.map((tab) => (
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
