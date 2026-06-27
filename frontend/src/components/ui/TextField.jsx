export default function TextField({
  id,
  label,
  error,
  className = '',
  ...props
}) {
  return (
    <div className={`field${className ? ` ${className}` : ''}`}>
      {label && (
        <label className="field__label" htmlFor={id}>
          {label}
        </label>
      )}
      <input
        id={id}
        className={`field__input${error ? ' field__input--error' : ''}`}
        {...props}
      />
      {error && <p className="field__error">{error}</p>}
    </div>
  )
}
