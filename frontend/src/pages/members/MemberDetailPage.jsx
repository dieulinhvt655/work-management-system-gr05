import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Pencil } from 'lucide-react'
import { Link, Navigate, useParams } from 'react-router-dom'
import {
  fetchAssignableTeams,
  fetchOrganizationMemberById,
  updateMemberOrganization,
} from '../../api/organizationMembersApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import PermissionGate from '../../components/common/PermissionGate'
import Toast from '../../components/common/Toast'
import UserAvatar from '../../components/common/UserAvatar'
import Button from '../../components/ui/Button'
import { PERMISSIONS } from '../../constants/permissions'
import { USER_ROLE_LABELS } from '../../constants/users'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import EditMemberOrganizationModal from './components/EditMemberOrganizationModal'
import MemberOrgStatusBadge from './components/MemberOrgStatusBadge'
import MemberProjectsCard, {
  MemberOrganizationCard,
  MemberOrgHistoryCard,
} from './components/MemberDetailSections'

const memberQueryKey = (memberId) => ['organization', 'members', memberId]

export default function MemberDetailPage() {
  const { memberId } = useParams()
  const queryClient = useQueryClient()
  const [showEdit, setShowEdit] = useState(false)
  const [actionError, setActionError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  const {
    data: member,
    isLoading,
    isError,
  } = useQuery({
    queryKey: memberQueryKey(memberId),
    queryFn: () => fetchOrganizationMemberById(memberId),
  })

  const { data: teams = [] } = useQuery({
    queryKey: ['organization', 'assignable-teams', member?.workspaceId],
    queryFn: () => fetchAssignableTeams(member?.workspaceId),
    enabled: Boolean(member?.workspaceId),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => updateMemberOrganization(id, payload),
    onSuccess: () => {
      setShowEdit(false)
      setActionError('')
      setToastMessage('Thông tin tổ chức của thành viên đã được cập nhật.')
      queryClient.invalidateQueries({ queryKey: memberQueryKey(memberId) })
      queryClient.invalidateQueries({ queryKey: ['organization', 'members'] })
    },
    onError: (error) => {
      setActionError(
        getErrorMessage(error, 'Không thể cập nhật thông tin tổ chức.'),
      )
    },
  })

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isError || !member) {
    return <Navigate to="/members" replace />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.MEMBER_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide members-page member-detail-page">
        <header className="member-detail-page__header">
          <Link to="/members" className="member-detail-page__back">
            <ArrowLeft size={16} aria-hidden="true" />
            Quay lại danh sách
          </Link>

          <div className="member-detail-page__hero">
            <div className="member-detail-page__identity">
              <UserAvatar fullName={member.fullName} size="lg" />
              <div>
                <h1 className="member-detail-page__name">{member.fullName}</h1>
                <p className="member-detail-page__email">{member.email}</p>
                <div className="member-detail-page__meta">
                  <MemberOrgStatusBadge status={member.organizationStatus} />
                  <code className="member-table__code">{member.employeeCode}</code>
                  <span className="member-detail-page__role">
                    {USER_ROLE_LABELS[member.role] ?? member.role}
                  </span>
                </div>
              </div>
            </div>

            <PermissionGate permission={PERMISSIONS.MEMBER_MANAGE}>
              <Button
                type="button"
                variant="ghost"
                className="member-detail-page__edit-btn"
                onClick={() => setShowEdit(true)}
              >
                <Pencil size={16} aria-hidden="true" />
                Cập nhật tổ chức
              </Button>
            </PermissionGate>
          </div>
        </header>

        <div className="member-detail-page__grid">
          <div className="member-detail-page__column">
            <section className="member-detail-card">
              <header className="member-detail-card__head">
                <h2 className="member-detail-card__title">Thông tin định danh</h2>
              </header>
              <dl className="member-detail-dl">
                <div className="member-detail-dl__row">
                  <dt>Email</dt>
                  <dd>{member.email}</dd>
                </div>
                <div className="member-detail-dl__row">
                  <dt>Mã nhân viên</dt>
                  <dd>
                    <code>{member.employeeCode}</code>
                  </dd>
                </div>
                <div className="member-detail-dl__row">
                  <dt>Số điện thoại</dt>
                  <dd>{member.phone || '—'}</dd>
                </div>
                <div className="member-detail-dl__row">
                  <dt>Vai trò hệ thống</dt>
                  <dd>{USER_ROLE_LABELS[member.role] ?? member.role}</dd>
                </div>
              </dl>
            </section>

            <MemberOrganizationCard member={member} />
          </div>

          <div className="member-detail-page__column">
            <MemberProjectsCard member={member} />
            <MemberOrgHistoryCard history={member.organizationHistory} />
          </div>
        </div>

        {showEdit && (
          <EditMemberOrganizationModal
            member={member}
            teams={teams}
            onClose={() => {
              setShowEdit(false)
              setActionError('')
            }}
            onSave={(id, payload) => updateMutation.mutate({ id, payload })}
            isSaving={updateMutation.isPending}
            saveError={actionError}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
