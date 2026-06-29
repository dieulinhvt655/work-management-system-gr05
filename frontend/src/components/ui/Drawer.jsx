import { useEffect } from 'react'
import { X } from 'lucide-react'

export default function Drawer({
  title,
  subtitle,
  onClose,
  children,
  footer,
  width = '420px',
}) {
  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [onClose])

  return (
    <div className="drawer-root" role="presentation">
      <button
        type="button"
        className="drawer-backdrop"
        onClick={onClose}
        aria-label="Đóng panel"
      />
      <aside
        className="drawer-panel"
        style={{ '--drawer-width': width }}
        role="dialog"
        aria-modal="true"
        aria-labelledby="drawer-title"
      >
        <header className="drawer-panel__header">
          <div className="drawer-panel__heading">
            {title && (
              <h2 id="drawer-title" className="drawer-panel__title">
                {title}
              </h2>
            )}
            {subtitle && <p className="drawer-panel__subtitle">{subtitle}</p>}
          </div>
          <button
            type="button"
            className="drawer-panel__close"
            onClick={onClose}
            aria-label="Đóng"
          >
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        <div className="drawer-panel__body">{children}</div>

        {footer && <footer className="drawer-panel__footer">{footer}</footer>}
      </aside>
    </div>
  )
}
