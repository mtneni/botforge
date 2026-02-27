import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ChatProvider } from './hooks/useChat'
import { useAuth } from './hooks/useAuth'
import { LoginPage } from './pages/LoginPage'
import { ChatPage } from './pages/ChatPage'
import { KnowledgePage } from './pages/KnowledgePage'
import { DataPage } from './pages/DataPage'
import { StudioPage } from './pages/StudioPage'
import { MainLayout } from './components/MainLayout'

export function App() {
    const { user, loading } = useAuth()

    if (loading) {
        return (
            <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                height: '100vh', background: 'var(--color-bg-primary)', color: 'var(--color-text-muted)',
            }}>
                Loading…
            </div>
        )
    }

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={!user ? <LoginPage /> : <Navigate to="/" />} />

                {/* Authenticated Routes */}
                <Route element={user ? <ChatProvider><MainLayout /></ChatProvider> : <Navigate to="/login" />}>
                    <Route path="/" element={<ChatPage />} />
                    <Route path="/chat/:id" element={<ChatPage />} />
                    <Route path="/knowledge" element={<KnowledgePage />} />
                    <Route path="/data" element={<DataPage />} />
                    <Route path="/studio" element={<StudioPage />} />
                </Route>

                <Route path="*" element={<Navigate to="/" />} />
            </Routes>
        </BrowserRouter>
    )
}
