import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import AuthBrand from '../../components/common/AuthBrand'
import Button from '../../components/ui/Button'
import PasswordField from '../../components/ui/PasswordField'
import TextField from '../../components/ui/TextField'
import { useAuth } from '../../context/AuthContext'
import { getErrorMessage } from '../../utils/getErrorMessage'
import { loginSchema } from './loginSchema'
import '../../assets/styles/auth.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [submitError, setSubmitError] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = async (values) => {
    setSubmitError('')

    try {
      await login(values)
      navigate('/dashboard', { replace: true })
    } catch (error) {
      setSubmitError(getErrorMessage(error, 'Email hoặc mật khẩu không đúng.'))
    }
  }

  return (
    <div className="auth-card">
      <AuthBrand />

      <h1 className="auth-card__title">Welcome Back!</h1>
      <p className="auth-card__subtitle">
        Đăng nhập để quản lý công việc và nhóm của bạn.
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
          placeholder="alex.chen@company.com"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />

        <PasswordField
          id="password"
          label="Mật khẩu"
          placeholder="••••••••"
          autoComplete="current-password"
          error={errors.password?.message}
          labelExtra={
            <Link className="field__link" to="/forgot-password">
              Quên mật khẩu?
            </Link>
          }
          {...register('password')}
        />

        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </Button>
      </form>
    </div>
  )
}
