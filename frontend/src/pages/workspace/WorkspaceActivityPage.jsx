import { useQuery } from '@tanstack/react-query'
import { fetchWorkspaceActivityLogs } from '../../api/workspacesApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'
import { formatLastActivity } from '../admin/users/utils/formatUserDate'

export default function WorkspaceActivityPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const workspaceId = user?.workspaceId

  const { data: payload, isLoading } = useQuery({
    queryKey: ['workspace', 'activity', workspaceId, 'full'],
    queryFn: () => fetchWorkspaceActivityLogs(workspaceId, { size: 50 }),
    enabled: isAuthenticated && !authLoading && Boolean(workspaceId),
  })

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  const activities = payload?.items ?? []

  return (
    <PermissionRoute permission={PERMISSIONS.WORKSPACE_ACTIVITY_READ}>
      <div className="page page--wide workspace-activity-page">
        <header className="workspace-activity-page__header">
          <h1>Workspace Activity</h1>
          <p>Lịch sử hoạt động trong workspace của bạn.</p>
        </header>

        <section className="workspace-activity-page__card">
          {activities.length === 0 ? (
            <p className="workspace-activity-page__empty">
              Chưa có hoạt động nào được ghi nhận.
            </p>
          ) : (
            <ul className="workspace-activity-page__list">
              {activities.map((activity) => (
                <li key={activity.id} className="workspace-activity-page__item">
                  <p className="workspace-activity-page__action">
                    {activity.action ?? activity.description ?? 'Hoạt động'}
                  </p>
                  <time dateTime={activity.createdAt}>
                    {formatLastActivity(activity.createdAt)}
                  </time>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </PermissionRoute>
  )
}
