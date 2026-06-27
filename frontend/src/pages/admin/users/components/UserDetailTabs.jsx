import { NavLink, useParams } from 'react-router-dom'
import { USER_DETAIL_TABS } from '../../../../constants/navigation/userDetailTabs'

export default function UserDetailTabs() {
  const { userId } = useParams()

  return (
    <nav className="user-detail-tabs" aria-label="User detail sections">
      {USER_DETAIL_TABS.map((tab) => (
        <NavLink
          key={tab.id}
          to={
            tab.path
              ? `/admin/users/${userId}/${tab.path}`
              : `/admin/users/${userId}`
          }
          end={!tab.path}
          className={({ isActive }) =>
            `user-detail-tabs__link${isActive ? ' user-detail-tabs__link--active' : ''}`
          }
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}
