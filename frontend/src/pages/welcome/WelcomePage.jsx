import WelcomeNavbar from '../../components/welcome/WelcomeNavbar'
import HeroSection from '../../components/welcome/HeroSection'
import ConnectSection from '../../components/welcome/ConnectSection'
import AppFooter from '../../components/layout/AppFooter'
import '../../assets/styles/welcome.css'

export default function WelcomePage() {
  return (
    <div className="welcome">
      <WelcomeNavbar />
      <HeroSection />
      <ConnectSection />
      <AppFooter />
    </div>
  )
}
