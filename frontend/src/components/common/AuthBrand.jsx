import { Layers } from 'lucide-react'

export default function AuthBrand() {
  return (
    <div className="auth-brand">
      <div className="auth-brand__logo" aria-hidden="true">
        <Layers size={18} strokeWidth={2.25} />
      </div>
      <span className="auth-brand__name">TaskFlow</span>
    </div>
  )
}
