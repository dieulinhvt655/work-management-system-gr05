import { useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createProject } from '../../api/projectsApi'
import { fetchTeams } from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import ProjectFormModal from './components/ProjectFormModal'

export default function CreateProjectPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const [formError, setFormError] = useState('')

  const { data: teams = [], isLoading } = useQuery({
    queryKey: ['teams', user?.workspaceId],
    queryFn: () => fetchTeams(user?.workspaceId),
    enabled: isAuthenticated && !authLoading,
  })

  const teamOptions = useMemo(() => {
    if (user?.isSystemAdmin) return teams
    return teams.filter((team) =>
      (team.members ?? []).some(
        (member) =>
          member.isLeader && String(member.userId) === String(user?.id),
      ),
    )
  }, [teams, user])

  const createMutation = useMutation({
    mutationFn: (values) => {
      const team = teamOptions.find(
        (entry) => String(entry.id) === String(values.teamId),
      )
      return createProject(team?.workspaceId ?? user?.workspaceId, values.teamId, values)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      navigate('/projects', { replace: true })
    },
    onError: (error) => {
      setFormError(getErrorMessage(error, 'Không thể tạo project.'))
    },
  })

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  if (teamOptions.length === 0) {
    return <Navigate to="/projects" replace />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.PROJECT_CREATE}>
      <div className="page page--wide">
        <ProjectFormModal
          title="Tạo project mới"
          description="Team Leader tạo project trong team mình quản lý."
          teams={teamOptions}
          initialTeamId={teamOptions[0]?.id ?? ''}
          onClose={() => navigate('/projects')}
          onSave={(values) => createMutation.mutate(values)}
          isSaving={createMutation.isPending}
          saveError={formError}
        />
      </div>
    </PermissionRoute>
  )
}
