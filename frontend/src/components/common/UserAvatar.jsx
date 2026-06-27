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
  size = 'md',
  className = '',
  ...props
}) {
  return (
    <span
      className={`user-avatar user-avatar--${size}${className ? ` ${className}` : ''}`}
      title={fullName}
      {...props}
    >
      {getInitials(fullName) || '?'}
    </span>
  )
}

export { getInitials }
