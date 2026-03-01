import React, { useState, useMemo, useEffect } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import {
    Plus,
    MessageSquare,
    Search,
    Database,
    Palette,
    LogOut,
    Trash2,
    Sparkles,
    Clock,
    User,
    PanelLeftClose,
    PanelLeftOpen,
    Edit2,
    Check,
    Shield
} from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import '../styles/sidebar.css'

interface SidebarProps {
    onNewChat: () => void
    conversations?: any[]
    activeConversationId?: string | null
    onDeleteConversation?: (id: string) => void
    onRenameConversation?: (id: string, newTitle: string) => void
}

export function Sidebar({
    onNewChat,
    conversations = [],
    activeConversationId,
    onDeleteConversation,
    onRenameConversation
}: SidebarProps) {
    const [isCollapsed, setIsCollapsed] = useState(false)
    const [searchQuery, setSearchQuery] = useState('')
    const [editingId, setEditingId] = useState<string | null>(null)
    const [editTitle, setEditTitle] = useState('')
    const { user, logout } = useAuth()
    const location = useLocation()

    // Keyboard shortcut for toggling sidebar
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'b') {
                e.preventDefault()
                setIsCollapsed(prev => !prev)
            }
        }
        window.addEventListener('keydown', handleKeyDown)
        return () => window.removeEventListener('keydown', handleKeyDown)
    }, [])

    const filteredConversations = useMemo(() => {
        if (!searchQuery.trim()) return conversations
        return conversations.filter(c =>
            c.title.toLowerCase().includes(searchQuery.toLowerCase())
        )
    }, [conversations, searchQuery])

    const toggleSidebar = () => setIsCollapsed(!isCollapsed)

    const startRename = (id: string, currentTitle: string, e: React.MouseEvent) => {
        e.preventDefault()
        e.stopPropagation()
        setEditingId(id)
        setEditTitle(currentTitle)
    }

    const submitRename = (id: string) => {
        if (editTitle.trim() && onRenameConversation) {
            onRenameConversation(id, editTitle.trim())
        }
        setEditingId(null)
    }

    const handleRenameKeyDown = (e: React.KeyboardEvent, id: string) => {
        if (e.key === 'Enter') {
            submitRename(id)
        } else if (e.key === 'Escape') {
            setEditingId(null)
        }
    }

    return (
        <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}>
            <header className="sidebar-header">
                <div className="sidebar-logo">
                    <div className="logo-icon" title="BotForge">
                        <Sparkles size={18} fill="currentColor" />
                    </div>
                    <span className="logo-text">BotForge</span>
                </div>
            </header>

            <div className="sidebar-search">
                <div className="search-wrapper">
                    <Search size={14} className="search-icon" />
                    <input
                        type="text"
                        className="search-input"
                        placeholder={isCollapsed ? "" : "Search chats..."}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        title="Search chats"
                    />
                </div>
            </div>

            <div className="sidebar-content">
                {/* Workspace Section */}
                <section className="sidebar-section">
                    {!isCollapsed && <span className="section-label">Workspace</span>}
                    <div className="nav-group">
                        <Link to="/" className="new-chat-btn-link" onClick={() => onNewChat()}>
                            <button className="new-chat-btn" title="New Chat">
                                <Plus size={18} />
                                <span>New Chat</span>
                            </button>
                        </Link>
                    </div>
                </section>

                {/* Library Section */}
                <section className="sidebar-section">
                    {!isCollapsed && (
                        <div className="section-label">
                            <span>Recent Chats</span>
                            <Clock size={12} />
                        </div>
                    )}
                    <div className="conversation-list">
                        {filteredConversations.length > 0 ? (
                            filteredConversations.map(conv => (
                                <div
                                    key={conv.id}
                                    className={`conversation-item-wrapper ${activeConversationId === conv.id ? 'active' : ''}`}
                                    title={conv.title}
                                >
                                    {editingId === conv.id ? (
                                        <div className="rename-wrapper" onClick={e => e.preventDefault()}>
                                            <input
                                                type="text"
                                                className="rename-input"
                                                value={editTitle}
                                                onChange={e => setEditTitle(e.target.value)}
                                                onKeyDown={e => handleRenameKeyDown(e, conv.id)}
                                                autoFocus
                                            />
                                            <button
                                                className="confirm-rename-btn"
                                                onClick={(e) => {
                                                    e.preventDefault()
                                                    e.stopPropagation()
                                                    submitRename(conv.id)
                                                }}
                                            >
                                                <Check size={14} />
                                            </button>
                                        </div>
                                    ) : (
                                        <Link to={`/chat/${conv.id}`} className="conversation-item">
                                            <MessageSquare size={16} />
                                            <span>{conv.title}</span>
                                        </Link>
                                    )}
                                    {!editingId && (
                                        <div className="conversation-actions">
                                            <button
                                                className="edit-conv-btn"
                                                onClick={(e) => startRename(conv.id, conv.title, e)}
                                                title="Rename chat"
                                            >
                                                <Edit2 size={14} />
                                            </button>
                                            <button
                                                className="delete-conv-btn"
                                                onClick={(e) => {
                                                    e.preventDefault()
                                                    e.stopPropagation()
                                                    onDeleteConversation?.(conv.id)
                                                }}
                                                title="Delete chat"
                                            >
                                                <Trash2 size={14} />
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))
                        ) : (
                            !isCollapsed && <div className="no-results">No chats found</div>
                        )}
                    </div>
                </section>
            </div>

            <footer className="sidebar-footer">
                {/* Resources Section moved to footer */}
                <section className="sidebar-section footer-resources">
                    {!isCollapsed && <span className="section-label">Resources</span>}
                    <div className="nav-group">
                        <NavLink to="/knowledge" className="sidebar-link" title="Knowledge Base">
                            <Database size={18} />
                            <span>Knowledge Base</span>
                        </NavLink>
                        <NavLink to="/data" className="sidebar-link" title="Your Data">
                            <User size={18} />
                            <span>Your Data</span>
                        </NavLink>
                        <NavLink to="/studio" className="sidebar-link" title="Persona Studio">
                            <Palette size={18} />
                            <span>Persona Studio</span>
                        </NavLink>
                        <NavLink to="/admin" className="sidebar-link" title="Admin Dashboard">
                            <Shield size={18} />
                            <span>Admin Center</span>
                        </NavLink>
                    </div>
                </section>

                <div className="user-profile-row">
                    <button className="user-profile-btn" title={user?.displayName || 'User Profile'}>
                        <div className="avatar-sphere">
                            {user?.displayName?.charAt(0).toUpperCase() || 'U'}
                        </div>
                        <div className="user-meta">
                            <span className="username-display">{user?.displayName || 'User'}</span>
                            <span className="user-status">Online</span>
                        </div>
                    </button>
                    <button className="logout-action-btn" onClick={logout} title="Sign Out">
                        <LogOut size={16} />
                    </button>
                </div>

                <div className="sidebar-footer-actions">
                    <button
                        className="sidebar-toggle-btn"
                        onClick={toggleSidebar}
                        title={isCollapsed ? "Expand Sidebar (Ctrl+B)" : "Close Sidebar (Ctrl+B)"}
                    >
                        {isCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
                        <span>{isCollapsed ? "Expand" : "Close Sidebar"}</span>
                    </button>
                </div>
            </footer>
        </aside>
    )
}
