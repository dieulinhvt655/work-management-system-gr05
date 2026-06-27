export default function UserDetailRow({ label, children }) {
  return (
    <div className="user-detail-row">
      <dt className="user-detail-row__label">{label}</dt>
      <dd className="user-detail-row__value">{children}</dd>
    </div>
  )
}
