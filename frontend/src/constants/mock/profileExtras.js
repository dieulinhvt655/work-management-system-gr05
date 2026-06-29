/** Editable profile fields not in users table (mock until DB migration). */
export const MOCK_PROFILE_EXTRAS = {
  'owner@company.com': {
    bio: 'Quản lý workspace và cơ cấu tổ chức.',
    personalLink: 'https://linkedin.com/in/owner-demo',
  },
  'leader@company.com': {
    bio: 'Team Leader — phụ trách phân bổ nguồn lực và triển khai dự án.',
    personalLink: 'https://github.com/leader-demo',
  },
  'pm@company.com': {
    bio: 'Project Manager — điều phối backlog, sprint và tiến độ dự án.',
    personalLink: '',
  },
  'member@company.com': {
    bio: 'Project Contributor — thực hiện task và cập nhật tiến độ công việc.',
    personalLink: '',
  },
  'admin@company.com': {
    bio: 'System Administrator.',
    personalLink: '',
  },
}

export const MOCK_PROFILE_PASSWORD = 'password123'
