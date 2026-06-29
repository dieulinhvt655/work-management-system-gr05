export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || '/api/v1'

/** Mock auth is opt-in only — set VITE_USE_MOCK_AUTH=true to enable. */
export const USE_MOCK_AUTH = import.meta.env.VITE_USE_MOCK_AUTH === 'true'

export const MOCK_ACCESS_TOKEN = 'mock-access-token'

export const IS_NGROK_API = API_BASE_URL.includes('ngrok')
