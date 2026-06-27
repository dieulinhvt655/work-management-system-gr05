import { z } from 'zod'
import { WORKSPACE_STATUS } from '../../../constants/workspaces'

export const createWorkspaceSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên Workspace'),
  code: z
    .string()
    .min(1, 'Vui lòng nhập mã định danh Workspace')
    .regex(
      /^[a-z0-9-]+$/i,
      'Chỉ chứa chữ cái, số và dấu gạch ngang',
    ),
  logoUrl: z.string().optional(),
  description: z.string().optional(),
  contactEmail: z
    .string()
    .min(1, 'Vui lòng nhập email liên hệ')
    .email('Email không hợp lệ'),
  contactPhone: z.string().optional(),
  address: z.string().optional(),
  ownerId: z.string().min(1, 'Vui lòng chọn Workspace Owner'),
  status: z.enum([WORKSPACE_STATUS.ACTIVE, WORKSPACE_STATUS.INACTIVE], {
    errorMap: () => ({ message: 'Vui lòng chọn trạng thái ban đầu' }),
  }),
})

export const editWorkspaceSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên Workspace'),
  code: z
    .string()
    .min(1, 'Vui lòng nhập mã định danh Workspace')
    .regex(
      /^[a-z0-9-]+$/i,
      'Chỉ chứa chữ cái, số và dấu gạch ngang',
    ),
  logoUrl: z.string().optional(),
  description: z.string().optional(),
  contactEmail: z
    .string()
    .min(1, 'Vui lòng nhập email liên hệ')
    .email('Email không hợp lệ'),
  contactPhone: z.string().optional(),
  address: z.string().optional(),
  ownerId: z.string().min(1, 'Vui lòng chọn Workspace Owner'),
})
