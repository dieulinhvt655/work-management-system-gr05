import {
  getDepartmentName,
  getMockUserActivities,
  getMockUserStatusHistory,
  MOCK_DEPARTMENTS,
  MOCK_USERS_GROUPED_BY_WORKSPACE,
} from '../constants/mock/usersData'
import { USER_ACCOUNT_STATUS } from '../constants/users'

function cloneGroupedUsers() {
  return MOCK_USERS_GROUPED_BY_WORKSPACE.map((group) => ({
    ...group,
    users: group.users.map((u) => ({ ...u })),
  }))
}

let groupedUsers = cloneGroupedUsers()

function delay(ms = 180) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function findUserEntry(userId) {
  for (const group of groupedUsers) {
    const index = group.users.findIndex((u) => u.id === userId)
    if (index >= 0) {
      return { group, index, user: group.users[index] }
    }
  }
  return null
}

export async function mockFetchDepartments() {
  await delay()
  return MOCK_DEPARTMENTS.map((dept) => ({ ...dept }))
}

export async function mockFetchUsersGroupedByWorkspace() {
  await delay()
  return groupedUsers.map((group) => ({
    ...group,
    users: group.users.map((u) => ({ ...u })),
  }))
}

export async function mockFetchUserById(userId) {
  await delay()
  const entry = findUserEntry(userId)
  if (!entry) {
    throw new Error('Không tìm thấy người dùng')
  }

  const { group, user } = entry
  return {
    ...user,
    workspaceId: group.workspaceId,
    workspaceName: group.workspaceName,
    workspaceCode: group.workspaceCode,
    activities: getMockUserActivities(userId),
    statusHistory: getMockUserStatusHistory(userId),
  }
}

export async function mockUpdateUser(userId, payload) {
  await delay()
  const entry = findUserEntry(userId)
  if (!entry) {
    throw new Error('Không tìm thấy người dùng')
  }

  const next = {
    ...entry.user,
    ...payload,
    departmentName: getDepartmentName(
      payload.departmentId ?? entry.user.departmentId,
    ),
  }

  entry.group.users[entry.index] = next
  return { ...next }
}

export async function mockUpdateUserRole(userId, role) {
  await delay()
  const entry = findUserEntry(userId)
  if (!entry) {
    throw new Error('Không tìm thấy người dùng')
  }

  entry.group.users[entry.index] = { ...entry.user, role }
  return { ...entry.group.users[entry.index] }
}

export async function mockUpdateUserStatus(userId, status) {
  await delay()
  const entry = findUserEntry(userId)
  if (!entry) {
    throw new Error('Không tìm thấy người dùng')
  }

  entry.group.users[entry.index] = { ...entry.user, status }
  return { ...entry.group.users[entry.index] }
}

function findUserByEmail(email) {
  const normalized = email.trim().toLowerCase()
  for (const group of groupedUsers) {
    const user = group.users.find(
      (entry) => entry.email.trim().toLowerCase() === normalized,
    )
    if (user) return user
  }
  return null
}

function findUserByEmployeeCode(employeeCode) {
  const normalized = employeeCode.trim().toLowerCase()
  for (const group of groupedUsers) {
    const user = group.users.find(
      (entry) => entry.employeeCode.trim().toLowerCase() === normalized,
    )
    if (user) return user
  }
  return null
}

function createUserId() {
  return `user-${Date.now()}`
}

export async function mockCreateUser(payload) {
  await delay()

  if (findUserByEmail(payload.email)) {
    throw new Error('Email công việc đã được sử dụng')
  }

  if (findUserByEmployeeCode(payload.employeeCode)) {
    throw new Error('Mã nhân viên đã tồn tại')
  }

  const group = groupedUsers.find(
    (entry) => entry.workspaceId === payload.workspaceId,
  )

  if (!group) {
    throw new Error('Không tìm thấy workspace')
  }

  const nextUser = {
    id: createUserId(),
    fullName: payload.fullName.trim(),
    email: payload.email.trim(),
    employeeCode: payload.employeeCode.trim(),
    departmentId: payload.departmentId,
    departmentName: getDepartmentName(payload.departmentId),
    position: payload.position.trim(),
    role: payload.role,
    status: payload.status ?? USER_ACCOUNT_STATUS.ACTIVE,
    phone: payload.phone?.trim() || null,
    createdAt: new Date().toISOString(),
    lastLoginAt: null,
    lastActivityAt: null,
  }

  group.users.push(nextUser)
  return { ...nextUser, workspaceId: group.workspaceId }
}

/** Reset store — useful for dev/hot reload testing */
export function resetMockUsersStore() {
  groupedUsers = cloneGroupedUsers()
}
