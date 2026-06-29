import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchAssignableTeams,
  fetchOrganizationMembers,
  updateMemberOrganization,
} from '../../api/organizationMembersApi'
import { fetchTeams } from '../../api/teamsApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import Toast from '../../components/common/Toast'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import { useIsWorkspaceOwner } from '../../hooks/useIsWorkspaceOwner'
import { useWorkspaceScope } from '../../hooks/useWorkspaceScope'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { buildMemberSummary } from '../../utils/memberMappers'
import { attachTeamAssignmentsToMembers } from '../teams/utils/buildAssignmentCandidates'
import EditMemberOrganizationModal from './components/EditMemberOrganizationModal'
import MemberDetailDrawer from './components/MemberDetailDrawer'
import MemberFilters, { FILTER_ALL } from './components/MemberFilters'
import MemberStatsCards from './components/MemberStatsCards'
import MemberTable from './components/MemberTable'
import {
  filterMembers,
  hasActiveMemberFilters,
} from './utils/filterMembers'

const INITIAL_FILTERS = {
  search: '',
  teamId: FILTER_ALL,
  status: FILTER_ALL,
  availability: FILTER_ALL,
}

const DEFAULT_PAGE_SIZE = 10

export default function MembersListPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const isWorkspaceOwner = useIsWorkspaceOwner()
  const { workspaceId } = useWorkspaceScope()
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)
  const [selectedMember, setSelectedMember] = useState(null)
  const [editingMember, setEditingMember] = useState(null)
  const [actionError, setActionError] = useState('')
  const [toastMessage, setToastMessage] = useState('')

  const membersQueryKey = ['organization', 'members', workspaceId]
  const summaryQueryKey = ['organization', 'members', 'summary', workspaceId]

  const { data: members = [], isLoading: membersLoading } = useQuery({
    queryKey: membersQueryKey,
    queryFn: () => fetchOrganizationMembers(workspaceId),
  })

  const { data: teams = [] } = useQuery({
    queryKey: ['organization', 'assignable-teams', workspaceId],
    queryFn: () => fetchAssignableTeams(workspaceId),
  })

  const { data: summary } = useQuery({
    queryKey: summaryQueryKey,
    queryFn: () => fetchOrganizationMemberSummary(workspaceId),
  })

  const filteredMembers = useMemo(
    () => filterMembers(members, filters),
    [members, filters],
  )

  const totalPages = Math.max(1, Math.ceil(filteredMembers.length / pageSize))
  const safePage = Math.min(page, totalPages)

  const paginatedMembers = useMemo(() => {
    const start = (safePage - 1) * pageSize
    return filteredMembers.slice(start, start + pageSize)
  }, [filteredMembers, safePage, pageSize])

  useEffect(() => {
    setPage(1)
  }, [filters, pageSize])

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages)
    }
  }, [page, totalPages])

  const filtersActive = hasActiveMemberFilters(filters)

  const updateMutation = useMutation({
    mutationFn: ({ memberId, payload }) =>
      updateMemberOrganization(memberId, payload),
    onSuccess: () => {
      setEditingMember(null)
      setActionError('')
      setToastMessage('Thông tin tổ chức của thành viên đã được cập nhật.')
      queryClient.invalidateQueries({ queryKey: membersQueryKey })
      queryClient.invalidateQueries({ queryKey: summaryQueryKey })
      queryClient.invalidateQueries({ queryKey: ['organization', 'members'] })
    },
    onError: (error) => {
      setActionError(
        getErrorMessage(error, 'Không thể cập nhật thông tin tổ chức.'),
      )
    },
  })

  const handleSaveMember = (memberId, values) => {
    setActionError('')
    updateMutation.mutate({
      memberId,
      payload: values,
    })
  }

  if (membersLoading) {
    return <LoadingScreen />
  }

  return (
    <PermissionRoute permission={PERMISSIONS.MEMBER_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page page--wide members-page">
        <header className="members-page__toolbar">
          <div className="members-page__intro">
            <h1 className="members-page__title">
              {isWorkspaceOwner ? 'Quản lý thành viên' : 'Thành viên tổ chức'}
            </h1>
            <p className="members-page__subtitle">
              {isWorkspaceOwner
                ? 'Theo dõi thành viên workspace, phân bổ phòng ban và trạng thái tham gia.'
                : 'Theo dõi và cập nhật Team / Department, trạng thái tham gia tổ chức và thông tin phân bổ nguồn lực trong workspace.'}
            </p>
          </div>
        </header>

        <MemberStatsCards summary={summary} />

        <MemberFilters
          filters={filters}
          onChange={setFilters}
          resultCount={filteredMembers.length}
          teams={teams}
        />

        {members.length === 0 ? (
          <div className="members-empty">
            <p className="members-empty__title">Chưa có thành viên</p>
            <p className="members-empty__text">
              Thành viên sẽ hiển thị sau khi được thêm vào workspace.
            </p>
          </div>
        ) : filteredMembers.length === 0 ? (
          <div className="members-empty">
            <p className="members-empty__title">Không có kết quả</p>
            <p className="members-empty__text">
              {filtersActive
                ? 'Thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.'
                : 'Không tìm thấy thành viên phù hợp.'}
            </p>
          </div>
        ) : (
          <MemberTable
            members={paginatedMembers}
            selectedMemberId={selectedMember?.id}
            onSelect={setSelectedMember}
            onEdit={setEditingMember}
            page={safePage}
            pageSize={pageSize}
            totalCount={filteredMembers.length}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        )}

        {selectedMember && (
          <MemberDetailDrawer
            memberId={selectedMember.id}
            onClose={() => setSelectedMember(null)}
          />
        )}

        {editingMember && (
          <EditMemberOrganizationModal
            member={editingMember}
            teams={teams}
            onClose={() => {
              setEditingMember(null)
              setActionError('')
            }}
            onSave={handleSaveMember}
            isSaving={updateMutation.isPending}
            saveError={actionError}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
