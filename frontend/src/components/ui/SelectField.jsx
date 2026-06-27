export default function SelectField({
  id,
  label,
  error,
  className = '',
  children,
  ...props
}) {
  return (
    <div className={`field${className ? ` ${className}` : ''}`}>
      {label && (
        <label className="field__label" htmlFor={id}>
          {label}
        </label>
      )}
      <select
        id={id}
        className={`field__input field__select${error ? ' field__input--error' : ''}`}
        {...props}
      >
        {children}
      </select>
      {error && <p className="field__error">{error}</p>}
    </div>
  )
}
