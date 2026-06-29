/** Mỗi thành viên chỉ được tham gia 1 dự án tại một thời điểm. */
export const MOCK_MEMBER_CURRENT_PROJECT = {
  'org-member-001': {
    id: 'proj-001',
    name: 'Q4 Alpha Launch',
    role: 'Team Leader',
    status: 'ACTIVE',
  },
  'org-member-002': {
    id: 'proj-003',
    name: 'HR Portal',
    role: 'Team Leader',
    status: 'DRAFT',
  },
  'org-member-003': {
    id: 'proj-004',
    name: 'Marketing Campaign Q2',
    role: 'Team Leader',
    status: 'COMPLETED',
  },
  'org-member-005': {
    id: 'proj-002',
    name: 'Mobile App v2',
    role: 'Contributor',
    status: 'ACTIVE',
  },
  'org-member-006': {
    id: 'proj-003',
    name: 'HR Portal',
    role: 'Project Manager',
    status: 'DRAFT',
  },
  'org-member-007': {
    id: 'proj-001',
    name: 'Q4 Alpha Launch',
    role: 'Contributor',
    status: 'ACTIVE',
  },
}

/** Lịch sử tham gia dự án (bao gồm dự án hiện tại). */
export const MOCK_MEMBER_PROJECT_HISTORY = {
  'org-member-001': [
    {
      id: 'proj-002',
      name: 'Mobile App v2',
      role: 'Contributor',
      status: 'COMPLETED',
      joinedAt: '2023-08-01T08:00:00',
      leftAt: '2024-02-15T17:00:00',
      isCurrent: false,
    },
    {
      id: 'proj-004',
      name: 'Marketing Campaign Q2',
      role: 'Team Leader',
      status: 'COMPLETED',
      joinedAt: '2024-02-20T08:00:00',
      leftAt: '2024-05-10T17:00:00',
      isCurrent: false,
    },
    {
      id: 'proj-001',
      name: 'Q4 Alpha Launch',
      role: 'Team Leader',
      status: 'ACTIVE',
      joinedAt: '2024-05-15T08:00:00',
      leftAt: null,
      isCurrent: true,
    },
  ],
  'org-member-002': [
    {
      id: 'proj-003',
      name: 'HR Portal',
      role: 'Team Leader',
      status: 'DRAFT',
      joinedAt: '2024-03-01T09:00:00',
      leftAt: null,
      isCurrent: true,
    },
  ],
  'org-member-003': [
    {
      id: 'proj-004',
      name: 'Marketing Campaign Q2',
      role: 'Team Leader',
      status: 'COMPLETED',
      joinedAt: '2024-02-12T10:30:00',
      leftAt: null,
      isCurrent: true,
    },
  ],
  'org-member-005': [
    {
      id: 'proj-001',
      name: 'Q4 Alpha Launch',
      role: 'Contributor',
      status: 'COMPLETED',
      joinedAt: '2023-11-10T08:00:00',
      leftAt: '2024-04-01T17:00:00',
      isCurrent: false,
    },
    {
      id: 'proj-002',
      name: 'Mobile App v2',
      role: 'Contributor',
      status: 'ACTIVE',
      joinedAt: '2024-04-05T08:00:00',
      leftAt: null,
      isCurrent: true,
    },
  ],
  'org-member-006': [
    {
      id: 'proj-004',
      name: 'Marketing Campaign Q2',
      role: 'Contributor',
      status: 'COMPLETED',
      joinedAt: '2024-01-10T08:00:00',
      leftAt: '2024-04-01T17:00:00',
      isCurrent: false,
    },
    {
      id: 'proj-003',
      name: 'HR Portal',
      role: 'Project Manager',
      status: 'DRAFT',
      joinedAt: '2024-04-02T09:00:00',
      leftAt: null,
      isCurrent: true,
    },
  ],
  'org-member-007': [
    {
      id: 'proj-001',
      name: 'Q4 Alpha Launch',
      role: 'Contributor',
      status: 'ACTIVE',
      joinedAt: '2024-04-15T09:30:00',
      leftAt: null,
      isCurrent: true,
    },
  ],
}

export function getMemberCurrentProject(memberId) {
  return MOCK_MEMBER_CURRENT_PROJECT[memberId] ?? null
}

export function getMemberProjectHistory(memberId) {
  return (MOCK_MEMBER_PROJECT_HISTORY[memberId] ?? []).map((entry) => ({
    ...entry,
  }))
}

export const MOCK_MEMBER_ORG_HISTORY = {
  'org-member-001': [
    {
      id: 'mhist-001',
      action: 'TEAM_ASSIGNED',
      message: 'Gán vào Marketing Digital',
      changedBy: 'Workspace Owner',
      createdAt: '2024-01-20T08:00:00',
    },
    {
      id: 'mhist-002',
      action: 'ROLE_UPDATED',
      message: 'Cập nhật chức danh: Department Manager',
      changedBy: 'Workspace Owner',
      createdAt: '2024-03-10T10:00:00',
    },
  ],
  'org-member-006': [
    {
      id: 'mhist-003',
      action: 'TEAM_TRANSFER',
      message: 'Chuyển từ Marketing Digital sang Phòng Kỹ thuật',
      changedBy: 'Workspace Owner',
      createdAt: '2024-04-02T09:00:00',
    },
  ],
}
