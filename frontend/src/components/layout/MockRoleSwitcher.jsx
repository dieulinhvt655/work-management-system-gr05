import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { MOCK_ROLE_LABELS, MOCK_ROLES } from '../../constants/roles'
import { getDefaultRoute } from '../../utils/navUtils'
import { USE_MOCK_AUTH } from '../../constants/config'

export default function MockRoleSwitcher() {
  const navigate = useNavigate()
  const { switchMockRole } = useAuth()

  if (!USE_MOCK_AUTH) {
    return null
  }

  const handleChange = (event) => {
    const mockRole = event.target.value
    if (!mockRole) return

    const nextUser = switchMockRole(mockRole)
    navigate(getDefaultRoute(nextUser?.permissions), { replace: true })
  }

  return (
    <label className="header__mock-switcher">
      <span className="header__mock-label">Mock role</span>
      <select
        className="header__mock-select"
        defaultValue=""
        onChange={handleChange}
        aria-label="Chọn mock role để test quyền"
      >
        <option value="" disabled>
          Đổi role...
        </option>
        {Object.entries(MOCK_ROLES).map(([key, value]) => (
          <option key={key} value={value}>
            {MOCK_ROLE_LABELS[value]}
          </option>
        ))}
      </select>
    </label>
  )
}
