import {
  Ban,
  Eye,
  Pencil,
} from 'lucide-react'
import IconButton from '../../../../components/ui/IconButton'
import PermissionGate from '../../../../components/common/PermissionGate'
import { PERMISSIONS } from '../../../../constants/permissions'
import { WORKSPACE_STATUS } from '../../../../constants/workspaces'
import { formatWorkspaceDate } from '../utils/formatWorkspaceDate'
import WorkspaceLogo from './WorkspaceLogo'
import WorkspaceStatusBadge from './WorkspaceStatusBadge'

export default function WorkspaceTable({
  workspaces,
  onView,
  onEdit,
  onDisable,
}) {
  return (
    <div className="workspace-table-wrap">
      <table className="workspace-table">
        <colgroup>
          <col className="workspace-table__col-logo" />
          <col className="workspace-table__col-name" />
          <col className="workspace-table__col-code" />
          <col className="workspace-table__col-owner" />
          <col className="workspace-table__col-email" />
          <col className="workspace-table__col-depts" />
          <col className="workspace-table__col-members" />
          <col className="workspace-table__col-status" />
          <col className="workspace-table__col-created" />
          <col className="workspace-table__col-actions" />
        </colgroup>
        <thead>
          <tr>
            <th scope="col">
              <span className="sr-only">Logo</span>
            </th>
            <th scope="col">Tên Workspace</th>
            <th scope="col">Mã Workspace</th>
            <th scope="col">Workspace Owner</th>
            <th scope="col">Email liên hệ</th>
            <th scope="col">Phòng ban / Nhóm</th>
            <th scope="col">Thành viên</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Ngày tạo</th>
            <th scope="col" className="workspace-table__actions-col">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {workspaces.map((workspace) => {
            const isDisabled = workspace.status === WORKSPACE_STATUS.DISABLED

            return (
              <tr
                key={workspace.id}
                className="workspace-table__row--clickable"
                onClick={() => onView(workspace)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    onView(workspace)
                  }
                }}
                tabIndex={0}
                role="link"
                aria-label={`Xem chi tiết ${workspace.name}`}
              >
                <td>
                  <WorkspaceLogo
                    name={workspace.name}
                    logoUrl={workspace.logoUrl}
                    size="sm"
                    className="workspace-logo--table"
                  />
                </td>
                <td className="workspace-table__name workspace-table__truncate" title={workspace.name}>
                  {workspace.name}
                </td>
                <td>
                  <code className="workspace-table__code">{workspace.code}</code>
                </td>
                <td className="workspace-table__truncate" title={workspace.ownerName}>
                  {workspace.ownerName}
                </td>
                <td className="workspace-table__truncate" title={workspace.contactEmail}>
                  {workspace.contactEmail}
                </td>
                <td>{workspace.departmentCount}</td>
                <td>{workspace.memberCount}</td>
                <td>
                  <WorkspaceStatusBadge status={workspace.status} />
                </td>
                <td className="workspace-table__muted workspace-table__truncate">
                  {formatWorkspaceDate(workspace.createdAt)}
                </td>
                <td
                  className="workspace-table__actions-cell"
                  onClick={(event) => event.stopPropagation()}
                  onKeyDown={(event) => event.stopPropagation()}
                >
                  <div className="workspace-table__actions">
                    <IconButton
                      label="Xem chi tiết"
                      onClick={() => onView(workspace)}
                    >
                      <Eye size={15} aria-hidden="true" />
                    </IconButton>

                    <PermissionGate permission={PERMISSIONS.WORKSPACE_ADMIN_MANAGE}>
                      <IconButton
                        label="Cập nhật"
                        onClick={() => onEdit(workspace)}
                      >
                        <Pencil size={15} aria-hidden="true" />
                      </IconButton>

                      <IconButton
                        label={isDisabled ? 'Workspace đã vô hiệu hóa' : 'Vô hiệu hóa Workspace'}
                        variant="lock"
                        disabled={isDisabled}
                        onClick={() => onDisable(workspace)}
                      >
                        <Ban size={15} aria-hidden="true" />
                      </IconButton>
                    </PermissionGate>
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
