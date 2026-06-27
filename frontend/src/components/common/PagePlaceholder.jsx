export default function PagePlaceholder({ title, description }) {
  return (
    <div className="page">
      <header className="page__header">
        <h1>{title}</h1>
        {description && <p className="page__description">{description}</p>}
      </header>
    </div>
  )
}
