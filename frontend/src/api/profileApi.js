import { isSystemAdminRole } from '../utils/backendAuthMapper'
import { mapUserToProfile } from '../utils/userMappers'
import { unwrapApiResponse } from './apiResponse'
import api from './axios'
import { resolveCurrentUserOrganization } from './userOrganizationApi'

async function enrichProfile(apiUser) {
  const roleName = apiUser.role?.name ?? apiUser.role
  const roleScope = apiUser.role?.scope
  const isSystemAdmin = isSystemAdminRole(roleName, roleScope)

  if (isSystemAdmin) {
    return mapUserToProfile(apiUser)
  }

  const organization = await resolveCurrentUserOrganization({
    id: apiUser.id,
    isSystemAdmin,
  })

  return mapUserToProfile(apiUser, organization)
}

export async function fetchProfile() {
  const { data } = await api.get('/users/me')
  return enrichProfile(unwrapApiResponse({ data }))
}

export async function updateProfile(payload) {
  const current = payload.fullName ? payload : await fetchProfile()

  const body = {
    fullName: current.fullName,
    phone: payload.phone?.trim() || null,
  }

  if (payload.avatarUrl !== undefined) {
    body.avatarUrl = payload.avatarUrl || null
  }

  if (payload.bio !== undefined) {
    body.description = payload.bio.trim() || null
  }

  const { data } = await api.patch('/users/me', body)
  const apiUser = unwrapApiResponse({ data })

  return enrichProfile({
    ...apiUser,
    phone: apiUser.phone ?? payload.phone ?? null,
    avatarUrl: apiUser.avatarUrl ?? payload.avatarUrl ?? null,
    description: apiUser.description ?? payload.bio ?? '',
  })
}

export async function changePassword({
  currentPassword,
  newPassword,
  confirmPassword,
}) {
  const profile = await fetchProfile()

  const { data } = await api.patch('/users/me', {
    fullName: profile.fullName,
    currentPassword,
    newPassword,
    confirmNewPassword: confirmPassword ?? newPassword,
  })

  return unwrapApiResponse({ data })
}

export async function fetchProfileHistory() {
  return []
}
