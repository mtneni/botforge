import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { useChat } from '../hooks/useChat'
import { useAuth } from '../hooks/useAuth'

export function MainLayout() {
    const {
        conversations,
        activeConversationId,
        createNewChat,
        deleteConversation,
        renameConversation
    } = useChat()

    // We don't need activeTab/onTabChange anymore if we use NavLink in Sidebar
    // But for now, we'll keep the Sidebar props compatible until we refactor it.

    return (
        <div style={{ display: 'flex', width: '100%', height: '100vh', overflow: 'hidden' }}>
            <Sidebar
                onNewChat={createNewChat}
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
