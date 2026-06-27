import { Link } from 'react-router-dom'

export default function AppFooter() {
  return (
    <footer className="app-footer">
      <p className="app-footer__copyright">
        © {new Date().getFullYear()} TaskFlow. All rights reserved.
      </p>

      <nav className="app-footer__links" aria-label="Footer">
        <Link to="/terms" className="app-footer__link">
          Điều khoản dịch vụ
        </Link>
        <Link to="/privacy" className="app-footer__link">
          Chính sách bảo mật
        </Link>
        <Link to="/help" className="app-footer__link">
          Trợ giúp
        </Link>
      </nav>
    </footer>
  )
}
