import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api, { clearAuthStorage, setAuthTokens } from '../api/axios'
import { isMockSession, mockLogin } from '../api/mockAuth'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/auth'
import { MOCK_ACCESS_TOKEN, USE_MOCK_AUTH } from '../constants/config'
import { createMockUser } from '../constants/mock/rolePermissions'
import { MOCK_ROLES } from '../constants/roles'

const AuthContext = createContext(null)

function unwrapResponse(response) {
  return response.data?.data ?? response.data
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem(AUTH_USER_KEY)
    return stored ? JSON.parse(stored) : null
  })
  const [isLoading, setIsLoading] = useState(true)

  const persistUser = useCallback((nextUser) => {
    setUser(nextUser)
    if (nextUser) {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser))
    } else {
      localStorage.removeItem(AUTH_USER_KEY)
    }
  }, [])

  const fetchCurrentUser = useCallback(async () => {
    const { data } = await api.get('/auth/me')
    const currentUser = unwrapResponse({ data })
    persistUser(currentUser)
    return currentUser
  }, [persistUser])

  useEffect(() => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)

    if (!token) {
      setIsLoading(false)
      return
    }

    if (isMockSession(token)) {
      setIsLoading(false)
      return
    }

    fetchCurrentUser()
      .catch(() => {
        clearAuthStorage()
        persistUser(null)
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [fetchCurrentUser, persistUser])

  const login = useCallback(
    async ({ email, password, mockRole }) => {
      if (USE_MOCK_AUTH) {
        const mockUser = mockLogin({
          mockRole: mockRole ?? MOCK_ROLES.TEAM_MEMBER,
          email: email || undefined,
        })
        setAuthTokens({
          accessToken: MOCK_ACCESS_TOKEN,
          refreshToken: 'mock-refresh-token',
        })
        persistUser(mockUser)
        return mockUser
      }

      const { data } = await api.post('/auth/login', { email, password })
      const payload = unwrapResponse({ data })

      setAuthTokens({
        accessToken: payload.accessToken ?? payload.token,
        refreshToken: payload.refreshToken,
      })

      if (payload.user) {
        persistUser(payload.user)
        return payload.user
      }

      return fetchCurrentUser()
    },
    [fetchCurrentUser, persistUser],
  )

  const switchMockRole = useCallback(
    (mockRole) => {
      if (!USE_MOCK_AUTH) return

      const nextUser = createMockUser(mockRole, user?.email)
      setAuthTokens({
        accessToken: MOCK_ACCESS_TOKEN,
        refreshToken: 'mock-refresh-token',
      })
      persistUser(nextUser)
      return nextUser
    },
    [persistUser, user?.email],
  )

  const logout = useCallback(async () => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)

    if (!isMockSession(token)) {
      try {
        await api.post('/auth/logout')
      } catch {
        // Ignore logout errors and clear local session anyway.
      }
    }

    clearAuthStorage()
    persistUser(null)
  }, [persistUser])

  const value = useMemo(
    () => ({
      user,
      permissions: user?.permissions ?? [],
      isAuthenticated: Boolean(user),
      isLoading,
      login,
      logout,
      switchMockRole,
      isMockAuthEnabled: USE_MOCK_AUTH,
    }),
    [user, isLoading, login, logout, switchMockRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
