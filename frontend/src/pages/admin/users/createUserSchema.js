import { z } from 'zod'
import { USER_ACCOUNT_STATUS } from '../../../constants/users'

export const createUserSchema = z.object({
  fullName: z.string().min(1, 'Vui lòng nhập họ tên'),
  email: z
    .string()
    .min(1, 'Vui lòng nhập email')
    .email('Email không hợp lệ'),
  employeeCode: z.string().min(1, 'Vui lòng nhập mã nhân viên'),
  phone: z.string().optional(),
  workspaceId: z.string().min(1, 'Vui lòng chọn workspace'),
  departmentId: z.string().min(1, 'Vui lòng chọn phòng ban / nhóm'),
  position: z.string().min(1, 'Vui lòng nhập chức vụ'),
  role: z.string().min(1, 'Vui lòng chọn vai trò ban đầu'),
  status: z.enum([USER_ACCOUNT_STATUS.ACTIVE, USER_ACCOUNT_STATUS.INACTIVE], {
    errorMap: () => ({ message: 'Vui lòng chọn trạng thái tài khoản' }),
  }),
})
