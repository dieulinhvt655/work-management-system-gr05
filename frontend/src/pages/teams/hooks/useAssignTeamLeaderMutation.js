import { useMutation } from '@tanstack/react-query'
import { resolveRoleIdByKey } from '../../../api/rolesApi'
import { assignTeamLeader, updateTeamMember } from '../../../api/teamsApi'
import { getErrorMessage } from '../../../utils/getErrorMessage'

export function useAssignTeamLeaderMutation({ onSuccess, onError } = {}) {
  return useMutation({
    mutationFn: async ({ team, payload }) => {
      const teamLeaderRoleId = await resolveRoleIdByKey('TEAM_LEADER')
      const member = team.members.find((entry) => entry.id === payload.teamMemberId)

      if (!member) {
        throw new Error('Không tìm thấy thành viên trong phòng ban.')
      }

      if (Number(member.roleId) !== Number(teamLeaderRoleId)) {
        await updateTeamMember(team.workspaceId, team.id, member.id, {
          roleId: teamLeaderRoleId,
        })
      }

      return assignTeamLeader(team.workspaceId, team.id, member.id)
    },
    onSuccess,
    onError: (error) => {
      onError?.(getErrorMessage(error, 'Không thể gán Team Leader.'))
    },
  })
}
