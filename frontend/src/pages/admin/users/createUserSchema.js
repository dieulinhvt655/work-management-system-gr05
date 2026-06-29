import { z } from 'zod'
import { USER_ACCOUNT_STATUS } from '../../../constants/users'

export const createUserSchema = z.object({
  fullName: z.string().min(1, 'Vui lòng nhập họ tên'),
  email: z
    .string()
    .min(1, 'Vui lòng nhập email')
    .email('Email không hợp lệ'),
  username: z
    .string()
    .min(3, 'Username phải có ít nhất 3 ký tự')
    .max(100, 'Username tối đa 100 ký tự'),
  phone: z.string().optional(),
  role: z.string().min(1, 'Vui lòng chọn vai trò hệ thống'),
  status: z.enum([USER_ACCOUNT_STATUS.ACTIVE, USER_ACCOUNT_STATUS.INACTIVE], {
    errorMap: () => ({ message: 'Vui lòng chọn trạng thái tài khoản' }),
  }),
})
