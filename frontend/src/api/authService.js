import {
  loginApi,
  fetchCurrentUserApi,
  requestPasswordReset as requestPasswordResetApi,
  resetPasswordApi,
} from './authApi'
import api from './axios'
import { getRefreshToken } from './axios'

export async function login({ email, password }) {
  return loginApi({ email, password })
}

export async function fetchCurrentUser() {
  return fetchCurrentUserApi()
}

export async function logoutApi() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return

  try {
    await api.post('/auth/logout', { refreshToken })
  } catch {
    // Ignore logout errors — client tokens are cleared regardless.
  }
}

export async function requestPasswordReset(email) {
  return requestPasswordResetApi(email)
}

export async function resetPassword({ token, newPassword }) {
  return resetPasswordApi({ token, newPassword })
}
