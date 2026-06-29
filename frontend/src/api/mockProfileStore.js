import { MOCK_USERS } from '../constants/mock/rolePermissions'
import { MOCK_PROFILE_EXTRAS, MOCK_PROFILE_PASSWORD } from '../constants/mock/profileExtras'
import {
  MOCK_USERS_GROUPED_BY_WORKSPACE,
} from '../constants/mock/usersData'
import { MOCK_ORGANIZATION_MEMBERS } from '../constants/mock/organizationMembersData'
import { MOCK_ROLE_LABELS } from '../constants/roles'
import { USER_ROLE_LABELS } from '../constants/users'

const profileExtras = { ...MOCK_PROFILE_EXTRAS }
const profileHistory = []

function delay(ms = 180) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function findAccountByEmail(email) {
  const normalized = email?.trim().toLowerCase()
  if (!normalized) return null

  for (const group of MOCK_USERS_GROUPED_BY_WORKSPACE) {
    const user = group.users.find(
      (entry) => entry.email.trim().toLowerCase() === normalized,
    )
    if (user) {
      return { user, group }
    }
  }
  return null
}

function findOrgMemberByEmail(email) {
  const normalized = email?.trim().toLowerCase()
  return MOCK_ORGANIZATION_MEMBERS.find(
    (member) => member.email.trim().toLowerCase() === normalized,
  )
}

function resolveRoleLabel(email) {
  const entry = Object.entries(MOCK_USERS).find(
    ([, profile]) => profile.email === email,
  )
  if (!entry) return '—'
  const roleKey = entry[0]
  return USER_ROLE_LABELS[roleKey] ?? MOCK_ROLE_LABELS[roleKey] ?? roleKey
}

function buildProfile(email) {
  const account = findAccountByEmail(email)
  const orgMember = findOrgMemberByEmail(email)
  const extras = profileExtras[email] ?? { bio: '', personalLink: '' }

  if (account) {
    const { user, group } = account
    return {
      id: user.id,
      fullName: user.fullName,
      username: user.email.split('@')[0],
      email: user.email,
      employeeCode: user.employeeCode,
      phone: user.phone ?? '',
      avatarUrl: user.avatarUrl ?? '',
      bio: extras.bio ?? '',
      personalLink: extras.personalLink ?? '',
      workspaceId: group.workspaceId,
      workspaceName: group.workspaceName,
      teamName: user.departmentName ?? '—',
      roleLabel: USER_ROLE_LABELS[user.role] ?? user.role,
      accountStatus: user.status,
      createdAt: user.createdAt,
      lastLoginAt: user.lastLoginAt ?? null,
    }
  }

  const mockProfile = Object.values(MOCK_USERS).find(
    (profile) => profile.email === email,
  )

  return {
    id: `profile-${email}`,
    fullName: mockProfile?.fullName ?? email,
    username: email.split('@')[0],
    email,
    employeeCode: '—',
    phone: '',
    avatarUrl: '',
    bio: extras.bio ?? '',
    personalLink: extras.personalLink ?? '',
    workspaceId: null,
    workspaceName: '—',
    teamName: orgMember ? '—' : '—',
    roleLabel: resolveRoleLabel(email),
    accountStatus: 'ACTIVE',
    createdAt: null,
    lastLoginAt: null,
  }
}

export async function mockFetchProfile(email) {
  await delay()
  if (!email) {
    throw new Error('Không xác định được người dùng')
  }
  return buildProfile(email)
}

export async function mockUpdateProfile(email, payload) {
  await delay()

  profileExtras[email] = {
    bio: payload.bio?.trim() ?? '',
    personalLink: payload.personalLink?.trim() ?? '',
  }

  const account = findAccountByEmail(email)
  if (account && payload.phone !== undefined) {
    account.user.phone = payload.phone.trim()
  }

  profileHistory.unshift({
    id: `phist-${Date.now()}`,
    action: 'PROFILE_UPDATED',
    message: 'Cập nhật thông tin cá nhân',
    createdAt: new Date().toISOString(),
  })

  return buildProfile(email)
}

export async function mockChangePassword(email, { currentPassword, newPassword }) {
  await delay()

  if (currentPassword !== MOCK_PROFILE_PASSWORD) {
    throw new Error('Mật khẩu hiện tại không chính xác')
  }

  if (!newPassword || newPassword.length < 8) {
    throw new Error('Mật khẩu mới phải có ít nhất 8 ký tự')
  }

  profileHistory.unshift({
    id: `phist-${Date.now()}`,
    action: 'PASSWORD_CHANGED',
    message: 'Đổi mật khẩu',
    createdAt: new Date().toISOString(),
  })

  return { success: true }
}

export async function mockFetchProfileHistory() {
  await delay(120)
  return profileHistory.map((entry) => ({ ...entry }))
}
