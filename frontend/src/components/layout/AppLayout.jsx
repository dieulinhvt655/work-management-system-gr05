import { Outlet } from 'react-router-dom'
import { TooltipProvider } from '../ui/IconButton'
import Header from './Header'
import Sidebar from './Sidebar'
import AppFooter from './AppFooter'

export default function AppLayout() {
  return (
    <TooltipProvider>
      <div className="app-layout">
        <Sidebar />
        <div className="app-main">
          <Header />
          <main className="app-content">
            <Outlet />
          </main>
          <AppFooter />
        </div>
      </div>
    </TooltipProvider>
  )
}
