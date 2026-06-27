import { Navigate, useParams } from 'react-router-dom'
import { getDefaultProjectTabPath } from '../../constants/navigation/projectTabs'
import { usePermission } from '../../hooks/usePermission'

export default function ProjectIndexRedirect() {
  const { projectId } = useParams()
  const { can } = usePermission()
  const defaultTab = getDefaultProjectTabPath(can)

  if (!defaultTab) {
    return <Navigate to="/403" replace />
  }

  return <Navigate to={`/projects/${projectId}/${defaultTab}`} replace />
}
