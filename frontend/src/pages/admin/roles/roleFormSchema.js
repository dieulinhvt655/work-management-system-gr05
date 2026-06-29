import { z } from 'zod'
import { ROLE_SCOPE } from '../../../constants/roles'

export const roleFormSchema = z.object({
  name: z.string().trim().min(1, 'Vui lòng nhập tên vai trò'),
  description: z.string().optional(),
  scope: z.enum(
    [ROLE_SCOPE.SYSTEM, ROLE_SCOPE.WORKSPACE, ROLE_SCOPE.TEAM, ROLE_SCOPE.PROJECT],
    { errorMap: () => ({ message: 'Vui lòng chọn phạm vi vai trò' }) },
  ),
  permissionIds: z.array(z.number()).default([]),
})
