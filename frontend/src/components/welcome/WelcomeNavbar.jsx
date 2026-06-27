import { Link } from 'react-router-dom'
import { Layers } from 'lucide-react'

export default function WelcomeNavbar() {
  return (
    <header className="welcome-nav">
      <Link to="/" className="welcome-nav__brand">
        <span className="welcome-nav__logo" aria-hidden="true">
          <Layers size={18} strokeWidth={2.25} />
        </span>
        TaskFlow
      </Link>
    </header>
  )
}
