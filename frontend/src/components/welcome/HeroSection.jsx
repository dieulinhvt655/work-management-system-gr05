import { Link } from 'react-router-dom'
import heroDashboardPreview from '../../assets/images/hero-dashboard-preview.png'

export default function HeroSection() {
  return (
    <section className="welcome-hero">
      <div className="welcome-hero__content">
        <span className="welcome-hero__badge">Enterprise Work Management</span>

        <h1 className="welcome-hero__title">
          Manage work with{' '}
          <span className="welcome-hero__highlight">clarity</span> and {' '}
          <span className="welcome-hero__highlight">confidence</span>
        </h1>

        <p className="welcome-hero__subtitle">
          Plan projects, track progress, and align teams in one streamlined
          workspace. The all-in-one platform built for complex workflows.
        </p>

        <div className="welcome-hero__actions">
          <Link to="/login" className="welcome-btn welcome-btn--primary">
            Sign In
          </Link>
        </div>
      </div>

      <div className="welcome-hero__preview">
        <img
          src={heroDashboardPreview}
          alt="Sprint board dashboard preview"
          className="welcome-hero__illustration"
        />
      </div>
    </section>
  )
}
