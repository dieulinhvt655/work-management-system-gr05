import * as Tooltip from '@radix-ui/react-tooltip'

export default function IconButton({
  label,
  onClick,
  variant = 'default',
  disabled = false,
  children,
  className = '',
}) {
  return (
    <Tooltip.Root delayDuration={300}>
      <Tooltip.Trigger asChild>
        <button
          type="button"
          className={`icon-btn icon-btn--${variant}${className ? ` ${className}` : ''}`}
          onClick={onClick}
          disabled={disabled}
          aria-label={label}
        >
          {children}
        </button>
      </Tooltip.Trigger>
      <Tooltip.Portal>
        <Tooltip.Content className="icon-btn-tooltip" sideOffset={6}>
          {label}
          <Tooltip.Arrow className="icon-btn-tooltip__arrow" />
        </Tooltip.Content>
      </Tooltip.Portal>
    </Tooltip.Root>
  )
}

export function TooltipProvider({ children }) {
  return (
    <Tooltip.Provider skipDelayDuration={100}>{children}</Tooltip.Provider>
  )
}
