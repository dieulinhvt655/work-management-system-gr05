import { USER_ACCOUNT_STATUS } from '../constants/users'

/** Map backend user status → frontend account status. */
export function mapBackendStatusToFrontend(status) {
  const value = String(status ?? '').trim().toUpperCase()
  if (value === USER_ACCOUNT_STATUS.ACTIVE) return USER_ACCOUNT_STATUS.ACTIVE
  if (value === USER_ACCOUNT_STATUS.DELETED) return USER_ACCOUNT_STATUS.DELETED
  return USER_ACCOUNT_STATUS.INACTIVE
}

/** Map frontend account status → backend user status. */
export function mapFrontendStatusToBackend(status) {
  const value = String(status ?? '').trim().toUpperCase()
  if (value === USER_ACCOUNT_STATUS.ACTIVE) return USER_ACCOUNT_STATUS.ACTIVE
  if (value === USER_ACCOUNT_STATUS.DELETED) return USER_ACCOUNT_STATUS.DELETED
  return USER_ACCOUNT_STATUS.INACTIVE
}

/** Map backend role name → frontend role key (e.g. SYSTEM_ADMIN). */
export function mapBackendRoleToFrontendKey(role) {
  if (!role) return null

  const roleName = typeof role === 'string' ? role : role.name
  return roleName
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
export function mapUserToProfile(apiUser, organization = null) {
  const user = mapUserResponse(apiUser)
  const teamName = organization?.teamName ?? 'Chưa phân phòng ban'

  return {
    ...user,
    accountStatus: user.status,
    roleLabel: user.roleName ?? user.role ?? '—',
    bio: apiUser.bio ?? apiUser.description ?? '',
    workspaceId: organization?.workspaceId ?? user.workspaceId,
    workspaceName: organization?.workspaceName ?? '—',
    departmentName: organization?.departmentName ?? teamName,
    teamId: organization?.teamId ?? null,
    teamName,
    workspaceMemberId: organization?.workspaceMemberId ?? null,
    position: '—',
  }
}
