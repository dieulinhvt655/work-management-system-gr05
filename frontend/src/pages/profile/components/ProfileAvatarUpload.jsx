import { useRef, useState } from 'react'
import { Camera, X } from 'lucide-react'
import UserAvatar from '../../../components/common/UserAvatar'

const MAX_SIZE = 36 * 1024 * 1024
const ACCEPT = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp']

export default function ProfileAvatarUpload({
  fullName,
  value,
  onChange,
  error,
  compact = false,
}) {
  const inputRef = useRef(null)
  const [localError, setLocalError] = useState('')

  const handleFile = (file) => {
    if (!file) return

    if (!ACCEPT.includes(file.type)) {
      setLocalError('Chỉ chấp nhận file PNG, JPG hoặc WEBP')
      return
    }

    if (file.size > MAX_SIZE) {
      setLocalError('Dung lượng tối đa 36MB')
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
    <div className={`profile-avatar-upload${compact ? ' profile-avatar-upload--compact' : ''}`}>
      <div className="profile-avatar-upload__preview">
        <UserAvatar fullName={fullName} avatarUrl={value} size="lg" />
        <button
          type="button"
          className="profile-avatar-upload__trigger"
          onClick={() => inputRef.current?.click()}
          aria-label="Tải lên ảnh đại diện"
        >
          <Camera size={14} aria-hidden="true" />
        </button>
        {value && (
          <button
            type="button"
            className="profile-avatar-upload__remove"
            onClick={() => onChange('')}
            aria-label="Xóa ảnh đại diện"
          >
            <X size={12} aria-hidden="true" />
          </button>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept=".png,.jpg,.jpeg,.webp"
        className="profile-avatar-upload__input"
        onChange={(event) => handleFile(event.target.files?.[0])}
      />

      <div className="profile-avatar-upload__meta">
        <p className="profile-avatar-upload__label">Ảnh đại diện</p>
        <p className="profile-avatar-upload__hint">
          PNG, JPG hoặc WEBP — tối đa 36MB.
        </p>
      </div>

      {displayError && (
        <p className="field__error" role="alert">
          {displayError}
        </p>
      )}
    </div>
  )
}
