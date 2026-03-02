import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { useChat } from '../hooks/useChat'
import { useAuth } from '../hooks/useAuth'

export function MainLayout() {
    const {
        conversations,
        activeConversationId,
        startNewChat,
        deleteConversation,
        renameConversation
    } = useChat()

    return (
        <div className="main-layout-container" style={{
            display: 'flex',
            width: '100%',
            height: '100vh',
            overflow: 'hidden',
            background: 'var(--gradient-bg-main, linear-gradient(135deg, #0f172a 0%, #1e293b 100%))'
        }}>
            <Sidebar
                onNewChat={startNewChat}
                conversations={conversations}
                activeConversationId={activeConversationId}
                onDeleteConversation={deleteConversation}
                onRenameConversation={renameConversation}
            />
            <div className="content-area" style={{
                flex: 1,
                height: '100%',
                overflow: 'hidden',
                position: 'relative',
                display: 'flex',
                flexDirection: 'column'
            }}>
                <Outlet />
            </div>
        </div>
    )
}
