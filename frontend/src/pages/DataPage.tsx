import { useState, useEffect, useCallback, useMemo, type ChangeEvent } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { api } from '../api/client'
import {
    Brain,
    Users,
    FileText,
    Upload,
    Link,
    Trash2,
    RefreshCw,
    Eraser,
    Layers,
    Search,
    Shield,
    X,
    MessageSquare,
    Zap,
    Plus,
    Network
} from 'lucide-react'
import { ChunkVisualization } from '../components/chat/ChunkVisualization'
import { KnowledgeGraph } from '../components/chat/KnowledgeGraph'
import '../styles/resource-pages.css'

type Tab = 'memory' | 'entities' | 'documents' | 'upload' | 'url' | 'graph'

export function DataPage() {
    const { user } = useAuth()
    const toast = useToast()
    const [tab, setTab] = useState<Tab>('memory')
    const [loading, setLoading] = useState(false)
    const [searchQuery, setSearchQuery] = useState('')

    const [propositions, setPropositions] = useState<any[]>([])
    const [entities, setEntities] = useState<any[]>([])
    const [documents, setDocuments] = useState<any[]>([])
    const [docStats, setDocStats] = useState<any>(null)
    const [contexts, setContexts] = useState<string[]>([])
    const [context, setContext] = useState('')
    const [url, setUrl] = useState('')
    const [viewingDocChunks, setViewingDocChunks] = useState<string | null>(null)

    useEffect(() => {
        api.get('/api/context').then((data: any) => {
            setContexts(data.contexts || [])
            setContext(data.current || '')
        }).catch(() => { })
    }, [])

    const loadData = useCallback(async () => {
        setLoading(true)
        try {
            if (tab === 'memory') {
                const data = await api.get<{ propositions: any[] }>('/api/memory/propositions')
                setPropositions(data.propositions || [])
            } else if (tab === 'entities') {
                const data = await api.get<{ entities: any[] }>('/api/entities')
                setEntities(data.entities || [])
            } else if (tab === 'documents') {
                const [docs, stats] = await Promise.all([
                    api.get<any[]>('/api/documents?context=personal'),
                    api.get<any>('/api/documents/stats?context=personal'),
                ])
                setDocuments(docs || [])
                setDocStats(stats)
            }
        } catch { /* ignore */ } finally {
            setTimeout(() => setLoading(false), 300)
        }
    }, [tab])

    useEffect(() => { loadData() }, [loadData])

    const switchContext = async (newCtx: string) => {
        if (newCtx === context) return
        try {
            await api.post('/api/context/switch', { contextId: newCtx })
            setContext(newCtx)
            toast.success(`Active context switched to ${newCtx}`)
            loadData()
        } catch { toast.error('Failed to switch context') }
    }

    const handleClearMemory = async () => {
        if (!confirm('This will permanently delete all learned preferences and memory fragments. Continue?')) return
        try {
            await api.del('/api/memory/clear')
            toast.success('Your memory has been cleared')
            setPropositions([])
        } catch { toast.error('Failed to clear memory') }
    }

    const handleDeleteDoc = async (docName: string) => {
        if (!confirm('Delete this personal document?')) return
        try {
            await api.del(`/api/documents?uri=${encodeURIComponent(docName)}`)
            toast.success('Document removed')
            loadData()
        } catch { toast.error('Delete failed') }
    }

    const handleUpload = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return
        try {
            const res = await api.upload<any>('/api/documents/upload?context=personal', file)
            toast.success(res.message || `Uploaded "${file.name}" to private vault`)
            e.target.value = ''
            setTab('documents')
            loadData()
        } catch { toast.error('Upload failed') }
    }

    const handleUrlIngest = async () => {
        if (!url.trim()) return
        try {
            await api.post('/api/documents/url?context=personal', { url })
            toast.success('URL added to your private collection')
            setUrl('')
            setTab('documents')
            loadData()
        } catch { toast.error('Ingest failed') }
    }

    const filteredMemory = useMemo(() => {
        if (!searchQuery.trim()) return propositions
        return propositions.filter(p => p.text.toLowerCase().includes(searchQuery.toLowerCase()))
    }, [propositions, searchQuery])

    const filteredEntities = useMemo(() => {
        if (!searchQuery.trim()) return entities
        return entities.filter(e => e.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            e.labels?.some((l: string) => l.toLowerCase().includes(searchQuery.toLowerCase())))
    }, [entities, searchQuery])

    const tabs: { id: Tab; icon: any; label: string }[] = [
        { id: 'memory', icon: Brain, label: 'Memory' },
        { id: 'entities', icon: Users, label: 'Entities' },
        { id: 'graph', icon: Network, label: 'Visual Graph' },
        { id: 'documents', icon: Shield, label: 'Private Vault' },
        { id: 'upload', icon: Upload, label: 'Secure Upload' },
        { id: 'url', icon: Link, label: 'Link' },
    ]

    return (
        <div className="resource-page">
            <header className="resource-header">
                <div className="resource-header-inner">
                    <div className="resource-header-left">
                        <h1 className="resource-title">Personal Intel</h1>
                        <p className="resource-subtitle">
                            Secure, private intelligence curated from your interactions.
                            These data points are only accessible to you and influence how BotForge assists you.
                        </p>
                    </div>
                    <div className="resource-header-right" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        {contexts.length > 1 && (
                            <div className="resource-tabs" style={{ background: 'rgba(0,0,0,0.3)', padding: '4px' }}>
                                {contexts.map(c => (
                                    <button
                                        key={c}
                                        className={`resource-tab ${context === c ? 'active' : ''}`}
                                        onClick={() => switchContext(c)}
                                        style={{ fontSize: '0.8rem', padding: '4px 12px' }}
                                    >
                                        {c.toUpperCase()}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </header>

            <main className="resource-container">
                <div className="resource-content">
                    <div className="resource-tabs-container">
                        <div className="resource-tabs">
                            {tabs.map((t) => (
                                <button
                                    key={t.id}
                                    className={`resource-tab ${tab === t.id ? 'active' : ''}`}
                                    onClick={() => setTab(t.id)}
                                >
                                    <t.icon size={16} />
                                    {t.label}
                                </button>
                            ))}
                        </div>
                        {(tab === 'memory' || tab === 'entities') && (
                            <div className="url-form" style={{ width: '240px' }}>
                                <div style={{ position: 'relative', width: '100%' }}>
                                    <Search size={14} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)' }} />
                                    <input
                                        type="text"
                                        placeholder={`Search ${tab}...`}
                                        style={{ paddingLeft: '34px', paddingTop: '8px', paddingBottom: '8px', fontSize: '0.85rem' }}
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                    />
                                </div>
                            </div>
                        )}
                    </div>

                    {loading && (
                        <div className="data-grid">
                            {[1, 2, 3].map((i) => (
                                <div key={i} className="stat-card skeleton" style={{ height: '140px' }} />
                            ))}
                        </div>
                    )}

                    {!loading && tab === 'memory' && (
                        <>
                            <div className="resource-stats">
                                <div className="stat-card">
                                    <span className="stat-label">Neural Connections</span>
                                    <span className="stat-value">{propositions.length}</span>
                                </div>
                                <div className="stat-card" style={{ flexDirection: 'row', alignItems: 'center', gap: '16px' }}>
                                    <button className="secondary-btn" onClick={loadData} style={{ flex: 1 }}>
                                        <RefreshCw size={14} /> Sync
                                    </button>
                                    <button className="secondary-btn" onClick={handleClearMemory} style={{ flex: 1, borderColor: 'rgba(242, 139, 130, 0.3)', color: 'var(--color-error)' }}>
                                        <Eraser size={14} /> Wipe
                                    </button>
                                </div>
                            </div>

                            {propositions.length === 0 ? (
                                <div className="empty-state">
                                    <div className="upload-icon-circle">
                                        <Brain size={48} />
                                    </div>
                                    <h3>Total Amnesia</h3>
                                    <p>Talk to BotForge to start building a persistent memory of your preferences and history.</p>
                                </div>
                            ) : (
                                <div className="data-grid">
                                    {propositions.map((p: any, i: number) => (
                                        <div key={i} className="data-card" style={{ minHeight: '140px', justifyContent: 'space-between' }}>
                                            <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                                                <div className="card-icon" style={{ borderRadius: '50%', flexShrink: 0, width: '32px', height: '32px' }}>
                                                    <Zap size={16} />
                                                </div>
                                                <p style={{ fontSize: '0.9rem', lineHeight: '1.5', color: 'var(--color-text-primary)' }}>
                                                    {p.text}
                                                </p>
                                            </div>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px', paddingTop: '8px', borderTop: '1px solid var(--color-glass-border)' }}>
                                                {p.confidence != null && (
                                                    <span className={`schema-tag ${p.confidence > 70 ? '' : 'rel'}`} style={{ fontSize: '0.65rem' }}>
                                                        {Math.round(p.confidence)}% Confidence
                                                    </span>
                                                )}
                                                <div style={{ display: 'flex', gap: '4px' }}>
                                                    {p.mentions?.slice(0, 3).map((m: any, mi: number) => (
                                                        <span key={mi} className="schema-tag" style={{ background: 'rgba(255,255,255,0.05)', color: 'var(--color-text-muted)', border: 'none', fontSize: '0.65rem' }}>
                                                            #{typeof m === 'string' ? m : (m.span || m.name)}
                                                        </span>
                                                    ))}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                    {!loading && tab === 'entities' && (
                        <div className="entity-view">
                            <div className="stat-card" style={{ marginBottom: '32px', minHeight: 'auto', padding: '16px 24px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                    <Users size={24} className="text-accent" />
                                    <span>BotForge has indexed <strong>{entities.length}</strong> unique entities related to your profile.</span>
                                </div>
                            </div>

                            {entities.length === 0 ? (
                                <div className="empty-state">
                                    <div className="upload-icon-circle">
                                        <Users size={48} />
                                    </div>
                                    <h3>Socially Isolated</h3>
                                    <p>Entities like people, organizations, and projects will appear here as the engine learns from your chats.</p>
                                </div>
                            ) : (
                                <div className="data-grid">
                                    {filteredEntities.map((e: any) => (
                                        <div key={e.id} className="data-card">
                                            <div className="card-top-row">
                                                <div className="card-icon" style={{ background: 'rgba(155, 114, 203, 0.1)', color: '#9b72cb' }}>
                                                    <Users size={20} />
                                                </div>
                                                <div className="card-actions">
                                                    <button onClick={() => { }} title="Inspect">
                                                        <Search size={14} />
                                                    </button>
                                                </div>
                                            </div>
                                            <div className="card-info">
                                                <h3 className="card-name">{e.name}</h3>
                                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '12px' }}>
                                                    {e.labels?.map((l: string) => (
                                                        <span key={l} className="schema-tag" style={{ textTransform: 'uppercase', letterSpacing: '0.05em' }}>{l}</span>
                                                    ))}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {!loading && tab === 'documents' && (
                        <>
                            <div className="resource-stats">
                                <div className="stat-card">
                                    <span className="stat-label">Encrypted Assets</span>
                                    <span className="stat-value">{docStats?.documentCount || 0}</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label">Security Tier</span>
                                    <span className="stat-value">Level 4</span>
                                </div>
                            </div>
                            {documents.length === 0 ? (
                                <div className="empty-state">
                                    <div className="upload-icon-circle">
                                        <Shield size={48} />
                                    </div>
                                    <h3>Vault Empty</h3>
                                    <p>Store documents that should remain strictly private and excluded from global intelligence.</p>
                                    <button className="primary-btn" onClick={() => setTab('upload')}>
                                        Secure Upload
                                    </button>
                                </div>
                            ) : (
                                <div className="data-grid">
                                    {documents.map((d: any, i: number) => (
                                        <div key={i} className="data-card">
                                            <div className="card-top-row">
                                                <div className="card-icon"><Shield size={20} /></div>
                                                <div className="card-actions">
                                                    <button onClick={() => setViewingDocChunks(d.uri)} title="Inspect">
                                                        <Layers size={14} />
                                                    </button>
                                                    <button onClick={() => handleDeleteDoc(d.uri)} title="Purge" className="danger">
                                                        <Trash2 size={14} />
                                                    </button>
                                                </div>
                                            </div>
                                            <div className="card-info">
                                                <div className="card-name">{d.title || d.uri}</div>
                                                <div className="card-meta">{d.chunkCount || 0} private segments</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                    {!loading && (tab === 'upload' || tab === 'url') && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                            {tab === 'upload' && (
                                <div className="upload-container glass-card">
                                    <div className="upload-box" style={{ border: 'none', background: 'transparent' }}>
                                        <input
                                            type="file"
                                            className="upload-input"
                                            onChange={handleUpload}
                                            accept=".pdf,.txt,.md,.docx,.csv,.json"
                                        />
                                        <div className="upload-icon-circle">
                                            <Upload size={40} />
                                        </div>
                                        <h3 style={{ fontSize: '1.5rem', marginBottom: '12px' }}>Secure Persona Upload</h3>
                                        <p style={{ color: 'var(--color-text-muted)', marginBottom: '32px' }}>Files are encrypted and only usable in your personal context.</p>
                                        <button className="primary-btn">
                                            <Plus size={18} /> Choose Private File
                                        </button>
                                    </div>
                                </div>
                            )}
                            {tab === 'url' && (
                                <div className="url-ingest-container glass-card">
                                    <div style={{ textAlign: 'center', marginBottom: '32px' }}>
                                        <div className="upload-icon-circle" style={{ margin: '0 auto 24px' }}>
                                            <Link size={40} />
                                        </div>
                                        <h3 style={{ fontSize: '1.5rem', marginBottom: '12px' }}>Private Web Clipping</h3>
                                        <p style={{ color: 'var(--color-text-muted)' }}>Index reference pages into your personal vault.</p>
                                    </div>
                                    <div className="url-form">
                                        <input
                                            type="url"
                                            value={url}
                                            onChange={(e) => setUrl(e.target.value)}
                                            placeholder="https://my-private-notes.com"
                                            onKeyDown={(e) => e.key === 'Enter' && handleUrlIngest()}
                                        />
                                        <button
                                            className="primary-btn"
                                            onClick={handleUrlIngest}
                                            disabled={!url.trim()}
                                        >
                                            Secure Index
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {!loading && tab === 'graph' && (
                        <div className="graph-view" style={{ animation: 'fadeIn 0.5s ease-out' }}>
                            <div className="stat-card" style={{ marginBottom: '24px', minHeight: 'auto', padding: '16px 24px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                    <Network size={24} className="text-accent" />
                                    <span>Exploring <strong>{context || 'Personal Context'}</strong>. Relationships between your local entities, memories, and private documents.</span>
                                </div>
                            </div>
                            <KnowledgeGraph contextId={context} height={700} />
                        </div>
                    )}
                </div>
            </main>

            {viewingDocChunks && (
                <div className="modal-root">
                    <ChunkVisualization
                        uri={viewingDocChunks!}
                        onClose={() => setViewingDocChunks(null)}
                    />
                </div>
            )}
        </div>
    )
}
