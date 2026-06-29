/** Unwrap standard `{ success, data, message, errorCode }` API envelope. */
export function unwrapApiResponse(response) {
  const body = response.data

  if (body?.success === false) {
    const error = new Error(body.message || 'Yêu cầu thất bại')
    error.response = response
    error.errorCode = body.errorCode
    throw error
  }

  return body?.data ?? body
}

export function extractPageItems(payload) {
  if (Array.isArray(payload)) return payload
  return payload?.items ?? payload?.content ?? []
}

export function extractTotalPages(payload) {
  if (Array.isArray(payload)) return 1
  return payload?.totalPages ?? 1
}

export async function fetchAllPages(fetchPage) {
  const size = 100
  let page = 0
  let totalPages = 1
  const items = []
  while (page < totalPages) {
    const payload = await fetchPage({ page, size })
    items.push(...extractPageItems(payload))
    totalPages = extractTotalPages(payload)
    page += 1
  }
  return items
}
