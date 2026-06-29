import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Briefcase,
  Shield,
  UserCog,
  Users,
} from 'lucide-react'
import { fetchUsers } from '../../api/usersApi'
import { fetchWorkspaces } from '../../api/workspacesApi'
import LoadingScreen from '../../components/common/LoadingScreen'
import { PERMISSIONS } from '../../constants/permissions'
import { useAuth } from '../../context/AuthContext'
import PermissionRoute from '../../routes/PermissionRoute'

function AdminStatCard({ icon: Icon, label, value, to, tone = 'default' }) {
  return (
    <Link to={to} className={`admin-stat-card admin-stat-card--${tone}`}>
      <span className="admin-stat-card__icon" aria-hidden="true">
        <Icon size={20} />
      </span>
      <div>
        <p className="admin-stat-card__label">{label}</p>
        <p className="admin-stat-card__value">{value}</p>
      </div>
    </Link>
  )
}

function AdminActionCard({ title, description, to, cta }) {
  return (
    <article className="admin-action-card">
      <h2 className="admin-action-card__title">{title}</h2>
      <p className="admin-action-card__text">{description}</p>
      <Link to={to} className="admin-action-card__link">
        {cta}
      </Link>
    </article>
  )
}

export default function AdminDashboardPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth()

  const { data: users = [], isLoading: usersLoading } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: fetchUsers,
    enabled: isAuthenticated && !authLoading,
  })

  const { data: workspaces = [], isLoading: workspacesLoading } = useQuery({
    queryKey: ['admin', 'workspaces'],
    queryFn: fetchWorkspaces,
  })

  if (authLoading || usersLoading || workspacesLoading) {
    return <LoadingScreen />
  }

  const activeUsers = users.filter((user) => user.status === 'ACTIVE').length

  return (
    <PermissionRoute permission={PERMISSIONS.USER_READ}>
      <div className="page page--wide admin-dashboard-page">
        <header className="admin-dashboard-page__header">
          <p className="admin-dashboard-page__eyebrow">System Admin</p>
          <h1 className="admin-dashboard-page__title">Admin Dashboard</h1>
          <p className="admin-dashboard-page__subtitle">
            Quản lý Account và Workspace độc lập. Tạo tài khoản trước, tạo
            workspace riêng, sau đó gán account vào workspace khi cần.
          </p>
        </header>

        <section className="admin-dashboard-page__stats" aria-label="Thống kê">
          <AdminStatCard
            icon={Users}
            label="Tổng Account"
            value={String(users.length).padStart(2, '0')}
            to="/admin/users"
            tone="primary"
          />
          <AdminStatCard
            icon={UserCog}
            label="Account hoạt động"
            value={String(activeUsers).padStart(2, '0')}
            to="/admin/users/status"
            tone="success"
          />
          <AdminStatCard
            icon={Briefcase}
            label="Workspace"
            value={String(workspaces.length).padStart(2, '0')}
            to="/admin/workspaces"
            tone="violet"
          />
          <AdminStatCard
            icon={Shield}
            label="Roles & Permissions"
            value="RBAC"
            to="/roles"
            tone="slate"
          />
        </section>

        <section className="admin-dashboard-page__actions">
          <AdminActionCard
            title="Account Management"
            description="Tạo và quản lý tài khoản hệ thống. Account không bắt buộc gắn với workspace ngay khi tạo."
            to="/admin/users/create"
            cta="Tạo Account mới"
          />
          <AdminActionCard
            title="Workspace Management"
            description="Tạo workspace độc lập. Workspace và Account là hai nghiệp vụ tách biệt."
            to="/admin/workspaces/create"
            cta="Tạo Workspace mới"
          />
          <AdminActionCard
            title="Assign Accounts to Workspace"
            description="Chọn nhiều account và thêm vào workspace đã tạo."
            to="/admin/workspaces/assign-accounts"
            cta="Gán Account vào Workspace"
          />
        </section>
      </div>
    </PermissionRoute>
  )
}
