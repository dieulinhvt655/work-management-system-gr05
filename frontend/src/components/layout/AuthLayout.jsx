import { Outlet } from 'react-router-dom'
import AppFooter from './AppFooter'

export default function AuthLayout() {
  return (
    <div className="auth-layout">
      <div className="auth-layout__content">
        <Outlet />
      </div>
      <AppFooter />
    </div>
  )
}
