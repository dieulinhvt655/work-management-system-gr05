import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import LoadingScreen from '../components/common/LoadingScreen'
import PagePlaceholder from '../components/common/PagePlaceholder'
import AppLayout from '../components/layout/AppLayout'
import AuthLayout from '../components/layout/AuthLayout'
import { ROUTE_PERMISSIONS } from '../constants/navigation/navItems'
import { useAuth } from '../context/AuthContext'
import LoginPage from '../pages/auth/LoginPage'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage'
import CheckEmailPage from '../pages/auth/CheckEmailPage'
import ForbiddenPage from '../pages/errors/ForbiddenPage'
import WelcomePage from '../pages/welcome/WelcomePage'
import WorkspacesListPage from '../pages/admin/workspaces/WorkspacesListPage'
import CreateWorkspacePage from '../pages/admin/workspaces/CreateWorkspacePage'
import UsersListPage from '../pages/admin/users/UsersListPage'
import CreateUserPage from '../pages/admin/users/CreateUserPage'
import UserDetailPage from '../pages/admin/users/UserDetailPage'
import UserGeneralTab from '../pages/admin/users/detail/UserGeneralTab'
import UserRolesTab from '../pages/admin/users/detail/UserRolesTab'
import UserAccountStatusTab from '../pages/admin/users/detail/UserAccountStatusTab'
import UserActivityTab from '../pages/admin/users/detail/UserActivityTab'
import ProjectsListPage from '../pages/projects/ProjectsListPage'
import ProjectIndexRedirect from '../pages/projects/ProjectIndexRedirect'
import ProjectLayout from '../components/layout/ProjectLayout'
import {
  ProjectOverviewPage,
  ProjectMyTasksPage,
  ProjectBacklogPage,
  ProjectSprintPage,
  ProjectMembersPage,
  ProjectDocsPage,
  ProjectActivityPage,
} from '../pages/projects/detail/ProjectTabPages'
import PermissionRoute from './PermissionRoute'
import { getDefaultRoute } from '../utils/navUtils'

function LandingRoute() {
  const { isAuthenticated, isLoading, permissions } = useAuth()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isAuthenticated) {
    return <Navigate to={getDefaultRoute(permissions)} replace />
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
  const { isAuthenticated, isLoading, permissions } = useAuth()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isAuthenticated) {
    return <Navigate to={getDefaultRoute(permissions)} replace />
  }

  return <Outlet />
}

