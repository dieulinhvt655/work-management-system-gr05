import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import AppRoutes from './routes'
import './assets/styles/admin.css'
import './assets/styles/layout.css'
import './assets/styles/project.css'
import './assets/styles/users.css'
import './assets/styles/workspaces.css'
import './assets/styles/roles.css'
import './assets/styles/teams.css'
import './assets/styles/members.css'
import './assets/styles/profile.css'
import './assets/styles/workspace-owner.css'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
