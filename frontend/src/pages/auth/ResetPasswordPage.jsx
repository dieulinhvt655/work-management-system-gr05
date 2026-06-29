import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useSearchParams } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import { resetPassword } from '../../api/authService'
import AuthBackLink from '../../components/common/AuthBackLink'
import AuthBrand from '../../components/common/AuthBrand'
import Button from '../../components/ui/Button'
import PasswordField from '../../components/ui/PasswordField'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { resetPasswordSchema } from './resetPasswordSchema'
import '../../assets/styles/auth.css'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')?.trim()
  const [submitError, setSubmitError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      newPassword: '',
      confirmPassword: '',
    },
  })

  if (!token) {
    return (
      <div className="auth-card auth-card--centered">
        <AuthBrand />
        <h1 className="auth-card__title">Link không hợp lệ</h1>
        <p className="auth-card__subtitle">
          Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu
          link mới.
        </p>
        <Link to="/forgot-password" className="btn btn--primary">
          Yêu cầu link mới
        </Link>
        <div className="auth-card__footer">
          <AuthBackLink to="/login" />
        </div>
      </div>
    )
  }

  if (successMessage) {
    return (
      <div className="auth-card auth-card--centered">
        <AuthBrand />
        <div className="auth-status-icon auth-status-icon--success" aria-hidden="true">
          <CheckCircle2 size={28} strokeWidth={1.75} />
        </div>
        <h1 className="auth-card__title">Đổi mật khẩu thành công</h1>
        <p className="auth-card__subtitle auth-card__subtitle--compact">
          {successMessage}
        </p>
        <Link to="/login" className="btn btn--primary">
          Đăng nhập
        </Link>
      </div>
    )
  }

  const onSubmit = async ({ newPassword }) => {
    setSubmitError('')

    try {
      const result = await resetPassword({ token, newPassword })
      setSuccessMessage(
        result.message ?? 'Mật khẩu đã được cập nhật. Bạn có thể đăng nhập ngay.',
      )
    } catch (error) {
      setSubmitError(
        getErrorMessage(error, 'Không thể đặt lại mật khẩu. Vui lòng thử lại.'),
      )
    }
  }

  return (
    <div className="auth-card">
      <AuthBrand />

      <h1 className="auth-card__title">Đặt mật khẩu mới</h1>
      <p className="auth-card__subtitle">
        Nhập mật khẩu mới cho tài khoản của bạn.
      </p>

      <form className="auth-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {submitError && (
          <p className="auth-form__error" role="alert">
            {submitError}
          </p>
        )}

        <PasswordField
          id="new-password"
          label="Mật khẩu mới"
          placeholder="••••••••"
          autoComplete="new-password"
          error={errors.newPassword?.message}
          {...register('newPassword')}
        />

        <PasswordField
          id="confirm-password"
          label="Xác nhận mật khẩu mới"
          placeholder="••••••••"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {isSubmitting ? 'Đang lưu...' : 'Cập nhật mật khẩu'}
        </Button>
      </form>

      <div className="auth-card__footer">
        <AuthBackLink to="/login" />
      </div>
    </div>
  )
}
