import {
  loginApi,
  fetchCurrentUserApi,
  requestPasswordReset as requestPasswordResetApi,
  resetPasswordApi,
} from './authApi'
import api from './axios'
import { getRefreshToken } from './axios'
import { MOCK_ACCESS_TOKEN, USE_MOCK_AUTH } from '../constants/config'
import { AUTH_USER_KEY } from '../constants/auth'
import { mockLogin } from './mockAuth'

export async function login({ email, password, mockRole }) {
  if (USE_MOCK_AUTH) {
    return {
      accessToken: MOCK_ACCESS_TOKEN,
      refreshToken: 'mock-refresh-token',
      user: mockLogin({ mockRole, email: email || undefined }),
    }
  }

  return loginApi({ email, password })
}

export async function fetchCurrentUser() {
  if (USE_MOCK_AUTH) {
    const stored = localStorage.getItem(AUTH_USER_KEY)
    return stored ? JSON.parse(stored) : null
  }

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

/** Luôn gọi API thật — không dùng mock. */
export async function requestPasswordReset(email) {
  return requestPasswordResetApi(email)
}

/** Luôn gọi API thật — không dùng mock. */
export async function resetPassword({ token, newPassword }) {
  return resetPasswordApi({ token, newPassword })
}
