import { useCallback, useEffect, useState } from 'react'

const STORAGE_KEY = 'wms-sidebar-width'
const DEFAULT_WIDTH = 268
const MIN_WIDTH = 220
const MAX_WIDTH = 420

function readStoredWidth() {
  const stored = localStorage.getItem(STORAGE_KEY)
  const parsed = stored ? Number(stored) : DEFAULT_WIDTH

  if (!Number.isFinite(parsed)) {
    return DEFAULT_WIDTH
  }

  return Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, parsed))
}

export function useResizableSidebar() {
  const [width, setWidth] = useState(readStoredWidth)
  const [isResizing, setIsResizing] = useState(false)

  const startResize = useCallback((event) => {
    event.preventDefault()
    setIsResizing(true)
  }, [])

  useEffect(() => {
    if (!isResizing) return undefined

    const handleMouseMove = (event) => {
      const next = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, event.clientX))
      setWidth(next)
    }

    const handleMouseUp = () => {
      setIsResizing(false)
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
    document.body.style.cursor = 'col-resize'
    document.body.style.userSelect = 'none'

    return () => {
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
    }
  }, [isResizing])

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, String(width))
  }, [width])

  return { width, isResizing, startResize, resetWidth: () => setWidth(DEFAULT_WIDTH) }
}
