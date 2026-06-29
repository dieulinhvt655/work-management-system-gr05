import { z } from 'zod'

export const createWorkspaceSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên Workspace'),
  description: z.string().optional(),
})

export const editWorkspaceSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên Workspace'),
  description: z.string().optional(),
})
