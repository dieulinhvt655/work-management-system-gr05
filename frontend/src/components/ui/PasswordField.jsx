import { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

export default function PasswordField({
  id,
  label,
  labelExtra,
  error,
  className = '',
  ...props
}) {
  const [visible, setVisible] = useState(false)

  return (
    <div className={`field${className ? ` ${className}` : ''}`}>
      {(label || labelExtra) && (
        <div className="field__label-row">
          {label && (
            <label className="field__label" htmlFor={id}>
              {label}
            </label>
          )}
          {labelExtra}
        </div>
      )}
      <div className="field__input-wrapper">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          className={`field__input field__input--password${error ? ' field__input--error' : ''}`}
          {...props}
        />
        <button
          type="button"
          className="field__toggle"
          onClick={() => setVisible((prev) => !prev)}
          aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
        >
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
      {error && <p className="field__error">{error}</p>}
    </div>
  )
}
