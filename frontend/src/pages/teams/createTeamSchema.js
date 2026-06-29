import { z } from 'zod'
import { TEAM_STATUS } from '../../constants/teams'

export const createTeamSchema = z.object({
  name: z.string().trim().min(1, 'Vui lòng nhập tên phòng ban / team'),
  description: z.string().optional(),
  status: z.enum([TEAM_STATUS.ACTIVE, TEAM_STATUS.INACTIVE], {
    errorMap: () => ({ message: 'Vui lòng chọn trạng thái ban đầu' }),
  }),
  workspaceId: z.string().optional(),
})
