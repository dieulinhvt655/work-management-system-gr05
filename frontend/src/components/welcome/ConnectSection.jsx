import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'

export default function ConnectSection() {
  return (
    <section className="welcome-connect">
      <div className="welcome-connect__grid">
        <div>
          <p className="welcome-section__eyebrow">CONNECTED WORKFLOW</p>
          <h2 className="welcome-section__title">
          See how work connects
          </h2>
          <p className="welcome-section__desc">
          Link tasks, teams, and goals to understand progress clearly and keep everyone moving in the same direction.
          </p>
          <Link to="/login" className="welcome-btn welcome-btn--gradient">
            Explore Workflow Graph
            <ArrowRight size={16} />
          </Link>
        </div>

        <div className="welcome-connect__visual">
          <img
            src="/work-management-illustration-no-background.png"
            alt="Work management illustration"
            className="welcome-connect__illustration"
          />
        </div>
      </div>
    </section>
  )
}
