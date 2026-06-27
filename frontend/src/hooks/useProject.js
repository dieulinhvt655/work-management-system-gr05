import { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { getMockProjectById } from '../constants/mock/projects'

export function useProject() {
  const { projectId } = useParams()

  const project = useMemo(
    () => (projectId ? getMockProjectById(projectId) : null),
    [projectId],
  )

  return {
    projectId,
    project,
    isLoading: false,
    notFound: Boolean(projectId && !project),
  }
}
