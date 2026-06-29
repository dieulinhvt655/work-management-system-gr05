/** Generate a temporary initial password for admin-created accounts. */
export function generateInitialPassword(username) {
  const normalized = username?.trim() || 'user'
  return `${normalized}@123`
}
