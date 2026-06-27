import { useRef, useState } from 'react'
import { CloudUpload, X } from 'lucide-react'

const MAX_SIZE = 2 * 1024 * 1024
const ACCEPT = ['image/png', 'image/jpeg', 'image/jpg']

export default function WorkspaceLogoUpload({ value, onChange, error }) {
  const inputRef = useRef(null)
  const [localError, setLocalError] = useState('')

  const handleFile = (file) => {
    if (!file) return

    if (!ACCEPT.includes(file.type)) {
      setLocalError('Chỉ chấp nhận file PNG hoặc JPG')
      return
    }

    if (file.size > MAX_SIZE) {
      setLocalError('Dung lượng tối đa 2MB')
      return
    }

    setLocalError('')
    const reader = new FileReader()
    reader.onload = () => {
      onChange(typeof reader.result === 'string' ? reader.result : '')
    }
    reader.readAsDataURL(file)
  }

  const displayError = error || localError

  return (
    <div className="workspace-logo-upload">
      <input
        ref={inputRef}
        type="file"
        accept=".png,.jpg,.jpeg"
        className="workspace-logo-upload__input"
        onChange={(event) => handleFile(event.target.files?.[0])}
      />

      {value ? (
        <div className="workspace-logo-upload__preview">
          <img src={value} alt="Logo preview" />
          <button
            type="button"
            className="workspace-logo-upload__remove"
            onClick={() => onChange('')}
            aria-label="Xóa logo"
          >
            <X size={14} aria-hidden="true" />
          </button>
        </div>
      ) : (
        <button
          type="button"
          className="workspace-logo-upload__dropzone"
          onClick={() => inputRef.current?.click()}
        >
          <CloudUpload size={28} aria-hidden="true" />
          <span>Tải lên Logo (PNG, JPG)</span>
        </button>
      )}

      <p className="workspace-logo-upload__hint">
        Kích thước khuyến nghị: 512x512px. Dung lượng tối đa 2MB.
      </p>

      {displayError && (
        <p className="field__error" role="alert">
          {displayError}
        </p>
      )}
    </div>
  )
}
