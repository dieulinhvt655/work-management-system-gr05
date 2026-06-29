import { mapUserToProfile } from '../utils/userMappers'
import { unwrapApiResponse } from './apiResponse'
import api from './axios'

export async function fetchProfile() {
  const { data } = await api.get('/users/me')
  return mapUserToProfile(unwrapApiResponse({ data }))
}

export async function updateProfile(payload) {
  const current = payload.fullName ? payload : await fetchProfile()

  const { data } = await api.patch('/users/me', {
    fullName: current.fullName,
    phone: payload.phone?.trim() || null,
  })

  return mapUserToProfile(unwrapApiResponse({ data }))
}

export async function changePassword({ currentPassword, newPassword }) {
  const profile = await fetchProfile()

  const { data } = await api.patch('/users/me', {
    fullName: profile.fullName,
    currentPassword,
    newPassword,
  })

  return unwrapApiResponse({ data })
}

export async function fetchProfileHistory() {
  return []
}
