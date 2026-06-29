import axios from 'axios'
import {
  AUTH_TOKEN_KEY,
  AUTH_REFRESH_TOKEN_KEY,
  AUTH_USER_KEY,
} from '../constants/auth'
import { API_BASE_URL, IS_NGROK_API } from '../constants/config'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: '*/*',
  },
})

export function getAccessToken() {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(AUTH_REFRESH_TOKEN_KEY)
}

function applyAuthHeader(config) {
  const token = getAccessToken()
  if (!token) {
    return config
  }

  if (!config.headers) {
    config.headers = {}
  }

  if (typeof config.headers.set === 'function') {
    config.headers.set('Authorization', `Bearer ${token}`)
  } else {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
}

/** Merge Authorization (+ ngrok header) into axios request config. */
export function getAuthRequestConfig(config = {}) {
  const token = getAccessToken()

  if (!token) {
    const error = new Error('Chưa đăng nhập hoặc token không hợp lệ')
    error.code = 'AUTH_MISSING_TOKEN'
    throw error
  }

  const headers = {
    ...(config.headers ?? {}),
    Authorization: `Bearer ${token}`,
  }

  if (IS_NGROK_API || config.baseURL?.includes('ngrok')) {
    headers['ngrok-skip-browser-warning'] = 'true'
  }

  return {
    ...config,
    headers,
  }
}

export function setAuthTokens({ accessToken, refreshToken }) {
  if (accessToken) {
    localStorage.setItem(AUTH_TOKEN_KEY, accessToken)
  }
  if (refreshToken) {
    localStorage.setItem(AUTH_REFRESH_TOKEN_KEY, refreshToken)
  }
}

export function clearAuthStorage() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_REFRESH_TOKEN_KEY)
  localStorage.removeItem(AUTH_USER_KEY)
}

api.interceptors.request.use((config) => {
  applyAuthHeader(config)

  if (IS_NGROK_API || config.baseURL?.includes('ngrok')) {
    if (typeof config.headers?.set === 'function') {
      config.headers.set('ngrok-skip-browser-warning', 'true')
    } else if (config.headers) {
      config.headers['ngrok-skip-browser-warning'] = 'true'
    }
  }

  return config
})

let isRefreshing = false
let failedQueue = []

function processQueue(error, token = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  failedQueue = []
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status

    if (status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      clearAuthStorage()
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      }).then((token) => {
        originalRequest.headers.Authorization = `Bearer ${token}`
        return api(originalRequest)
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      const { data } = await axios.post(
        `${API_BASE_URL}/auth/refresh`,
        { refreshToken },
        {
          headers: {
            'Content-Type': 'application/json',
            Accept: '*/*',
            ...(IS_NGROK_API ? { 'ngrok-skip-browser-warning': 'true' } : {}),
          },
        },
      )
      const payload = data?.data ?? data
      const accessToken = payload.accessToken ?? payload.token

      setAuthTokens({
        accessToken,
        refreshToken: payload.refreshToken ?? refreshToken,
      })

      processQueue(null, accessToken)
      originalRequest.headers.Authorization = `Bearer ${accessToken}`
      return api(originalRequest)
    } catch (refreshError) {
      processQueue(refreshError, null)
      clearAuthStorage()
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default api
