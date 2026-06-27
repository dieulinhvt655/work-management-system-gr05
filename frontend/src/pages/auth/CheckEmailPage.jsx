import { useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Mail } from 'lucide-react'
import { requestPasswordReset } from '../../api/authApi'
import AuthBackLink from '../../components/common/AuthBackLink'
import AuthBrand from '../../components/common/AuthBrand'
import { getErrorMessage } from '../../utils/getErrorMessage'
import '../../assets/styles/auth.css'

export default function CheckEmailPage() {
  const location = useLocation()
  const email = location.state?.email
  const [isResending, setIsResending] = useState(false)
  const [resendError, setResendError] = useState('')

  if (!email) {
    return <Navigate to="/forgot-password" replace />
  }

  const handleResend = async () => {
    setResendError('')
    setIsResending(true)

    try {
      await requestPasswordReset(email)
    } catch (error) {
      setResendError(getErrorMessage(error, 'Không thể gửi lại email. Vui lòng thử lại.'))
    } finally {
      setIsResending(false)
    }
  }

  return (
    <div className="auth-card auth-card--centered">
      <AuthBrand />

      <div className="auth-status-icon" aria-hidden="true">
        <Mail size={28} strokeWidth={1.75} />
      </div>

      <h1 className="auth-card__title">Kiểm tra email của bạn</h1>

      <p className="auth-card__subtitle auth-card__subtitle--compact">
        Chúng tôi đã gửi link đặt lại mật khẩu đến
      </p>
      <p className="auth-card__email">{email}</p>

      <div className="auth-info-box">
        <p>
          Không nhận được email? Kiểm tra thư mục spam hoặc{' '}
          <button
            type="button"
            className="auth-info-box__link"
            onClick={handleResend}
            disabled={isResending}
          >
            {isResending ? 'Đang gửi lại...' : 'thử lại'}
          </button>
        </p>
        {resendError && (
          <p className="auth-info-box__error" role="alert">
            {resendError}
          </p>
        )}
      </div>

      <div className="auth-card__footer">
        <AuthBackLink />
      </div>
    </div>
  )
}
