import { useState, useEffect, useCallback, type ChangeEvent } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import { api } from '../../api/client'
import {
    X,
    Brain,
    Users,
    FileText,
    Upload,
    Link,
    Trash2,
    RefreshCw,
    Sparkles,
    Eraser,
    Database,
    Layers,
    List,
    Network
} from 'lucide-react'
import { ChunkVisualization } from '../chat/ChunkVisualization'
import { KnowledgeGraph } from '../chat/KnowledgeGraph'

type Tab = 'memory' | 'entities' | 'documents' | 'upload' | 'url'
type ViewMode = 'list' | 'graph'

interface UserDataDialogProps {
    onClose: () => void
}

export function UserDataDialog({ onClose }: UserDataDialogProps) {
    const { user } = useAuth()
    const toast = useToast()
    const [tab, setTab] = useState<Tab>('memory')
    const [loading, setLoading] = useState(false)

    // --- Data ---
    const [propositions, setPropositions] = useState<any[]>([])
    const [entities, setEntities] = useState<any[]>([])
    const [documents, setDocuments] = useState<any[]>([])
    const [docStats, setDocStats] = useState<any>(null)
    const [contexts, setContexts] = useState<string[]>([])
    const [context, setContext] = useState('')
    const [url, setUrl] = useState('')
    const [viewingDocChunks, setViewingDocChunks] = useState<string | null>(null)
    const [viewMode, setViewMode] = useState<ViewMode>('list')

    // Load contexts
    useEffect(() => {
        api.get('/api/contexts').then((data: any) => {
            setContexts(data.contexts || [])
            setContext(data.current || '')
        }).catch(() => { })
    }, [])

    // Load tab data
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
        } catch {
            // Silently handle errors
        } finally {
            setLoading(false)
        }
    }, [tab])

    useEffect(() => { loadData() }, [loadData])

    // Context switch
    const switchContext = async (newCtx: string) => {
        try {
            await api.put('/api/contexts/current', { context: newCtx })
            setContext(newCtx)
            toast.success(`Switched to ${newCtx}`)
            loadData()
        } catch { toast.error('Failed to switch context') }
    }

    // Memory actions
    const handleLearn = async () => {
        try {
            await api.post('/api/memory/learn')
            toast.success('Learning from conversation…')
            setTimeout(loadData, 2000)
        } catch { toast.error('Learn failed') }
    }

    const handleAnalyze = async () => {
        try {
            await api.post('/api/memory/analyze')
            toast.success('Analyzing memory…')
        } catch { toast.error('Analysis failed') }
    }

    const handleClearMemory = async () => {
        if (!confirm('Delete all memory propositions?')) return
        try {
            await api.del('/api/memory/clear')
            toast.success('Memory cleared')
            setPropositions([])
        } catch { toast.error('Failed to clear memory') }
    }

    // Document actions
    const handleDeleteDoc = async (docName: string) => {
        try {
            await api.del(`/api/documents?uri=${encodeURIComponent(docName)}`)
            toast.success('Document deleted')
            loadData()
        } catch { toast.error('Delete failed') }
    }

    const handleUpload = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return
        try {
            const res = await api.upload<any>('/api/documents/upload?context=personal', file)
            toast.success(res.message || `Uploaded "${file.name}"`)
            e.target.value = ''
            if (tab === 'documents') loadData()
        } catch { toast.error('Upload failed') }
    }

    const handleUrlIngest = async () => {
        if (!url.trim()) return
        try {
            await api.post('/api/documents/url?context=personal', { url })
            toast.success('URL ingested')
            setUrl('')
        } catch { toast.error('Ingest failed') }
    }

    const tabs: { id: Tab; icon: typeof Brain; label: string }[] = [
        { id: 'memory', icon: Brain, label: 'Memory' },
        { id: 'entities', icon: Users, label: 'Entities' },
        { id: 'documents', icon: FileText, label: 'Docs' },
        { id: 'upload', icon: Upload, label: 'Upload' },
        { id: 'url', icon: Link, label: 'URL' },
    ]

    return (
        <>
            <div className="modal-overlay" onClick={onClose} />
            <div className="modal-panel">
                <div className="modal-header">
                    <h2 className="modal-title">Your Data</h2>
                    <button className="modal-close-btn" onClick={onClose}><X size={20} /></button>
                </div>

                {/* Context selector */}
                {contexts.length > 1 && (
                    <div style={{ padding: '0 20px' }}>
                        <div className="context-selector">
                            <label>Context</label>
                            <select
                                className="context-select"
                                value={context}
                                onChange={(e) => switchContext(e.target.value)}
                            >
                                {contexts.map((c) => <option key={c} value={c}>{c}</option>)}
                            </select>
                        </div>
                    </div>
                )}

                <div className="modal-tabs">
                    {tabs.map((t) => (
                        <button
                            key={t.id}
                            className={`modal-tab ${tab === t.id ? 'active' : ''}`}
                            onClick={() => setTab(t.id)}
                        >
                            <t.icon size={16} />
                            {t.label}
                        </button>
                    ))}
                </div>

                <div className="modal-body">
                    {/* Loading skeleton */}
                    {loading && (
                        <div className="drawer-skeleton">
                            {[1, 2, 3].map((i) => (
                                <div key={i} className="drawer-skeleton-card">
                                    <div className="skeleton skeleton-line long" />
                                    <div className="skeleton skeleton-line medium" />
                                    <div className="skeleton skeleton-line short" />
                                </div>
                            ))}
                        </div>
                    )}

                    {!loading && tab === 'memory' && (
                        <>
                            <div className="memory-actions" style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div className="memory-view-toggle flex gap-2">
                                    <button
                                        className={`secondary-btn icon-only ${viewMode === 'list' ? 'active' : ''}`}
                                        onClick={() => setViewMode('list')}
                                        title="List View"
                                    >
                                        <List size={14} />
                                    </button>
                                    <button
                                        className={`secondary-btn icon-only ${viewMode === 'graph' ? 'active' : ''}`}
                                        onClick={() => setViewMode('graph')}
                                        title="Graph View"
                                    >
                                        <Network size={14} />
                                    </button>
                                </div>
                                <div className="flex gap-2">
                                    <button className="memory-action-btn" onClick={loadData}>
                                        <RefreshCw size={13} /> Refresh
                                    </button>
                                    <button className="memory-action-btn danger" onClick={handleClearMemory}>
                                        <Eraser size={13} /> Clear All
                                    </button>
                                </div>
                            </div>

                            {viewMode === 'graph' ? (
                                <div style={{ minHeight: '400px', borderRadius: '8px', overflow: 'hidden', border: '1px solid var(--color-glass-border)' }}>
                                    <KnowledgeGraph contextId={context === 'global' ? undefined : context} height={400} />
                                </div>
                            ) : propositions.length === 0 ? (
                                <div className="drawer-empty">
                                    <div className="drawer-empty-icon"><Brain size={48} /></div>
                                    <p>No memories yet — start chatting to build your knowledge base.</p>
                                </div>
                            ) : (
                                <div className="proposition-list">
                                    {propositions.map((p: any, i: number) => (
                                        <div key={i} className="proposition-card">
                                            <div className="proposition-text">{p.text}</div>
                                            <div className="proposition-meta">
                                                {p.confidence != null && (
                                                    <span className={`proposition-confidence ${p.confidence > 70 ? 'confidence-high' :
                                                        p.confidence > 40 ? 'confidence-medium' : 'confidence-low'
                                                        }`}>
                                                        {Math.round(p.confidence)}%
                                                    </span>
                                                )}
                                                {p.mentions?.map((m: any, mi: number) => (
                                                    <span key={mi} className="mention-tag">{typeof m === 'string' ? m : (m.span || m.name || 'Entity')}</span>
                                                ))}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                    {!loading && tab === 'entities' && (
                        entities.length === 0 ? (
                            <div className="drawer-empty">
                                <div className="drawer-empty-icon"><Users size={32} /></div>
                                No entities discovered yet.
                            </div>
                        ) : (
                            <div className="entity-grid">
                                {entities.map((e: any) => (
                                    <div key={e.id} className="entity-card">
                                        <div className="entity-name">{e.name}</div>
                                        <div className="entity-labels">
                                            {e.labels?.map((l: string) => (
                                                <span key={l} className="entity-label">{l}</span>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )
                    )}

                    {!loading && tab === 'documents' && (
                        <>
                            {docStats && (
                                <div className="doc-stats">
                                    <div className="doc-stat">
                                        Documents <span className="doc-stat-value">{docStats.documentCount || 0}</span>
                                    </div>
                                    <div className="doc-stat">
                                        Chunks <span className="doc-stat-value">{docStats.chunkCount || 0}</span>
                                    </div>
                                </div>
                            )}
                            {documents.length === 0 ? (
                                <div className="drawer-empty">
                                    <div className="drawer-empty-icon"><FileText size={32} /></div>
                                    No personal documents. Upload some to get started.
                                </div>
                            ) : (
                                <div className="doc-list">
                                    {documents.map((d: any, i: number) => (
                                        <div key={i} className="doc-item">
                                            <FileText size={16} className="doc-icon" />
                                            <div className="doc-info">
                                                <div className="doc-name">{d.title || d.uri}</div>
                                                {d.chunkCount != null && <div className="doc-meta">{d.chunkCount} chunks</div>}
                                            </div>
                                            <button
                                                className="doc-delete-btn"
                                                onClick={() => setViewingDocChunks(d.uri)}
                                                title="View Chunks"
                                                style={{ marginRight: 4 }}
                                            >
                                                <Layers size={14} />
                                            </button>
                                            <button
                                                className="doc-delete-btn"
                                                onClick={() => handleDeleteDoc(d.uri)}
                                                title="Delete"
                                            >
                                                <Trash2 size={14} />
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                    {!loading && tab === 'upload' && (
                        <div className="upload-zone">
                            <input
                                type="file"
                                className="upload-input"
                                onChange={handleUpload}
                                accept=".pdf,.txt,.md,.docx,.csv,.json"
                            />
                            <div className="upload-zone-icon"><Upload size={28} /></div>
                            <div className="upload-zone-text">
                                Drop a file here or click to upload
                            </div>
                            <div className="upload-zone-hint">
                                PDF, TXT, Markdown, DOCX, CSV, JSON
                            </div>
                        </div>
                    )}

                    {!loading && tab === 'url' && (
                        <div>
                            <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
                                Ingest a web page into your personal knowledge base.
                            </p>
                            <div className="url-ingest-form">
                                <input
                                    type="url"
                                    className="url-input"
                                    value={url}
                                    onChange={(e) => setUrl(e.target.value)}
                                    placeholder="https://example.com/article"
                                    onKeyDown={(e) => e.key === 'Enter' && handleUrlIngest()}
                                />
                                <button
                                    className="url-submit-btn"
                                    onClick={handleUrlIngest}
                                    disabled={!url.trim()}
                                >
                                    Ingest
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                {viewingDocChunks && (
                    <ChunkVisualization
                        uri={viewingDocChunks}
                        onClose={() => setViewingDocChunks(null)}
                    />
                )}
            </div>
        </>
    )
}
