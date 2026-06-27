import { USER_ACCOUNT_STATUS } from '../users'

export const MOCK_DEPARTMENTS = [
  { id: 'dept-eng', name: 'Kỹ thuật' },
  { id: 'dept-product', name: 'Sản phẩm' },
  { id: 'dept-hr', name: 'Nhân sự' },
  { id: 'dept-ops', name: 'Vận hành' },
  { id: 'dept-sales', name: 'Kinh doanh' },
]

const DEPT_MAP = Object.fromEntries(
  MOCK_DEPARTMENTS.map((dept) => [dept.id, dept.name]),
)

function user(input) {
  return {
    departmentName: DEPT_MAP[input.departmentId] ?? '—',
    position: input.position ?? '—',
    createdAt: input.createdAt ?? '2024-06-01T09:00:00',
    lastLoginAt: input.lastLoginAt ?? input.lastActivityAt ?? null,
    ...input,
  }
}

export const MOCK_USERS_GROUPED_BY_WORKSPACE = [
  {
    workspaceId: 'ws-acme',
    workspaceName: 'Acme Workspace',
    workspaceCode: 'ACME',
    users: [
      user({
        id: 'user-001',
        fullName: 'Trần Thị Owner',
        email: 'owner@company.com',
        employeeCode: 'NV001',
        departmentId: 'dept-product',
        role: 'WORKSPACE_OWNER',
        status: USER_ACCOUNT_STATUS.ACTIVE,
        phone: '0902000001',
        position: 'Workspace Owner',
        createdAt: '2024-01-10T08:00:00',
        lastLoginAt: '2025-06-26T08:30:00',
        lastActivityAt: '2025-06-26T08:30:00',
      }),
      user({
        id: 'user-002',
        fullName: 'Lê Minh Tuấn',
        email: 'leader@company.com',
        employeeCode: 'NV002',
        departmentId: 'dept-eng',
        role: 'TEAM_LEADER',
        status: USER_ACCOUNT_STATUS.ACTIVE,
        phone: '0903000002',
        position: 'Team Leader',
        lastActivityAt: '2025-06-26T07:15:00',
      }),
      user({
        id: 'user-003',
        fullName: 'Phạm Thu Hà',
        email: 'pm@company.com',
        employeeCode: 'NV003',
        departmentId: 'dept-product',
        role: 'PROJECT_MANAGER',
        status: USER_ACCOUNT_STATUS.ACTIVE,
        phone: '0904000003',
        lastActivityAt: '2025-06-25T16:45:00',
      }),
      user({
        id: 'user-004',
        fullName: 'Hoàng Đức Anh',
        email: 'member@company.com',
        employeeCode: 'NV004',
        departmentId: 'dept-eng',
        role: 'TEAM_MEMBER',
        status: USER_ACCOUNT_STATUS.ACTIVE,
        phone: '0905000004',
        lastActivityAt: '2025-06-26T09:00:00',
      }),
      user({
        id: 'user-005',
        fullName: 'Võ Thị Lan',
        email: 'lan.vo@company.com',
        employeeCode: 'NV005',
        departmentId: 'dept-eng',
        role: 'TEAM_MEMBER',
        status: USER_ACCOUNT_STATUS.LOCKED,
        phone: '0906000005',
        lastActivityAt: '2025-06-10T14:30:00',
      }),
    ],
  },
  {
    workspaceId: 'ws-beta',
    workspaceName: 'Beta Workspace',
    workspaceCode: 'BETA',
    users: [
      user({
        id: 'user-006',
        fullName: 'Nguyễn Văn Admin',
        email: 'admin@company.com',
        employeeCode: 'NV006',
        departmentId: 'dept-hr',
        role: 'SYSTEM_ADMIN',
        status: USER_ACCOUNT_STATUS.ACTIVE,
        phone: '0901000006',
        lastActivityAt: '2025-06-26T10:00:00',
      }),
      user({
        id: 'user-007',
        fullName: 'Đặng Quốc Bảo',
        email: 'bao.dang@company.com',
        employeeCode: 'NV007',
        departmentId: 'dept-sales',
        role: 'TEAM_MEMBER',
        status: USER_ACCOUNT_STATUS.PENDING,
        phone: '0907000007',
        lastActivityAt: null,
      }),
      user({
        id: 'user-008',
        fullName: 'Bùi Ngọc Linh',
        email: 'linh.bui@company.com',
        employeeCode: 'NV008',
        departmentId: 'dept-ops',
        role: 'PROJECT_MANAGER',
        status: USER_ACCOUNT_STATUS.ACTIVE,
        phone: '0908000008',
        lastActivityAt: '2025-06-23T18:00:00',
      }),
    ],
  },
]

