import { useEffect } from 'react'
import { CheckCircle2, X } from 'lucide-react'

export default function Toast({ message, onClose, duration = 4000 }) {
  useEffect(() => {
    const timer = window.setTimeout(onClose, duration)
    return () => window.clearTimeout(timer)
  }, [duration, onClose])

  return (
    <div className="toast" role="status" aria-live="polite">
      <CheckCircle2 size={18} className="toast__icon" aria-hidden="true" />
      <p className="toast__message">{message}</p>
      <button
        type="button"
        className="toast__close"
        onClick={onClose}
        aria-label="Đóng thông báo"
      >
        <X size={16} aria-hidden="true" />
      </button>
    </div>
  )
}
