function getInitials(name) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
}

export default function WorkspaceLogo({
  name,
  logoUrl,
  className = '',
  size = 'md',
}) {
  if (logoUrl) {
    return (
      <img
        src={logoUrl}
        alt=""
        className={`workspace-logo workspace-logo--${size}${className ? ` ${className}` : ''}`}
      />
    )
  }

  return (
    <span
      className={`workspace-logo workspace-logo--fallback workspace-logo--${size}${className ? ` ${className}` : ''}`}
      aria-hidden="true"
    >
      {getInitials(name || 'WS')}
    </span>
  )
}
