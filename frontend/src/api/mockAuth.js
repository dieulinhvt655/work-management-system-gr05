import { MOCK_ACCESS_TOKEN, USE_MOCK_AUTH } from '../constants/config'
import { createMockUser } from '../constants/mock/rolePermissions'

export function isMockSession(token) {
  return USE_MOCK_AUTH && token === MOCK_ACCESS_TOKEN
}

export function mockLogin({ mockRole, email }) {
  if (!mockRole) {
    throw new Error('Mock role is required for mock login')
  }

  return createMockUser(mockRole, email)
}
