import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { fetchProjectById } from '../api/projectsApi'
import { useAuth } from '../context/AuthContext'

export function useProject() {
  const { projectId } = useParams()
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const workspaceId = user?.workspaceId

  const {
    data: project,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['projects', workspaceId, projectId],
    queryFn: () => fetchProjectById(projectId, workspaceId),
    enabled: isAuthenticated && !authLoading && Boolean(projectId),
  })

  return {
    projectId,
    project: project ?? null,
    isLoading: authLoading || isLoading,
    notFound: Boolean(projectId && !isLoading && (isError || !project)),
  }
}
