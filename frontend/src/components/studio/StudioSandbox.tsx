import { useState, useRef, useEffect } from 'react'
import { Send, Terminal, RefreshCw, User, Bot, Sparkles } from 'lucide-react'
import { api } from '../../api/client'

interface Message {
    id: string
    role: 'user' | 'assistant'
    text: string
}

interface StudioSandboxProps {
    personaName: string
    objective: string
    onClose: () => void
}

export function StudioSandbox({ personaName, objective, onClose }: StudioSandboxProps) {
    const [messages, setMessages] = useState<Message[]>([])
    const [input, setInput] = useState('')
    const [loading, setLoading] = useState(false)
    const scrollRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight
        }
    }, [messages])

    const handleSend = async () => {
        if (!input.trim() || loading) return

        const userMsg: Message = { id: Date.now().toString(), role: 'user', text: input }
        setMessages(prev => [...prev, userMsg])
        setInput('')
        setLoading(true)

        try {
            // We use a dedicated dry-run endpoint that doesn't save to database
            const res = await api.post<{ reply: string }>('/api/personas/dry-run', {
                objective,
                message: input,
                history: messages.slice(-5).map(m => ({ role: m.role, text: m.text }))
            })

            const assistantMsg: Message = { id: (Date.now() + 1).toString(), role: 'assistant', text: res.reply }
            setMessages(prev => [...prev, assistantMsg])
        } catch (error) {
            setMessages(prev => [...prev, { id: 'err', role: 'assistant', text: "Neural uplink failed. Ensure the objective is valid." }])
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="glass-card animate-in" style={{ padding: 0, overflow: 'hidden', height: '500px', display: 'flex', flexDirection: 'column' }}>
            <header style={{ padding: '16px 20px', borderBottom: '1px solid var(--color-glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(255,255,255,0.02)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div className="card-icon" style={{ width: '32px', height: '32px', background: 'var(--color-accent-subtle)', color: 'var(--color-accent)' }}>
                        <Terminal size={16} />
                    </div>
                    <div>
                        <h4 style={{ fontSize: '0.9rem', margin: 0 }}>Sandbox: {personaName}</h4>
                        <p style={{ fontSize: '0.7rem', color: 'var(--color-text-muted)', margin: 0 }}>Isolated Testing Environment</p>
                    </div>
                </div>
                <button className="secondary-btn icon-only" onClick={() => setMessages([])} title="Reset Simulation">
                    <RefreshCw size={14} />
                </button>
            </header>

            <div ref={scrollRef} style={{ flex: 1, padding: '20px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {messages.length === 0 && (
                    <div style={{ textAlign: 'center', marginTop: '40px', color: 'var(--color-text-muted)' }}>
                        <Sparkles size={32} style={{ margin: '0 auto 16px', opacity: 0.3 }} />
                        <p style={{ fontSize: '0.85rem' }}>Simulation ready. Test the persona's reasoning and tone.</p>
                    </div>
                )}
                {messages.map(m => (
                    <div key={m.id} style={{ alignSelf: m.role === 'user' ? 'flex-end' : 'flex-start', maxWidth: '85%' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px', flexDirection: m.role === 'user' ? 'row-reverse' : 'row' }}>
                            {m.role === 'user' ? <User size={12} className="text-muted" /> : <Bot size={12} className="text-accent" />}
                            <span style={{ fontSize: '0.65rem', fontWeight: 700, textTransform: 'uppercase', opacity: 0.5 }}>{m.role === 'user' ? 'Subject' : personaName}</span>
                        </div>
                        <div style={{
                            padding: '10px 14px',
                            borderRadius: '14px',
                            fontSize: '0.9rem',
                            lineHeight: '1.5',
                            background: m.role === 'user' ? 'var(--color-accent)' : 'rgba(255,255,255,0.05)',
                            color: m.role === 'user' ? '#000' : '#ececec',
                            border: m.role === 'user' ? 'none' : '1px solid var(--color-glass-border)'
                        }}>
                            {m.text}
                        </div>
                    </div>
                ))}
                {loading && (
                    <div style={{ alignSelf: 'flex-start', display: 'flex', gap: '4px', padding: '12px' }}>
                        <div className="dot-pulse" style={{ width: '6px', height: '6px' }} />
                        <div className="dot-pulse" style={{ width: '6px', height: '6px', animationDelay: '0.2s' }} />
                        <div className="dot-pulse" style={{ width: '6px', height: '6px', animationDelay: '0.4s' }} />
                    </div>
                )}
            </div>

            <div style={{ padding: '16px', background: 'rgba(0,0,0,0.2)', borderTop: '1px solid var(--color-glass-border)' }}>
                <div className="url-form" style={{ padding: 0 }}>
                    <input
                        type="text"
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleSend()}
                        placeholder="Type a test prompt..."
                        style={{ background: 'rgba(255,255,255,0.03)', fontSize: '0.9rem' }}
                    />
                    <button className="primary-btn icon-only" onClick={handleSend} disabled={loading || !input.trim()}>
                        <Send size={16} />
                    </button>
                </div>
            </div>
        </div>
    )
}
