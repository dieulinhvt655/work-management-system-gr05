export function getErrorMessage(error, fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.') {
  const data = error?.response?.data

  if (typeof data === 'string') {
    return data
  }

  return data?.message ?? data?.error ?? error?.message ?? fallback
}