export function getDepartmentName(departmentId) {
  return DEPT_MAP[departmentId] ?? '—'
}

export const MOCK_USER_ACTIVITIES = {
  'user-001': [
    {
      id: 'act-001-1',
      action: 'LOGIN',
      description: 'Đăng nhập hệ thống',
      occurredAt: '2025-06-26T08:30:00',
    },
    {
      id: 'act-001-2',
      action: 'UPDATE_PROFILE',
      description: 'Cập nhật thông tin cá nhân',
      occurredAt: '2025-06-25T14:20:00',
    },
    {
      id: 'act-001-3',
      action: 'VIEW_WORKSPACE',
      description: 'Truy cập Acme Workspace',
      occurredAt: '2025-06-25T09:10:00',
    },
  ],
  'user-002': [
    {
      id: 'act-002-1',
      action: 'LOGIN',
      description: 'Đăng nhập hệ thống',
      occurredAt: '2025-06-26T07:15:00',
    },
    {
      id: 'act-002-2',
      action: 'MANAGE_PROJECT',
      description: 'Cập nhật sprint planning',
      occurredAt: '2025-06-24T16:00:00',
    },
  ],
  'user-005': [
    {
      id: 'act-005-1',
      action: 'ACCOUNT_LOCKED',
      description: 'Tài khoản bị khóa bởi quản trị viên',
      occurredAt: '2025-06-10T14:30:00',
    },
    {
      id: 'act-005-2',
      action: 'LOGIN',
      description: 'Đăng nhập lần cuối trước khi khóa',
      occurredAt: '2025-06-10T14:25:00',
    },
  ],
  'user-006': [
    {
      id: 'act-006-1',
      action: 'LOGIN',
      description: 'Đăng nhập hệ thống',
      occurredAt: '2025-06-26T10:00:00',
    },
    {
      id: 'act-006-2',
      action: 'MANAGE_USER',
      description: 'Xem danh sách người dùng',
      occurredAt: '2025-06-26T09:45:00',
    },
  ],
}

export const MOCK_USER_STATUS_HISTORY = {
  'user-005': [
    {
      id: 'sh-005-1',
      status: USER_ACCOUNT_STATUS.ACTIVE,
      changedAt: '2024-03-01T10:00:00',
      changedBy: 'System Admin',
      note: 'Tài khoản được tạo',
    },
    {
      id: 'sh-005-2',
      status: USER_ACCOUNT_STATUS.LOCKED,
      changedAt: '2025-06-10T14:30:00',
      changedBy: 'Nguyễn Văn Admin',
      note: 'Khóa do vi phạm chính sách bảo mật',
    },
  ],
  'user-007': [
    {
      id: 'sh-007-1',
      status: USER_ACCOUNT_STATUS.PENDING,
      changedAt: '2025-06-20T11:00:00',
      changedBy: 'System Admin',
      note: 'Tài khoản chờ kích hoạt email',
    },
  ],
}

export function getMockUserActivities(userId) {
  return MOCK_USER_ACTIVITIES[userId] ?? []
}

export function getMockUserStatusHistory(userId) {
  return MOCK_USER_STATUS_HISTORY[userId] ?? []
}
