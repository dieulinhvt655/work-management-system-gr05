import { z } from 'zod'

export const editTeamSchema = z.object({
  name: z.string().trim().min(1, 'Vui lòng nhập tên phòng ban / nhóm'),
  description: z.string().optional(),
})
