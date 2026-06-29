import { z } from 'zod'

export const editUserSchema = z.object({
  fullName: z.string().min(1, 'Vui lòng nhập họ tên'),
  email: z
    .string()
    .min(1, 'Vui lòng nhập email')
    .email('Email không hợp lệ'),
  username: z
    .string()
    .min(3, 'Username phải có ít nhất 3 ký tự')
    .max(100, 'Username tối đa 100 ký tự'),
  departmentId: z.string().optional(),
  phone: z.string().optional(),
})
