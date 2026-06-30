import { useQuery } from '@tanstack/react-query'
import { fetchAccessibleWorkspaceInfo } from '../../api/workspacesApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PERMISSIONS } from '../../constants/permissions'
import { WORKSPACE_STATUS_LABELS } from '../../constants/workspaces'
import { useAuth } from '../../context/AuthContext'
import { isTeamLeaderUser } from '../../utils/userRoleUtils'
import PermissionRoute from '../../routes/PermissionRoute'
import { formatLastActivity } from '../admin/users/utils/formatUserDate'

function InfoRow({ label, children }) {
  return (
    <div className="workspace-info-row">
      <dt className="workspace-info-row__label">{label}</dt>
      <dd className="workspace-info-row__value">{children}</dd>
    </div>
  )
}

export default function WorkspaceInfoPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const workspaceId = user?.workspaceId
  const isTeamLeader = isTeamLeaderUser(user)

  const { data: workspace, isLoading } = useQuery({
    queryKey: ['workspace', workspaceId, user?.teamName, isTeamLeader],
    queryFn: () =>
      fetchAccessibleWorkspaceInfo(workspaceId, {
        teamName: user?.teamName,
      }),
    enabled: isAuthenticated && !authLoading && Boolean(workspaceId),
  })

  if (authLoading || isLoading) {
    return <LoadingScreen />
  }

  if (!workspace) {
    return (
      <PermissionRoute permission={PERMISSIONS.WORKSPACE_READ}>
        <div className="page workspace-info-page">
          <p>Chưa có workspace nào được gán cho tài khoản này.</p>
        </div>
      </PermissionRoute>
    )
  }

  return (
    <PermissionRoute permission={PERMISSIONS.WORKSPACE_READ}>
      <div className="page page--wide workspace-info-page">
        <header className="workspace-info-page__header">
          <p className="workspace-info-page__eyebrow">Workspace</p>
          <h1>{workspace.name}</h1>
          <p className="workspace-info-page__subtitle">
            {isTeamLeader
              ? 'Thông tin workspace nơi bạn đang làm việc.'
              : 'Thông tin workspace bạn đang quản lý.'}
          </p>
        </header>

        <section className="workspace-info-page__card">
          <dl className="workspace-info-page__list">
            <InfoRow label="Tên workspace">{workspace.name}</InfoRow>
            <InfoRow label="Mô tả">{workspace.description || '—'}</InfoRow>
            <InfoRow label="Trạng thái">
              {WORKSPACE_STATUS_LABELS[workspace.status] ?? workspace.status}
            </InfoRow>
            {isTeamLeader ? (
              <InfoRow label="Phòng ban của bạn">
                {user?.teamName && user.teamName !== 'Chưa phân phòng ban'
                  ? user.teamName
                  : '—'}
              </InfoRow>
            ) : (
              <InfoRow label="Workspace Owner">{workspace.ownerName ?? '—'}</InfoRow>
            )}
            <InfoRow label="Số thành viên">{workspace.memberCount ?? 0}</InfoRow>
            <InfoRow label="Ngày tạo">
              {formatLastActivity(workspace.createdAt)}
            </InfoRow>
          </dl>
        </section>
      </div>
    </PermissionRoute>
  )
}
