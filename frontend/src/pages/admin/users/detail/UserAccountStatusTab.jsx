import { useOutletContext } from 'react-router-dom'
import { USER_STATUS_LABELS } from '../../../../constants/users'
import { formatLastActivity } from '../utils/formatUserDate'
import UserStatusBadge from '../components/UserStatusBadge'

export default function UserAccountStatusTab() {
  const { user } = useOutletContext()
  const history = user.statusHistory ?? []

  return (
    <section className="user-detail-tab">
      <div className="user-detail-tab__card">
        <div className="user-status-current">
          <p className="user-status-current__label">Trạng thái hiện tại</p>
          <UserStatusBadge status={user.status} />
        </div>

        <div className="user-status-history">
          <h3 className="user-status-history__title">Lịch sử thay đổi</h3>

          {history.length === 0 ? (
            <p className="user-detail-tab__empty">
              Chưa có lịch sử thay đổi trạng thái.
            </p>
          ) : (
            <ul className="user-status-history__list">
              {history.map((entry) => (
                <li key={entry.id} className="user-status-history__item">
                  <div className="user-status-history__meta">
                    <span className="user-status-history__status">
                      {USER_STATUS_LABELS[entry.status] ?? entry.status}
                    </span>
                    <time dateTime={entry.changedAt}>
                      {formatLastActivity(entry.changedAt)}
                    </time>
                  </div>
                  <p className="user-status-history__by">
                    Bởi {entry.changedBy}
                  </p>
                  {entry.note && (
                    <p className="user-status-history__note">{entry.note}</p>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