function ProtectedPage({ path, title, description }) {
  return (
    <PermissionRoute permission={ROUTE_PERMISSIONS[path]}>
      <PagePlaceholder title={title} description={description} />
    </PermissionRoute>
  )
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
        <Route path="/403" element={<ForbiddenPage />} />

        <Route element={<AppLayout />}>
          <Route
            path="/dashboard"
            element={<Navigate to="/dashboard/my" replace />}
          />
          <Route
            path="/dashboard/workspace"
            element={
              <ProtectedPage
                path="/dashboard/workspace"
                title="Workspace Dashboard"
              />
            }
          />
          <Route
            path="/dashboard/team"
            element={
              <ProtectedPage path="/dashboard/team" title="Team Dashboard" />
            }
          />
          <Route
            path="/dashboard/project"
            element={
              <ProtectedPage
                path="/dashboard/project"
                title="Project Dashboard"
              />
            }
          />
          <Route
            path="/dashboard/my"
            element={
              <ProtectedPage path="/dashboard/my" title="My Dashboard" />
            }
          />

          <Route path="/admin/workspaces" element={<WorkspacesListPage />} />
          <Route path="/admin/workspaces/create" element={<CreateWorkspacePage />} />
          <Route
            path="/admin/workspaces/activity"
            element={
              <ProtectedPage
                path="/admin/workspaces/activity"
                title="Workspace Activity"
              />
            }
          />

          <Route
            path="/workspace/info"
            element={
              <ProtectedPage
                path="/workspace/info"
                title="Thông tin Workspace"
              />
            }
          />
          <Route
            path="/workspace/settings"
            element={
              <ProtectedPage
                path="/workspace/settings"
                title="Workspace Settings"
              />
            }
          />
          <Route
            path="/workspace/activity"
            element={
              <ProtectedPage
                path="/workspace/activity"
                title="Workspace Activity"
              />
            }
          />

          <Route path="/admin/users" element={<UsersListPage />} />
          <Route path="/admin/users/create" element={<CreateUserPage />} />
          <Route
            path="/admin/users/status"
            element={
              <ProtectedPage
                path="/admin/users/status"
                title="Trạng thái tài khoản"
              />
            }
          />
          <Route path="/admin/users/:userId" element={<UserDetailPage />}>
            <Route index element={<UserGeneralTab />} />
            <Route path="roles" element={<UserRolesTab />} />
            <Route path="account-status" element={<UserAccountStatusTab />} />
            <Route path="activity" element={<UserActivityTab />} />
          </Route>

          <Route
            path="/teams"
            element={
              <ProtectedPage path="/teams" title="Danh sách Team" />
            }
          />
          <Route
            path="/teams/departments"
            element={
              <ProtectedPage
                path="/teams/departments"
                title="Danh sách Department"
              />
            }
          />
          <Route
            path="/teams/assignments"
            element={
              <ProtectedPage
                path="/teams/assignments"
                title="Phân công thành viên"
              />
            }
          />

          <Route
            path="/members"
            element={
              <ProtectedPage path="/members" title="Danh sách thành viên" />
            }
          />
          <Route
            path="/members/invite"
            element={
              <ProtectedPage path="/members/invite" title="Mời thành viên" />
            }
          />
          <Route
            path="/members/permissions"
            element={
              <ProtectedPage
                path="/members/permissions"
                title="Phân quyền thành viên"
              />
            }
          />

          <Route
            path="/roles"
            element={
              <ProtectedPage path="/roles" title="Roles & Permissions" />
            }
          />

          <Route path="/projects" element={<ProjectsListPage />} />
          <Route path="/projects/:projectId" element={<ProjectLayout />}>
            <Route index element={<ProjectIndexRedirect />} />
            <Route path="overview" element={<ProjectOverviewPage />} />
            <Route path="my-tasks" element={<ProjectMyTasksPage />} />
            <Route path="backlog" element={<ProjectBacklogPage />} />
            <Route path="sprint" element={<ProjectSprintPage />} />
            <Route path="members" element={<ProjectMembersPage />} />
            <Route path="docs" element={<ProjectDocsPage />} />
            <Route path="activity" element={<ProjectActivityPage />} />
          </Route>

          <Route
            path="/my-work/tasks"
            element={<ProtectedPage path="/my-work/tasks" title="My Tasks" />}
          />
          <Route
            path="/my-work/assigned"
            element={
              <ProtectedPage path="/my-work/assigned" title="Assigned Tasks" />
            }
          />
          <Route
            path="/my-work/activity"
            element={
              <ProtectedPage path="/my-work/activity" title="My Activity" />
            }
          />

          <Route
            path="/admin/audit/system"
            element={
              <ProtectedPage path="/admin/audit/system" title="System Logs" />
            }
          />
          <Route
            path="/admin/audit/security"
            element={
              <ProtectedPage
                path="/admin/audit/security"
                title="Security Logs"
              />
            }
          />

          <Route
            path="/admin/settings"
            element={<ProtectedPage path="/admin/settings" title="Settings" />}
          />

          <Route
            path="/profile"
            element={
              <ProtectedPage path="/profile" title="Hồ sơ cá nhân" />
            }
          />
        </Route>
      </Route>

      <Route path="/" element={<LandingRoute />} />
      <Route path="*" element={<PagePlaceholder title="404 - Không tìm thấy trang" />} />
    </Routes>
  )
}
