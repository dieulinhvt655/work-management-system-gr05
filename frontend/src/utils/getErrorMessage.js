export function getErrorMessage(error, fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.') {
  const data = error?.response?.data
  const status = error?.response?.status

  if (typeof data === 'string') {
    if (
      data.includes('<!DOCTYPE')
      || data.includes('ngrok')
      || data.includes('ERR_NGROK')
    ) {
      return 'Không kết nối được máy chủ API. Backend/ngrok có thể đang offline — kiểm tra VITE_PROXY_TARGET trong .env.development và restart npm run dev.'
    }

    return data
  }

  if (status === 404 && !data) {
    return 'Không tìm thấy API. Kiểm tra backend đang chạy và VITE_PROXY_TARGET trong .env.development.'
  }

  if (status === 403) {
    return data?.message ?? 'Không có quyền truy cập. Tài khoản hiện tại chưa được backend cấp quyền thực hiện thao tác này.'
  }

  if (data?.success === false && data?.message) {
    return data.message
  }

  return data?.message ?? data?.error ?? error?.message ?? fallback
}
