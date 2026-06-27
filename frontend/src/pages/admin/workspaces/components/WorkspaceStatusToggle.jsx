import { Pause, Play } from 'lucide-react'
import { WORKSPACE_STATUS } from '../../../../constants/workspaces'

const OPTIONS = [
  {
    value: WORKSPACE_STATUS.ACTIVE,
    label: 'Active',
    icon: Play,
  },
  {
    value: WORKSPACE_STATUS.INACTIVE,
    label: 'Inactive',
    icon: Pause,
  },
]

export default function WorkspaceStatusToggle({ value, onChange, error }) {
  return (
    <div className="workspace-status-toggle">
      <div className="workspace-status-toggle__options" role="group" aria-label="Trạng thái ban đầu">
        {OPTIONS.map(({ value: optionValue, label, icon: Icon }) => {
          const isActive = value === optionValue

          return (
            <button
              key={optionValue}
              type="button"
              className={`workspace-status-toggle__option${isActive ? ' workspace-status-toggle__option--active' : ''}`}
              onClick={() => onChange(optionValue)}
              aria-pressed={isActive}
            >
              <Icon size={16} aria-hidden="true" />
              {label}
            </button>
          )
        })}
      </div>
      {error && (
        <p className="field__error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
