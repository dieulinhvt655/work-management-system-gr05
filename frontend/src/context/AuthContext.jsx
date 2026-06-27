import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api, { clearAuthStorage, setAuthTokens } from '../api/axios'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/auth'

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

    fetchCurrentUser()
      .catch(() => {
        clearAuthStorage()
        persistUser(null)
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [fetchCurrentUser, persistUser])

  const login = useCallback(async (credentials) => {
    const { data } = await api.post('/auth/login', credentials)
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
  }, [fetchCurrentUser, persistUser])

  const register = useCallback(async (payload) => {
    const { data } = await api.post('/auth/register', payload)
    return unwrapResponse({ data })
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      // Ignore logout errors and clear local session anyway.
    } finally {
      clearAuthStorage()
      persistUser(null)
    }
  }, [persistUser])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isLoading,
      login,
      register,
      logout,
    }),
    [user, isLoading, login, register, logout],
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
