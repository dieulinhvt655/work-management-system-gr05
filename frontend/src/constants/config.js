function normalizeApiBaseUrl(value) {
  const raw = (value || '/api/v1').replace(/\/+$/, '')
  return raw.endsWith('/api/v1') ? raw : `${raw}/api/v1`
}

export const API_BASE_URL = normalizeApiBaseUrl(
  import.meta.env.VITE_API_BASE_URL,
)

export const IS_NGROK_API = API_BASE_URL.includes('ngrok')
