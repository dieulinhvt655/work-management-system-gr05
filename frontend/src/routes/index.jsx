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
import ResetPasswordPage from '../pages/auth/ResetPasswordPage'
import ForbiddenPage from '../pages/errors/ForbiddenPage'
import WelcomePage from '../pages/welcome/WelcomePage'
import AdminDashboardPage from '../pages/admin/AdminDashboardPage'
import WorkspacesListPage from '../pages/admin/workspaces/WorkspacesListPage'
import CreateWorkspacePage from '../pages/admin/workspaces/CreateWorkspacePage'
import AssignAccountsToWorkspacePage from '../pages/admin/workspaces/AssignAccountsToWorkspacePage'
import UsersListPage from '../pages/admin/users/UsersListPage'
import CreateUserPage from '../pages/admin/users/CreateUserPage'
import AccountStatusPage from '../pages/admin/users/AccountStatusPage'
import UserDetailPage from '../pages/admin/users/UserDetailPage'
import UserGeneralTab from '../pages/admin/users/detail/UserGeneralTab'
import UserRolesTab from '../pages/admin/users/detail/UserRolesTab'
import UserAccountStatusTab from '../pages/admin/users/detail/UserAccountStatusTab'
import UserActivityTab from '../pages/admin/users/detail/UserActivityTab'
import ProjectsListPage from '../pages/projects/ProjectsListPage'
import CreateProjectPage from '../pages/projects/CreateProjectPage'
import TeamMembersPage from '../pages/projects/TeamMembersPage'
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
import RolesListPage from '../pages/admin/roles/RolesListPage'
import TeamsListPage from '../pages/teams/TeamsListPage'
import CreateTeamPage from '../pages/teams/CreateTeamPage'
import AssignMembersToTeamPage from '../pages/teams/AssignMembersToTeamPage'
import TeamDetailPage from '../pages/teams/TeamDetailPage'
import MembersListPage from '../pages/members/MembersListPage'
import MemberDetailPage from '../pages/members/MemberDetailPage'
import ProfilePage from '../pages/profile/ProfilePage'
import WorkspaceDashboardPage from '../pages/dashboard/WorkspaceDashboardPage'
import TeamLeaderDashboardPage from '../pages/dashboard/TeamLeaderDashboardPage'
import WorkspaceInfoPage from '../pages/workspace/WorkspaceInfoPage'
import WorkspaceActivityPage from '../pages/workspace/WorkspaceActivityPage'
import WorkspaceRolesPage from '../pages/workspace/WorkspaceRolesPage'
import PermissionRoute from './PermissionRoute'
import { getDefaultRoute } from '../utils/navUtils'

function LandingRoute() {
  const { isAuthenticated, isLoading, permissions, user } = useAuth()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isAuthenticated) {
    return <Navigate to={getDefaultRoute(permissions, user)} replace />
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
  const { isAuthenticated, isLoading, permissions, user } = useAuth()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isAuthenticated) {
    return <Navigate to={getDefaultRoute(permissions, user)} replace />
  }

  return <Outlet />
}

function ProtectedPage({ path, title, description }) {
  const content = <PagePlaceholder title={title} description={description} />
  const permission = ROUTE_PERMISSIONS[path]

  if (!permission) {
    return content
  }

  return <PermissionRoute permission={permission}>{content}</PermissionRoute>
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/forgot-password/sent" element={<CheckEmailPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
        </Route>
      </Route>

      <Route element={<PrivateRoute />}>
        <Route path="/403" element={<ForbiddenPage />} />

        <Route element={<AppLayout />}>
          <Route
            path="/dashboard"
            element={<Navigate to="/dashboard/workspace" replace />}
          />
          <Route
            path="/dashboard/workspace"
            element={<WorkspaceDashboardPage />}
          />
          <Route path="/dashboard/team" element={<TeamLeaderDashboardPage />} />
          <Route
            path="/dashboard/my"
            element={
              <ProtectedPage path="/dashboard/my" title="My Dashboard" />
            }
          />

          <Route path="/admin/dashboard" element={<AdminDashboardPage />} />

          <Route path="/admin/workspaces" element={<WorkspacesListPage />} />
          <Route path="/admin/workspaces/create" element={<CreateWorkspacePage />} />
          <Route
            path="/admin/workspaces/assign-accounts"
            element={<AssignAccountsToWorkspacePage />}
          />
          <Route
            path="/admin/workspaces/activity"
            element={
              <ProtectedPage
                path="/admin/workspaces/activity"
                title="Workspace Activity"
              />
            }
          />

          <Route path="/workspace/info" element={<WorkspaceInfoPage />} />
          <Route path="/workspace/roles" element={<WorkspaceRolesPage />} />
          <Route
            path="/workspace/settings"
            element={
              <ProtectedPage
                path="/workspace/settings"
                title="Workspace Settings"
              />
            }
          />
          <Route path="/workspace/activity" element={<WorkspaceActivityPage />} />

          <Route path="/admin/users" element={<UsersListPage />} />
          <Route path="/admin/users/create" element={<CreateUserPage />} />
          <Route path="/admin/users/status" element={<AccountStatusPage />} />
          <Route path="/admin/users/:userId" element={<UserDetailPage />}>
            <Route index element={<UserGeneralTab />} />
            <Route path="roles" element={<UserRolesTab />} />
            <Route path="account-status" element={<UserAccountStatusTab />} />
            <Route path="activity" element={<UserActivityTab />} />
          </Route>

          <Route path="/teams" element={<TeamsListPage />} />
          <Route path="/teams/create" element={<CreateTeamPage />} />
          <Route
            path="/teams/assign-members"
            element={<AssignMembersToTeamPage />}
          />
          <Route path="/teams/:teamId" element={<TeamDetailPage />} />

          <Route path="/members" element={<MembersListPage />} />
          <Route path="/members/:memberId" element={<MemberDetailPage />} />
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
            path="/my-team"
            element={
              <ProtectedPage path="/my-team" title="My Team" />
            }
          />
          <Route
            path="/my-team/members"
            element={
              <ProtectedPage path="/my-team/members" title="Team Members" />
            }
          />

          <Route path="/roles" element={<RolesListPage />} />

          <Route path="/projects" element={<ProjectsListPage />} />
          <Route
            path="/projects/create"
            element={<CreateProjectPage />}
          />
          <Route
            path="/projects/team-members"
            element={<TeamMembersPage />}
          />
          <Route
            path="/projects/activity"
            element={
              <ProtectedPage path="/projects/activity" title="Project Activity" />
            }
          />
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

          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="/" element={<LandingRoute />} />
      <Route path="*" element={<PagePlaceholder title="404 - Không tìm thấy trang" />} />
    </Routes>
  )
}
