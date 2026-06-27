export const PROJECT_STATUS_LABELS = {
  DRAFT: 'Draft',
  ACTIVE: 'Active',
  COMPLETED: 'Completed',
  ARCHIVED: 'Archived',
}

export const MOCK_PROJECTS = [
  {
    id: 'proj-001',
    code: 'PRJ-Q4-ALPHA',
    name: 'Q4 Alpha Launch',
    description: 'Ra mắt sản phẩm quý 4 với luồng onboarding mới.',
    status: 'ACTIVE',
    teamId: 'team-eng',
    teamName: 'Engineering',
    managerName: 'Nguyễn PM',
    progress: 72,
    taskCount: 48,
    memberCount: 8,
  },
  {
    id: 'proj-002',
    code: 'PRJ-MOBILE-V2',
    name: 'Mobile App v2',
    description: 'Tái thiết kế UI và tích hợp thanh toán.',
    status: 'ACTIVE',
    teamId: 'team-eng',
    teamName: 'Engineering',
    managerName: 'Trần PM',
    progress: 45,
    taskCount: 32,
    memberCount: 5,
  },
  {
    id: 'proj-003',
    code: 'PRJ-HR-PORTAL',
    name: 'HR Portal',
    description: 'Cổng thông tin nội bộ cho phòng Nhân sự.',
    status: 'DRAFT',
    teamId: 'team-hr',
    teamName: 'Human Resources',
    managerName: 'Lê PM',
    progress: 12,
    taskCount: 6,
    memberCount: 3,
  },
  {
    id: 'proj-004',
    code: 'PRJ-MKT-Q2',
    name: 'Marketing Campaign Q2',
    description: 'Chiến dịch marketing quý 2.',
    status: 'COMPLETED',
    teamId: 'team-mkt',
    teamName: 'Marketing',
    managerName: 'Phạm PM',
    progress: 100,
    taskCount: 24,
    memberCount: 4,
  },
]

export function getMockProjectById(projectId) {
  return MOCK_PROJECTS.find((project) => project.id === projectId) ?? null
}
