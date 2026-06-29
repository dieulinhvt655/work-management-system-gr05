import api from './axios'

/** UC-1.3 — Gọi API yêu cầu khôi phục mật khẩu */
export async function requestPasswordReset(email) {
  const { data } = await api.post('/auth/forgot-password', { email })
  return data?.data ?? data
}
