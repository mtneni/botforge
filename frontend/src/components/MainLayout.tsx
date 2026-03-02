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
        <div style={{ display: 'flex', width: '100%', height: '100vh', overflow: 'hidden' }}>
            <Sidebar
                onNewChat={startNewChat}
                conversations={conversations}
                activeConversationId={activeConversationId}
                onDeleteConversation={deleteConversation}
                onRenameConversation={renameConversation}
            />
            <div style={{ flex: 1, height: '100%', overflow: 'hidden', position: 'relative' }}>
                <Outlet />
            </div>
        </div>
    )
}
