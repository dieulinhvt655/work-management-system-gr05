export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const IS_NGROK_API = API_BASE_URL.includes('ngrok')
