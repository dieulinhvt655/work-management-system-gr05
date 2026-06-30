import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Save, Undo2 } from 'lucide-react'
import {
  changePassword,
  fetchProfile,
  updateProfile,
} from '../../api/profileApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import Toast from '../../components/common/Toast'
import UserAvatar from '../../components/common/UserAvatar'
import Button from '../../components/ui/Button'
import TextField from '../../components/ui/TextField'
import { PERMISSIONS } from '../../constants/permissions'
import { USER_STATUS_LABELS } from '../../constants/users'
import PermissionRoute from '../../routes/PermissionRoute'
import { getErrorMessage } from '../../utils/getErrorMessage'
import ChangePasswordModal from './components/ChangePasswordModal'
import ProfileAvatarUpload from './components/ProfileAvatarUpload'
import { profileSchema } from './profileSchema'

const PROFILE_QUERY_KEY = ['profile', 'me']

function formatDateTime(value) {
  if (!value) return '—'

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function toFormValues(profile) {
  return {
    phone: String(profile.phone ?? ''),
    bio: profile.bio ?? '',
    avatarUrl: profile.avatarUrl ?? '',
  }
}

function isProfileDirty(formValues, profile) {
  if (!formValues || !profile) return false

  return (
    formValues.phone.trim() !== String(profile.phone ?? '').trim() ||
    formValues.bio.trim() !== String(profile.bio ?? '').trim() ||
    formValues.avatarUrl !== (profile.avatarUrl ?? '')
  )
}

export default function ProfilePage() {
  const queryClient = useQueryClient()
  const [formValues, setFormValues] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [toastMessage, setToastMessage] = useState('')
  const [showPasswordModal, setShowPasswordModal] = useState(false)
  const [passwordError, setPasswordError] = useState('')

  const { data: profile, isLoading } = useQuery({
    queryKey: PROFILE_QUERY_KEY,
    queryFn: fetchProfile,
  })

  useEffect(() => {
    if (profile && !formValues) {
      setFormValues(toFormValues(profile))
    }
  }, [profile, formValues])

  const updateMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (nextProfile, variables) => {
      const mergedProfile = {
        ...nextProfile,
        phone: nextProfile.phone ?? variables.phone ?? null,
        bio: nextProfile.bio ?? variables.bio ?? '',
        avatarUrl: nextProfile.avatarUrl ?? variables.avatarUrl ?? '',
      }

      setFormError('')
      setFieldErrors({})
      setFormValues(toFormValues(mergedProfile))
      setToastMessage('Hồ sơ cá nhân đã được cập nhật.')
      queryClient.setQueryData(PROFILE_QUERY_KEY, mergedProfile)
    },
    onError: (error) => {
      setFormError(getErrorMessage(error, 'Không thể cập nhật hồ sơ.'))
    },
  })

  const passwordMutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      setPasswordError('')
      setShowPasswordModal(false)
      setToastMessage('Mật khẩu đã được đổi thành công.')
    },
    onError: (error) => {
      setPasswordError(getErrorMessage(error, 'Không thể đổi mật khẩu.'))
    },
  })

  const setField = (key, value) => {
    setFieldErrors((current) => ({ ...current, [key]: '' }))
    setFormError('')
    setFormValues((current) => ({ ...current, [key]: value }))
  }

  const handleCancel = () => {
    if (profile) {
      setFormValues(toFormValues(profile))
    }
    setFieldErrors({})
    setFormError('')
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    if (!formValues) return

    if (!isProfileDirty(formValues, profile)) {
      setToastMessage('Không có thay đổi để lưu.')
      return
    }

    const result = profileSchema.safeParse(formValues)
    if (!result.success) {
      const nextErrors = {}
      for (const issue of result.error.issues) {
        const field = issue.path[0]
        if (field && !nextErrors[field]) {
          nextErrors[field] = issue.message
        }
      }
      setFieldErrors(nextErrors)
      return
    }

    updateMutation.mutate({
      phone: formValues.phone.trim(),
      bio: formValues.bio.trim(),
      avatarUrl: formValues.avatarUrl,
    })
  }

  if (isLoading || !profile || !formValues) {
    return <LoadingScreen />
  }

  const isDirty = isProfileDirty(formValues, profile)

  return (
    <PermissionRoute permission={PERMISSIONS.PROFILE_READ}>
      {toastMessage && (
        <Toast message={toastMessage} onClose={() => setToastMessage('')} />
      )}

      <div className="page profile-page">
        <header className="profile-page__header">
          <div>
            <p className="profile-page__eyebrow">Tài khoản</p>
            <h1 className="profile-page__title">Hồ sơ cá nhân</h1>
            <p className="profile-page__subtitle">
              Quản lý thông tin liên hệ và bảo mật tài khoản của bạn.
            </p>
          </div>
        </header>

        <div className="profile-page__layout">
          <section className="profile-card profile-card--readonly">
            <div className="profile-card__hero">
              <UserAvatar
                fullName={profile.fullName}
                avatarUrl={profile.avatarUrl}
                size="lg"
              />
              <div className="profile-card__hero-text">
                <h2 className="profile-card__name">{profile.fullName}</h2>
                <p className="profile-card__email">{profile.email}</p>
              </div>
            </div>

            <dl className="profile-readonly">
              <div className="profile-readonly__row">
                <dt>Tên đăng nhập</dt>
                <dd>{profile.username}</dd>
              </div>
              <div className="profile-readonly__row">
                <dt>Mã nhân viên</dt>
                <dd>
                  <code>{profile.employeeCode}</code>
                </dd>
              </div>
              <div className="profile-readonly__row">
                <dt>Team / Department</dt>
                <dd>{profile.teamName}</dd>
              </div>
              <div className="profile-readonly__row">
                <dt>Vai trò hệ thống</dt>
                <dd>{profile.roleLabel}</dd>
              </div>
              <div className="profile-readonly__row">
                <dt>Trạng thái tài khoản</dt>
                <dd>{USER_STATUS_LABELS[profile.accountStatus] ?? profile.accountStatus}</dd>
              </div>
              <div className="profile-readonly__row">
                <dt>Ngày tạo</dt>
                <dd>{formatDateTime(profile.createdAt)}</dd>
              </div>
              <div className="profile-readonly__row">
                <dt>Đăng nhập gần nhất</dt>
                <dd>{formatDateTime(profile.lastLoginAt)}</dd>
              </div>
            </dl>
          </section>

          <div className="profile-page__main">
            <form className="profile-card profile-card--combined" onSubmit={handleSubmit} noValidate>
              <section className="profile-section">
                <div className="profile-card__head">
                  <h2 className="profile-card__title">Thông tin có thể chỉnh sửa</h2>
                  <p className="profile-card__desc">
                    Cập nhật ảnh đại diện, số điện thoại và mô tả cá nhân.
                  </p>
                </div>

                {formError && (
                  <p className="profile-form__error" role="alert">
                    {formError}
                  </p>
                )}

                <div className="profile-editable__row">
                  <ProfileAvatarUpload
                    fullName={profile.fullName}
                    value={formValues.avatarUrl}
                    onChange={(value) => setField('avatarUrl', value)}
                    error={fieldErrors.avatarUrl}
                    compact
                  />

                  <TextField
                    id="profile-phone"
                    label="Số điện thoại"
                    value={formValues.phone}
                    onChange={(event) => setField('phone', event.target.value)}
                    error={fieldErrors.phone}
                    placeholder="0901234567"
                  />
                </div>

                <div className="field profile-form__description">
                  <label className="field__label" htmlFor="profile-bio">
                    Mô tả
                  </label>
                  <textarea
                    id="profile-bio"
                    className={`field__input profile-form__textarea${
                      fieldErrors.bio ? ' field__input--error' : ''
                    }`}
                    value={formValues.bio}
                    onChange={(event) => setField('bio', event.target.value)}
                    rows={4}
                    placeholder="Mô tả ngắn về bạn"
                  />
                  {fieldErrors.bio && (
                    <p className="field__error">{fieldErrors.bio}</p>
                  )}
                </div>

                <div className="profile-card__actions">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={handleCancel}
                    disabled={!isDirty || updateMutation.isPending}
                  >
                    <Undo2 size={16} aria-hidden="true" />
                    Hủy
                  </Button>
                  <Button
                    type="submit"
                    variant="primary"
                    disabled={updateMutation.isPending}
                  >
                    <Save size={16} aria-hidden="true" />
                    {updateMutation.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
                  </Button>
                </div>
              </section>

              <section className="profile-section profile-section--security">
                <div className="profile-section__security-copy">
                  <h2 className="profile-card__title">Bảo mật</h2>
                  <p className="profile-card__desc">
                    Đổi mật khẩu đăng nhập định kỳ để bảo vệ tài khoản.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  className="profile-card__security-btn"
                  onClick={() => {
                    setPasswordError('')
                    setShowPasswordModal(true)
                  }}
                >
                  <KeyRound size={16} aria-hidden="true" />
                  Đổi mật khẩu
                </Button>
              </section>
            </form>
          </div>
        </div>

        {showPasswordModal && (
          <ChangePasswordModal
            onClose={() => {
              setShowPasswordModal(false)
              setPasswordError('')
            }}
            onSubmit={(payload) => passwordMutation.mutate(payload)}
            isSaving={passwordMutation.isPending}
            serverError={passwordError}
          />
        )}
      </div>
    </PermissionRoute>
  )
}
