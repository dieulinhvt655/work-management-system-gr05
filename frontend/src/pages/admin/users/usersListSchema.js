import { z } from 'zod'

export const editUserSchema = z.object({
  fullName: z.string().min(1, 'Vui lòng nhập họ tên'),
  email: z
    .string()
    .min(1, 'Vui lòng nhập email')
    .email('Email không hợp lệ'),
  employeeCode: z.string().min(1, 'Vui lòng nhập mã nhân viên'),
  departmentId: z.string().optional(),
  phone: z.string().optional(),
})
