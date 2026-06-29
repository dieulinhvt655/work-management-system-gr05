import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from 'react-router-dom'
import { requestPasswordReset } from '../../api/authService'
import AuthBackLink from '../../components/common/AuthBackLink'
import AuthBrand from '../../components/common/AuthBrand'
import Button from '../../components/ui/Button'
import TextField from '../../components/ui/TextField'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { forgotPasswordSchema } from './forgotPasswordSchema'
import '../../assets/styles/auth.css'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [submitError, setSubmitError] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: '',
    },
  })

  const onSubmit = async ({ email }) => {
    setSubmitError('')

    try {
      const result = await requestPasswordReset(email)
      navigate('/forgot-password/sent', {
        state: { email, message: result.message },
        replace: true,
      })
    } catch (error) {
      setSubmitError(
        getErrorMessage(error, 'Không thể gửi link đặt lại mật khẩu. Vui lòng thử lại.'),
      )
    }
  }

  return (
    <div className="auth-card">
      <AuthBrand />

      <h1 className="auth-card__title">Đặt lại mật khẩu</h1>
      <p className="auth-card__subtitle">
        Nhập email liên kết với tài khoản của bạn, chúng tôi sẽ gửi link đặt lại
        mật khẩu.
      </p>

      <form className="auth-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {submitError && (
          <p className="auth-form__error" role="alert">
            {submitError}
          </p>
        )}

        <TextField
          id="email"
          type="email"
          label="Email công việc"
          placeholder="name@company.com"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />

        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {isSubmitting ? 'Đang gửi...' : 'Gửi link đặt lại'}
        </Button>
      </form>

      <div className="auth-card__footer">
        <AuthBackLink />
      </div>
    </div>
  )
}
