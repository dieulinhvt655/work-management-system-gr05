import { TEAM_STATUS, TEAM_TYPE } from '../teams'

export const MOCK_TEAM_ACTIVITIES = [
  {
    id: 'act-001',
    action: 'TEAM_CREATED',
    message: 'Phòng Marketing Digital đã được tạo',
    actorName: 'Hoàng Anh',
    createdAt: '2024-06-25T14:30:00',
  },
  {
    id: 'act-002',
    action: 'TEAM_LEADER_ASSIGNED',
    message: 'Nguyễn Thu Hà được gán làm Trưởng nhóm Marketing',
    actorName: 'Hoàng Anh',
    createdAt: '2024-06-24T09:15:00',
  },
  {
    id: 'act-003',
    action: 'MEMBER_ADDED',
    message: '3 thành viên mới tham gia Phòng Kỹ thuật',
    actorName: 'Hoàng Anh',
    createdAt: '2024-06-23T16:45:00',
  },
  {
    id: 'act-004',
    action: 'TEAM_UPDATED',
    message: 'Cập nhật mô tả Phòng Chăm sóc khách hàng',
    actorName: 'Hoàng Anh',
    createdAt: '2024-06-22T11:00:00',
  },
  {
    id: 'act-005',
    action: 'TEAM_DISBANDED',
    message: 'Nhóm Dự án nội bộ đã được giải thể',
    actorName: 'Hoàng Anh',
    createdAt: '2024-06-20T08:30:00',
  },
]

export const MOCK_TEAMS = [
  {
    id: 'team-001',
    workspaceId: 'ws-demo-001',
    code: 'MK',
    name: 'Marketing Digital',
    type: TEAM_TYPE.DEPARTMENT,
    description:
      'Xây dựng hạ tầng dữ liệu, phát triển pipeline ETL và hệ thống phân tích phục vụ ra quyết định kinh doanh.',
    status: TEAM_STATUS.ACTIVE,
    memberCount: 12,
    projectCount: 8,
    openTaskCount: 45,
    leader: {
      id: 'user-leader-001',
      fullName: 'Nguyễn Thu Hà',
    },
    createdAt: '2024-01-15T08:00:00',
    updatedAt: '2024-06-20T10:00:00',
  },
  {
    id: 'team-002',
    workspaceId: 'ws-demo-001',
    code: 'DT',
    name: 'Phòng Kỹ thuật',
    type: TEAM_TYPE.DEPARTMENT,
    description:
      'Phát triển sản phẩm lõi, duy trì hệ thống backend và triển khai các tính năng mới cho nền tảng Work Management.',
    status: TEAM_STATUS.ACTIVE,
    memberCount: 5,
    projectCount: 2,
    openTaskCount: 14,
    leader: {
      id: 'user-leader-002',
      fullName: 'Trần Minh Đức',
    },
    createdAt: '2024-02-01T09:00:00',
    updatedAt: '2024-06-18T14:00:00',
  },
  {
    id: 'team-003',
    workspaceId: 'ws-demo-001',
    code: 'CS',
    name: 'Chăm sóc khách hàng',
    type: TEAM_TYPE.DEPARTMENT,
    description:
      'Tiếp nhận và xử lý yêu cầu khách hàng, theo dõi SLA và phối hợp với các phòng ban liên quan.',
    status: TEAM_STATUS.ACTIVE,
    memberCount: 18,
    projectCount: 1,
    openTaskCount: 112,
    leader: {
      id: 'user-leader-003',
      fullName: 'Lê Thị Mai',
    },
    createdAt: '2024-02-10T10:30:00',
    updatedAt: '2024-06-15T09:00:00',
  },
  {
    id: 'team-004',
    workspaceId: 'ws-demo-001',
    code: 'HR',
    name: 'Nhân sự',
    type: TEAM_TYPE.DEPARTMENT,
    description:
      'Quản lý tuyển dụng, đào tạo và phát triển nguồn nhân lực trong workspace.',
    status: TEAM_STATUS.ACTIVE,
    memberCount: 6,
    projectCount: 0,
    openTaskCount: 8,
    leader: {
      id: 'user-leader-004',
      fullName: 'Phạm Văn Hùng',
    },
    createdAt: '2024-03-05T08:00:00',
    updatedAt: '2024-06-10T11:00:00',
  },
  {
    id: 'team-005',
    workspaceId: 'ws-demo-001',
    code: 'OPS',
    name: 'Vận hành',
    type: TEAM_TYPE.TEAM,
    description:
      'Nhóm vận hành nội bộ, hỗ trợ triển khai quy trình và giám sát chất lượng dịch vụ.',
    status: TEAM_STATUS.ACTIVE,
    memberCount: 9,
    projectCount: 3,
    openTaskCount: 22,
    leader: {
      id: 'user-leader-005',
      fullName: 'Võ Thanh Bình',
    },
    createdAt: '2024-03-20T13:00:00',
    updatedAt: '2024-06-12T16:00:00',
  },
  {
    id: 'team-006',
    workspaceId: 'ws-demo-001',
    code: 'INT',
    name: 'Dự án nội bộ',
    type: TEAM_TYPE.TEAM,
    description:
      'Nhóm thử nghiệm cho các dự án ngắn hạn — đã hoàn tất và giải thể.',
    status: TEAM_STATUS.INACTIVE,
    memberCount: 0,
    projectCount: 0,
    openTaskCount: 0,
    leader: null,
    createdAt: '2023-11-01T08:00:00',
    updatedAt: '2024-06-20T08:30:00',
  },
  {
    id: 'team-007',
    workspaceId: 'ws-demo-001',
    code: 'LEG',
    name: 'Legacy Support',
    type: TEAM_TYPE.TEAM,
    description: 'Nhóm hỗ trợ hệ thống cũ — không còn hoạt động.',
    status: TEAM_STATUS.INACTIVE,
    memberCount: 0,
    projectCount: 0,
    openTaskCount: 0,
    leader: null,
    createdAt: '2023-08-15T08:00:00',
    updatedAt: '2024-05-01T10:00:00',
  },
]

export function computeTeamSummary(teams) {
  const active = teams.filter((team) => team.status === TEAM_STATUS.ACTIVE)
  const disbanded = teams.filter((team) => team.status === TEAM_STATUS.INACTIVE)

  return {
    total: teams.length,
    active: active.length,
    disbanded: disbanded.length,
    newMembersThisMonth: 12,
  }
}
