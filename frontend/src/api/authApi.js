import api, { setAuthTokens } from './axios'
import { unwrapApiResponse } from './apiResponse'
import {
  isSystemAdminRole,
  resolveFrontendPermissions,
  resolveWorkspaceScope,
} from '../utils/backendAuthMapper'
import { isWorkspaceOwnerUser } from '../utils/userRoleUtils'
import { resolveCurrentUserOrganization } from './userOrganizationApi'

async function enrichUserWorkspaceScope(user) {
  if (!user || user.isSystemAdmin) {
    return user
  }

  try {
    const organization = await resolveCurrentUserOrganization(user)

    if (!organization) {
      return user
    }

    return {
      ...user,
      workspaceId: organization.workspaceId ?? user.workspaceId ?? null,
      managedWorkspaceIds:
        organization.workspaceIds.length > 0
          ? organization.workspaceIds
          : user.managedWorkspaceIds ?? [],
      workspaceMemberId: organization.workspaceMemberId,
      teamId: organization.teamId,
      teamName: organization.teamName,
      departmentName: organization.departmentName,
      teamAssignments: organization.teamAssignments,
      isTeamLeader: organization.isTeamLeader,
      ledTeamIds: organization.ledTeamIds,
      ledTeamNames: organization.ledTeamNames,
    }
  } catch {
    return user
  }
}

/** Map backend login user → frontend session user. */
export function mapAuthUser(apiUser) {
  if (!apiUser) {
    return null
  }

  const roleName = apiUser.role?.name ?? apiUser.role
  const roleScope = apiUser.role?.scope
  const backendCodes =
    apiUser.role?.permissions?.map((permission) => permission.code).filter(Boolean) ??
    apiUser.permissions ??
    []

  const permissions = resolveFrontendPermissions(roleName, backendCodes)
  const workspaceScope = resolveWorkspaceScope(roleName, roleScope, apiUser)

  return {
    id: apiUser.id,
    fullName: apiUser.fullName,
    email: apiUser.email,
    username: apiUser.username,
    role: roleName,
    roleId: apiUser.role?.id,
    roleScope,
    roleDescription: apiUser.role?.description,
    isSystemAdmin: isSystemAdminRole(roleName, roleScope),
    isWorkspaceOwner: isWorkspaceOwnerUser({
      role: roleName,
      roleScope,
      isSystemAdmin: isSystemAdminRole(roleName, roleScope),
    }),
    permissions,
    backendPermissions: backendCodes,
    ...workspaceScope,
  }
}

export async function mapAuthUserWithScope(apiUser) {
  return enrichUserWorkspaceScope(mapAuthUser(apiUser))
}

export async function loginApi({ email, password }) {
  const { data } = await api.post('/auth/login', { email, password })
  const payload = unwrapApiResponse({ data })

  const accessToken = payload.accessToken ?? payload.token
  const refreshToken = payload.refreshToken

  // Lưu token trước khi gọi API cần auth (fetchWorkspaces trong enrichUserWorkspaceScope).
  setAuthTokens({ accessToken, refreshToken })

  return {
    accessToken,
    refreshToken,
    expiresIn: payload.expiresIn,
    tokenType: payload.tokenType,
    user: await mapAuthUserWithScope(payload.user),
  }
}

export async function fetchCurrentUserApi() {
  const { data } = await api.get('/users/me')
  const payload = unwrapApiResponse({ data })
  return mapAuthUserWithScope(payload)
}

export async function refreshTokenApi(refreshToken) {
  const { data } = await api.post('/auth/refresh', { refreshToken })
  const payload = unwrapApiResponse({ data })

  return {
    accessToken: payload.accessToken ?? payload.token,
    refreshToken: payload.refreshToken ?? refreshToken,
  }
}

export async function requestPasswordReset(email) {
  const { data } = await api.post('/auth/forgot-password', {
    email: email.trim(),
  })

  const body = data

  if (body?.success === false) {
    const error = new Error(body.message || 'Yêu cầu thất bại')
    error.response = { data: body }
    error.errorCode = body.errorCode
    throw error
  }

  return {
    data: body?.data ?? null,
    message:
      body?.message ??
      'Chúng tôi đã gửi link đặt lại mật khẩu đến email của bạn.',
  }
}

export async function resetPasswordApi({ token, newPassword }) {
  const { data } = await api.post('/auth/reset-password', {
    token,
    newPassword,
  })

  const body = data

  if (body?.success === false) {
    const error = new Error(body.message || 'Yêu cầu thất bại')
    error.response = { data: body }
    error.errorCode = body.errorCode
    throw error
  }

  return {
    data: body?.data ?? null,
    message: body?.message ?? 'Mật khẩu đã được cập nhật thành công.',
  }
}
