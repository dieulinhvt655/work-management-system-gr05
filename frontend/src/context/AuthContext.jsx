import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { clearAuthStorage, setAuthTokens } from '../api/axios'
import { clearRolesCache } from '../api/rolesApi'
import { login as loginRequest, fetchCurrentUser, logoutApi } from '../api/authService'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/auth'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem(AUTH_USER_KEY)
    return stored ? JSON.parse(stored) : null
  })
  const [isLoading, setIsLoading] = useState(() => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)
    if (!token) return false
    // Có token + user cache → không chặn UI chờ refresh session.
    return !localStorage.getItem(AUTH_USER_KEY)
  })

  const persistUser = useCallback((nextUser) => {
    setUser(nextUser)
    if (nextUser) {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser))
    } else {
      localStorage.removeItem(AUTH_USER_KEY)
    }
  }, [])

  const restoreSession = useCallback(async () => {
    try {
      const currentUser = await fetchCurrentUser()
      if (currentUser) {
        persistUser(currentUser)
      }
      return currentUser
    } catch {
      const stored = localStorage.getItem(AUTH_USER_KEY)
      if (stored) {
        return JSON.parse(stored)
      }
      throw new Error('Session expired')
    }
  }, [persistUser])

  useEffect(() => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)

    if (!token) {
      setIsLoading(false)
      return
    }

    const storedUser = localStorage.getItem(AUTH_USER_KEY)
    if (storedUser) {
      // Hiển thị UI ngay với session cache; refresh profile ở background.
      setIsLoading(false)
    }

    restoreSession()
      .catch(() => {
        clearAuthStorage()
        persistUser(null)
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [persistUser, restoreSession])

  const login = useCallback(
    async ({ email, password }) => {
      const payload = await loginRequest({ email, password })

      setAuthTokens({
        accessToken: payload.accessToken,
        refreshToken: payload.refreshToken,
      })

      if (payload.user) {
        persistUser(payload.user)
        return payload.user
      }

      return restoreSession()
    },
    [persistUser, restoreSession],
  )

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } catch {
      // Ignore logout errors and clear local session anyway.
    }

    clearAuthStorage()
    clearRolesCache()
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
    }),
    [user, isLoading, login, logout],
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
