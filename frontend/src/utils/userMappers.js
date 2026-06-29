import { BACKEND_ROLE_TO_MOCK_ROLE } from './backendAuthMapper'
import { USER_ACCOUNT_STATUS } from '../constants/users'

const FRONTEND_STATUS_TO_BACKEND = {
  [USER_ACCOUNT_STATUS.ACTIVE]: 'ACTIVE',
  [USER_ACCOUNT_STATUS.INACTIVE]: 'INACTIVE',
  [USER_ACCOUNT_STATUS.LOCKED]: 'INACTIVE',
  [USER_ACCOUNT_STATUS.PENDING]: 'INACTIVE',
}

/** Map backend user status → frontend account status. */
export function mapBackendStatusToFrontend(status) {
  if (status === 'ACTIVE') return USER_ACCOUNT_STATUS.ACTIVE
  if (status === 'DELETED') return USER_ACCOUNT_STATUS.INACTIVE
  return USER_ACCOUNT_STATUS.LOCKED
}

/** Map frontend account status → backend user status. */
export function mapFrontendStatusToBackend(status) {
  return FRONTEND_STATUS_TO_BACKEND[status] ?? 'INACTIVE'
}

/** Map backend role name → frontend role key (e.g. SYSTEM_ADMIN). */
export function mapBackendRoleToFrontendKey(role) {
  if (!role) return null

  const roleName = typeof role === 'string' ? role : role.name
  return BACKEND_ROLE_TO_MOCK_ROLE[roleName] ?? roleName
}

/** Resolve system-generated employee code (Mã NV) from API user payload. */
export function resolveEmployeeCode(apiUser) {
  if (!apiUser) return ''

  const fromApi =
    apiUser.employeeCode?.trim() || apiUser.employee_code?.trim() || ''

  if (fromApi) return fromApi

  const username = apiUser.username?.trim() ?? ''
  if (/^NV\d+$/i.test(username)) {
    return username.toUpperCase()
  }

  if (apiUser.id != null) {
    return `NV${String(apiUser.id).padStart(3, '0')}`
  }

  return ''
}

/** Map backend UserResponse → frontend admin user model. */
export function mapUserResponse(apiUser) {
  if (!apiUser) return null

  const roleName = apiUser.role?.name ?? null

  return {
    id: String(apiUser.id),
    fullName: apiUser.fullName,
    email: apiUser.email,
    username: apiUser.username ?? '',
    employeeCode: resolveEmployeeCode(apiUser),
    phone: apiUser.phone ?? null,
    avatarUrl: apiUser.avatarUrl ?? null,
    status: mapBackendStatusToFrontend(apiUser.status),
    role: mapBackendRoleToFrontendKey(apiUser.role),
    roleId: apiUser.role?.id ?? null,
    roleName,
    roleScope: apiUser.role?.scope ?? null,
    departmentId: null,
    departmentName: null,
    workspaceId: null,
    workspaceName: null,
    position: null,
    createdAt: apiUser.createdAt ?? null,
    updatedAt: apiUser.updatedAt ?? null,
  }
}

/** Map backend UserResponse → frontend profile model. */
export function mapUserToProfile(apiUser) {
  const user = mapUserResponse(apiUser)

  return {
    ...user,
    bio: '',
    personalLink: '',
    workspaceName: '—',
    departmentName: '—',
    teamName: '—',
    position: '—',
  }
}
