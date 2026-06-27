import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import LoadingScreen from '../components/common/LoadingScreen'
import PagePlaceholder from '../components/common/PagePlaceholder'
import AppLayout from '../components/layout/AppLayout'
import AuthLayout from '../components/layout/AuthLayout'
import LoginPage from '../pages/auth/LoginPage'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage'
import CheckEmailPage from '../pages/auth/CheckEmailPage'
import WelcomePage from '../pages/welcome/WelcomePage'
import { useAuth } from '../context/AuthContext'

function LandingRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  return <WelcomePage />
}

function PrivateRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}

function PublicRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/forgot-password/sent" element={<CheckEmailPage />} />
        </Route>
      </Route>

      <Route element={<PrivateRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<PagePlaceholder title="Dashboard" />} />
          <Route path="/workspace" element={<PagePlaceholder title="Workspace" />} />
          <Route path="/projects" element={<PagePlaceholder title="Dự án" />} />
          <Route path="/backlog" element={<PagePlaceholder title="Product Backlog" />} />
          <Route path="/sprints" element={<PagePlaceholder title="Sprint" />} />
          <Route path="/profile" element={<PagePlaceholder title="Hồ sơ cá nhân" />} />
        </Route>
      </Route>

      <Route path="/" element={<LandingRoute />} />
      <Route
        path="*"
        element={<PagePlaceholder title="404 - Không tìm thấy trang" />}
      />
    </Routes>
  )
}
