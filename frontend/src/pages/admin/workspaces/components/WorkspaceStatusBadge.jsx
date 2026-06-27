import { WORKSPACE_STATUS_LABELS } from '../../../../constants/workspaces'

export default function WorkspaceStatusBadge({ status }) {
  return (
    <span className={`workspace-status workspace-status--${status.toLowerCase()}`}>
      <span className="workspace-status__dot" aria-hidden="true" />
      {WORKSPACE_STATUS_LABELS[status] ?? status}
    </span>
  )
}
