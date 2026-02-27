import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from './hooks/useAuth'
import { ToastProvider } from './hooks/useToast'
import { App } from './App'
import './styles/theme.css'
import './styles/chat.css'
import './styles/drawer.css'
import './styles/login.css'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ToastProvider>
            <AuthProvider>
                <App />
            </AuthProvider>
        </ToastProvider>
    </StrictMode>,
)
