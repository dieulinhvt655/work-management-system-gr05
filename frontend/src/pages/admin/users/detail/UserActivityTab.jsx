import { useOutletContext } from 'react-router-dom'
import { formatLastActivity } from '../utils/formatUserDate'

export default function UserActivityTab() {
  const { user } = useOutletContext()
  const activities = user.activities ?? []

  return (
    <section className="user-detail-tab">
      <div className="user-detail-tab__card">
        {activities.length === 0 ? (
          <p className="user-detail-tab__empty">Chưa có hoạt động nào.</p>
        ) : (
          <ul className="user-activity-timeline">
            {activities.map((activity) => (
              <li key={activity.id} className="user-activity-timeline__item">
                <span
                  className="user-activity-timeline__dot"
                  aria-hidden="true"
                />
                <div className="user-activity-timeline__content">
                  <p className="user-activity-timeline__title">
                    {activity.description}
                  </p>
                  <time
                    className="user-activity-timeline__time"
                    dateTime={activity.occurredAt}
                  >
                    {formatLastActivity(activity.occurredAt)}
                  </time>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}
