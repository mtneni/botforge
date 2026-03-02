import { useState, useRef, useEffect, useCallback, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useChat } from '../hooks/useChat'
import { useConfig } from '../hooks/useConfig'
import { useToast } from '../hooks/useToast'
import ReactMarkdown from 'react-markdown'
import {
    Send,
    Copy,
    Check,
    Plus,
    Sparkles,
    Palette,
    Download
} from 'lucide-react'
import { api } from '../api/client'
import { UserDataDialog } from '../components/drawer/UserDrawer'
import { KnowledgeBaseDialog } from '../components/drawer/GlobalDrawer'
import { ErrorBoundary } from '../components/ErrorBoundary'
import { CodeBlock } from '../components/CodeBlock'
import { CitationList } from '../components/chat/CitationList'
import { MemoryInsight } from '../components/chat/MemoryInsight'
import { ReasoningBlock } from '../components/chat/ReasoningBlock'
import { PersonaStudio } from '../components/studio/PersonaStudio'
import '../styles/chat.css'
import { Sidebar } from '../components/Sidebar'

interface ChatMessage {
    role: 'user' | 'assistant' | 'error'
    content: string
    timestamp?: number
    citations?: any[]
    thoughts?: string[]
}


export function ChatPage() {
    const { id } = useParams<{ id: string }>()
    const { user, logout } = useAuth()
    const {
        messages,
        conversations,
        activeConversationId,
        setActiveConversationId,
        setMessages,
        sendMessage,
        progress,
        connected,
        sending,
        connect,
        disconnect,
        fetchConversations,
        loadConversation,
        deleteConversation,
        memoryInsights
    } = useChat()
    const { config } = useConfig()
    const { success } = useToast()

    const [input, setInput] = useState('')
    const [copiedId, setCopiedId] = useState<number | null>(null)
    const [activeTab, setActiveTab] = useState<'chat' | 'history' | 'data' | 'knowledge' | 'studio'>('chat')
    const [showUserDrawer, setShowUserDrawer] = useState(false)
    const [showGlobalDrawer, setShowGlobalDrawer] = useState(false)
    const [showStudio, setShowStudio] = useState(false)
    const [showScrollFab, setShowScrollFab] = useState(false)

    // Persona override state for live switching
    const [personaOverride, setPersonaOverride] = useState<string | null>(null)
    const [taglineOverride, setTaglineOverride] = useState<string | null>(null)

    const messagesEndRef = useRef<HTMLDivElement>(null)
    const messagesContainerRef = useRef<HTMLDivElement>(null)
    const textareaRef = useRef<HTMLTextAreaElement>(null)

    const persona = personaOverride || config?.displayName || config?.persona || 'Assistant'
    const tagline = taglineOverride || config?.tagline || 'Chatbot with RAG and memory'

    // Unified SSE connection
    useEffect(() => {
        connect()
        return () => disconnect()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // Sync active conversation from URL
    useEffect(() => {
        if (id && id !== activeConversationId) {
            // If we have messages already (e.g. from the first message sent), 
            // don't clear them immediately; loadConversation(id, true) will keep them.
            // But usually we want to clear if it's a URL change.
            // However, for the "first message" case, messages.length > 0 and we JUST navigated.
            loadConversation(id, messages.length > 0 && !activeConversationId);
        } else if (!id && activeConversationId) {
            setActiveConversationId(null)
            setMessages([])
        }
    }, [id, activeConversationId, loadConversation, setActiveConversationId, setMessages, messages.length])

    // Auto-scroll on new messages
    useEffect(() => {
        scrollToBottom()
    }, [messages, progress])

    const scrollToBottom = useCallback((smooth = true) => {
        messagesEndRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto' })
    }, [])

    const handleScroll = useCallback(() => {
        const el = messagesContainerRef.current
        if (!el) return
        const fromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
        setShowScrollFab(fromBottom > 200)
    }, [])

    const handleSubmit = async (e?: FormEvent) => {
        e?.preventDefault()
        const text = input.trim()
        if (!text) return

        sendMessage(text)
        setInput('')
    }

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSubmit()
        }
    }

    const handleCopy = (text: string, idx: number) => {
        navigator.clipboard.writeText(text)
        setCopiedId(idx)
        success('Copied to clipboard')
        setTimeout(() => setCopiedId(null), 2000)
    }

    const handleTabChange = (tab: any) => {
        setActiveTab(tab)
        if (tab === 'data') setShowUserDrawer(true)
        if (tab === 'knowledge') setShowGlobalDrawer(true)
        if (tab === 'studio') setShowStudio(true)
    }

    const handleExport = async () => {
        if (!id) return;
        try {
            const blob = await api.export(id);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `conversation-${id}.md`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            success('Conversation exported as Markdown');
        } catch (err) {
            console.error('Export failed:', err);
        }
    };

    const formatTime = (ts?: number) => {
        if (!ts) return ''
        return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }

    const typedMessages = messages as ChatMessage[]

    return (
        <main className="chat-main">
            <header className="chat-header">
                <div className="chat-header-left">
                    <div className="chat-branding">
                        <h1 className="chat-title">{persona}</h1>
                        <span className="chat-subtitle">
                            <span className={`connection-dot ${connected ? 'connected' : 'disconnected'}`} />
                            {tagline}
                        </span>
                    </div>
                </div>
                <div className="chat-header-right">
                    {id && (
                        <button
                            className="header-action-btn"
                            onClick={handleExport}
                            title="Export Conversation"
                        >
                            <Download size={18} />
                            <span>Export</span>
                        </button>
                    )}
                </div>
            </header>

            <div className="messages-container" ref={messagesContainerRef} onScroll={handleScroll}>
                <div className="messages-inner">
                    {typedMessages.length === 0 ? (
                        <div className="welcome-screen">
                            <h1 className="welcome-title">Hello, {user?.displayName?.split(' ')[0]}</h1>
                            <p className="welcome-tagline">How can I help you today?</p>
                            <div className="welcome-suggestions">
                                <button className="suggestion-card" onClick={() => sendMessage('Tell me about my documents')}>
                                    Understand your data with RAG analysis
                                </button>
                                <button className="suggestion-card" onClick={() => sendMessage('Who am I according to your memory?')}>
                                    Explore what I've learned about you
                                </button>
                                <button className="suggestion-card" onClick={() => sendMessage('Help me brainstorm a plan')}>
                                    Let's collaborate on your next big idea
                                </button>
                            </div>
                        </div>
                    ) : (
                        typedMessages.map((msg, idx) => (
                            <div key={idx} className={`message-group ${msg.role}`}>
                                <div className="message-group-inner">
                                    <div className={`msg-avatar ${msg.role}`}>
                                        {msg.role === 'assistant' ? (
                                            <Sparkles size={18} />
                                        ) : (
                                            user?.displayName?.charAt(0).toUpperCase()
                                        )}
                                    </div>
                                    <div className="msg-body">
                                        <div className="msg-text">
                                            {msg.role === 'assistant' && msg.thoughts && msg.thoughts.length > 0 && (
                                                <ReasoningBlock thoughts={msg.thoughts} />
                                            )}
                                            {msg.role === 'assistant' ? (
                                                <ReactMarkdown
                                                    components={{
                                                        code({ node, className, children, ...props }: any) {
                                                            const match = /language-(\w+)/.exec(className || '')
                                                            return match ? (
                                                                <CodeBlock className={className}>{children}</CodeBlock>
                                                            ) : (
                                                                <code className={className} {...props}>{children}</code>
                                                            )
                                                        }
                                                    }}
                                                >
                                                    {msg.content}
                                                </ReactMarkdown>
                                            ) : (
                                                msg.content
                                            )}
                                            {msg.role === 'assistant' && msg.citations && msg.citations.length > 0 && (
                                                <CitationList citations={msg.citations} />
                                            )}
                                        </div>
                                        {msg.role === 'assistant' && (
                                            <div className="msg-actions">
                                                <button
                                                    className={`msg-action-btn ${copiedId === idx ? 'active' : ''}`}
                                                    onClick={() => handleCopy(msg.content, idx)}
                                                >
                                                    {copiedId === idx ? <Check size={14} /> : <Copy size={14} />}
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))
                    )}

                    {memoryInsights.length > 0 && (
                        <div className="memory-insights-container">
                            {memoryInsights.map((insight) => (
                                <MemoryInsight key={insight.timestamp} proposition={insight.proposition} status={insight.status} />
                            ))}
                        </div>
                    )}

                    {(progress || sending) && (
                        <div className="message-group assistant">
                            <div className="message-group-inner">
                                <div className="msg-avatar assistant">
                                    <Sparkles size={18} className="animate-pulse" />
                                </div>
                                <div className="msg-body">
                                    <div className="tool-call-indicator">
                                        <span className="tool-call-text">{progress || 'Thinking…'}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                    <div ref={messagesEndRef} />
                </div>
            </div>

            <div className="chat-input-section">
                <div className="chat-input-container">
                    <div className="chat-textarea-wrapper">
                        <textarea
                            ref={textareaRef}
                            className="chat-textarea"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            placeholder="Message BotForge"
                            rows={1}
                        />
                        <div className="input-actions-row">
                            <div className="input-tools">
                                <button className="input-tool-btn" title="Add source"><Plus size={18} /></button>
                                <button className="input-tool-btn" title="Persona Studio" onClick={() => window.location.href = '/studio'}><Palette size={18} /></button>
                            </div>
                            <button
                                className="chat-send-btn"
                                onClick={() => handleSubmit()}
                                disabled={!input.trim() || sending}
                            >
                                <Send size={18} />
                            </button>
                        </div>
                    </div>
                    <p className="chat-input-hint">BotForge can make mistakes. Check important info.</p>
                </div>
            </div>
        </main>
    )
}
