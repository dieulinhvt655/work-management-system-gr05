function getInitials(fullName = '') {
  return fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

export default function UserAvatar({
  fullName = '',
  avatarUrl = '',
  size = 'md',
  className = '',
  ...props
}) {
  const initials = getInitials(fullName) || '?'

  if (avatarUrl) {
    return (
      <span
        className={`user-avatar user-avatar--${size} user-avatar--image${
          className ? ` ${className}` : ''
        }`}
        title={fullName}
        {...props}
      >
        <img src={avatarUrl} alt="" />
      </span>
    )
  }

  return (
    <span
      className={`user-avatar user-avatar--${size}${className ? ` ${className}` : ''}`}
      title={fullName}
      {...props}
    >
      {initials}
    </span>
  )
}

export { getInitials }
