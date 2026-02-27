import { useState, useEffect, useCallback, useMemo, type ChangeEvent } from 'react'
import { useToast } from '../hooks/useToast'
import { api } from '../api/client'
import {
    FileText,
    Upload,
    Link,
    Trash2,
    RefreshCw,
    ExternalLink,
    BookOpen,
    Layers,
    Search,
    Globe,
    Cpu,
    Database,
    FilePlus,
    Plus,
    X,
    Network
} from 'lucide-react'
import { ChunkVisualization } from '../components/chat/ChunkVisualization'
import { KnowledgeGraph } from '../components/chat/KnowledgeGraph'
import '../styles/resource-pages.css'

type Tab = 'documents' | 'upload' | 'url' | 'schema' | 'graph'

export function KnowledgePage() {
    const toast = useToast()
    const [tab, setTab] = useState<Tab>('documents')
    const [loading, setLoading] = useState(false)
    const [searchQuery, setSearchQuery] = useState('')

    const [documents, setDocuments] = useState<any[]>([])
    const [docStats, setDocStats] = useState<any>(null)
    const [schema, setSchema] = useState<any[]>([])
    const [url, setUrl] = useState('')
    const [viewingDocChunks, setViewingDocChunks] = useState<string | null>(null)

    const loadData = useCallback(async () => {
        setLoading(true)
        try {
            if (tab === 'documents') {
                const [docs, stats] = await Promise.all([
                    api.get<any[]>('/api/documents?context=global'),
                    api.get<any>('/api/documents/stats?context=global'),
                ])
                setDocuments(docs || [])
                setDocStats(stats)
            } else if (tab === 'schema') {
                const data = await api.get<{ types: any[] }>('/api/schema')
                setSchema(data.types || [])
            }
        } catch { /* ignore */ } finally {
            setTimeout(() => setLoading(false), 300) // Aesthetic delay
        }
    }, [tab])

    useEffect(() => { loadData() }, [loadData])

    const handleDeleteDoc = async (docName: string) => {
        if (!confirm('Are you sure you want to remove this document from the knowledge base?')) return
        try {
            await api.del(`/api/documents?uri=${encodeURIComponent(docName)}`)
            toast.success('Document deleted successfully')
            loadData()
        } catch { toast.error('Failed to delete document') }
    }

    const handleUpload = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return
        try {
            const res = await api.upload<any>('/api/documents/upload?context=global', file)
            toast.success(res.message || `Successfully uploaded "${file.name}"`)
            e.target.value = ''
            setTab('documents')
            loadData()
        } catch { toast.error('Upload failed. Please check file format.') }
    }

    const handleUrlIngest = async () => {
        if (!url.trim()) return
        try {
            await api.post('/api/documents/url?context=global', { url })
            toast.success('Website crawling initiated')
            setUrl('')
            setTab('documents')
            loadData()
        } catch { toast.error('Crawl failed. The URL might be unreachable.') }
    }

    const filteredDocs = useMemo(() => {
        if (!searchQuery.trim()) return documents
        return documents.filter(d => (d.title || d.uri).toLowerCase().includes(searchQuery.toLowerCase()))
    }, [documents, searchQuery])

    const tabs: { id: Tab; icon: any; label: string }[] = [
        { id: 'documents', icon: Database, label: 'Repository' },
        { id: 'graph', icon: Network, label: 'Graph' },
        { id: 'upload', icon: Upload, label: 'Upload' },
        { id: 'url', icon: Link, label: 'Crawl' },
        { id: 'schema', icon: BookOpen, label: 'Schema' },
    ]

    return (
        <div className="resource-page">
            <header className="resource-header">
                <div className="resource-header-inner">
                    <div className="resource-header-left">
                        <h1 className="resource-title">Knowledge Base</h1>
                        <p className="resource-subtitle">
                            Global intelligence and shared organizational context.
                            Feed the brain with documents and web data to build an interconnected knowledge graph.
                        </p>
                    </div>
                    <div className="resource-header-actions">
                        <a
                            href="http://localhost:7474"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="secondary-btn"
                        >
                            <Cpu size={14} />
                            <span>Inspect Graph</span>
                            <ExternalLink size={12} />
                        </a>
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
                        {tab === 'documents' && (
                            <div className="url-form" style={{ width: '240px' }}>
                                <div style={{ position: 'relative', width: '100%' }}>
                                    <Search size={14} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)' }} />
                                    <input
                                        type="text"
                                        placeholder="Search knowledge..."
                                        style={{ paddingLeft: '34px', paddingRight: searchQuery ? '34px' : '15px', paddingTop: '8px', paddingBottom: '8px', fontSize: '0.85rem' }}
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                    />
                                    {searchQuery && (
                                        <X
                                            size={12}
                                            style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', cursor: 'pointer', color: 'var(--color-text-muted)' }}
                                            onClick={() => setSearchQuery('')}
                                        />
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    {loading && (
                        <div className="resource-stats">
                            {[1, 2].map((i) => (
                                <div key={i} className="stat-card skeleton" style={{ height: '90px', background: 'var(--color-bg-secondary)' }} />
                            ))}
                        </div>
                    )}

                    {!loading && tab === 'documents' && (
                        <>
                            <div className="resource-stats">
                                <div className="stat-card">
                                    <span className="stat-label">Total Knowledge</span>
                                    <span className="stat-value">{docStats?.documentCount || 0} Assets</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label">Graph Density</span>
                                    <span className="stat-value">{docStats?.chunkCount || 0} Nodes</span>
                                </div>
                            </div>

                            {documents.length === 0 ? (
                                <div className="empty-state" style={{ padding: '24px' }}>
                                    <div className="upload-icon-circle" style={{ width: '64px', height: '64px' }}>
                                        <Globe size={32} />
                                    </div>
                                    <h3 style={{ fontSize: '1.1rem', marginBottom: '8px' }}>Empty Knowledge Base</h3>
                                    <p style={{ fontSize: '0.85rem', marginBottom: '16px' }}>The shared brain is structure-less. Start by indexing reference data.</p>
                                    <div style={{ display: 'flex', gap: '10px' }}>
                                        <button className="primary-btn" onClick={() => setTab('upload')} style={{ fontSize: '0.8rem', padding: '6px 12px' }}>
                                            <FilePlus size={14} />
                                            Upload
                                        </button>
                                        <button className="secondary-btn" onClick={() => setTab('url')} style={{ fontSize: '0.8rem', padding: '6px 12px' }}>
                                            <Link size={14} />
                                            Index URL
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <>
                                    <div className="data-grid">
                                        {filteredDocs.map((d: any, i: number) => (
                                            <div key={i} className="data-card">
                                                <div className="card-top-row">
                                                    <div className="card-icon"><FileText size={20} /></div>
                                                    <div className="card-actions">
                                                        <button onClick={() => setViewingDocChunks(d.uri)} title="View segments">
                                                            <Layers size={14} />
                                                        </button>
                                                        <button onClick={() => handleDeleteDoc(d.uri)} title="Remove" className="danger">
                                                            <Trash2 size={14} />
                                                        </button>
                                                    </div>
                                                </div>
                                                <div className="card-info">
                                                    <div className="card-name">{d.title || d.uri}</div>
                                                    <div className="card-meta">
                                                        {d.chunkCount || 0} semantic chunks • Shared Intelligence
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                        {filteredDocs.length === 0 && searchQuery && (
                                            <div className="empty-state" style={{ gridColumn: '1 / -1' }}>
                                                <Search size={32} />
                                                <p>No documents found matching "{searchQuery}"</p>
                                            </div>
                                        )}
                                    </div>
                                    <div style={{ display: 'flex', justifyContent: 'center', marginTop: '24px' }}>
                                        <button className="secondary-btn" onClick={loadData}>
                                            <RefreshCw size={14} />
                                            Refresh Repository
                                        </button>
                                    </div>
                                </>
                            )}
                        </>
                    )}

                    {!loading && tab === 'upload' && (
                        <div className="upload-container glass-card" style={{ padding: '32px' }}>
                            <div className="upload-box" style={{ border: 'none', background: 'transparent' }}>
                                <input
                                    type="file"
                                    className="upload-input"
                                    onChange={handleUpload}
                                    accept=".pdf,.txt,.md,.docx,.csv,.json"
                                />
                                <div className="upload-icon-circle" style={{ width: '56px', height: '56px', margin: '0 auto 16px' }}>
                                    <Upload size={28} />
                                </div>
                                <h3 style={{ fontSize: '1.25rem', marginBottom: '8px' }}>Feed the Global Brain</h3>
                                <p style={{ color: 'var(--color-text-muted)', marginBottom: '16px', fontSize: '0.9rem' }}>Index documents into the common knowledge graph.</p>
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', justifyContent: 'center', marginBottom: '24px' }}>
                                    {['PDF', 'Markdown', 'Text', 'CSV', 'JSON'].map(fmt => (
                                        <span key={fmt} className="schema-tag" style={{ fontSize: '0.65rem' }}>{fmt}</span>
                                    ))}
                                </div>
                                <button className="primary-btn" style={{ padding: '10px 20px' }}>
                                    <Plus size={16} />
                                    Choose Documents
                                </button>
                            </div>
                        </div>
                    )}

                    {!loading && tab === 'url' && (
                        <div className="url-ingest-container glass-card">
                            <div style={{ textAlign: 'center', marginBottom: '32px' }}>
                                <div className="upload-icon-circle" style={{ margin: '0 auto 24px' }}>
                                    <Link size={40} />
                                </div>
                                <h3 style={{ fontSize: '1.5rem', marginBottom: '12px' }}>Web Crawling</h3>
                                <p style={{ color: 'var(--color-text-muted)' }}>BotForge will analyze the URL, extract content, and semantically link it to your existing intelligence.</p>
                            </div>
                            <div className="url-form">
                                <input
                                    type="url"
                                    value={url}
                                    onChange={(e) => setUrl(e.target.value)}
                                    placeholder="https://docs.example.com/api-reference"
                                    onKeyDown={(e) => e.key === 'Enter' && handleUrlIngest()}
                                />
                                <button
                                    className="primary-btn"
                                    onClick={handleUrlIngest}
                                    disabled={!url.trim()}
                                >
                                    Crawl & Map
                                </button>
                            </div>
                        </div>
                    )}

                    {!loading && tab === 'schema' && (
                        <div className="schema-view">
                            <div className="stat-card" style={{ marginBottom: '32px', minHeight: 'auto', padding: '16px 24px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                    <BookOpen size={24} className="text-accent" />
                                    <span>Currently tracking <strong>{schema.length}</strong> semantic entity types in the Knowledge Graph.</span>
                                </div>
                            </div>

                            {schema.length === 0 ? (
                                <div className="empty-state">
                                    <Database size={48} />
                                    <h3>Structure-less Graph</h3>
                                    <p>Add data to allow the engine to extract entities and establish relationships.</p>
                                </div>
                            ) : (
                                <div className="data-grid" style={{ gridTemplateColumns: '1fr' }}>
                                    {schema.map((type: any, i: number) => (
                                        <div key={i} className="glass-card" style={{ marginBottom: '24px' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
                                                <div>
                                                    <span className="schema-tag" style={{ fontSize: '1.1rem', padding: '6px 16px' }}>
                                                        {type.label || type.name}
                                                    </span>
                                                    {type.description && <p className="card-meta" style={{ marginTop: '12px', fontSize: '1rem' }}>{type.description}</p>}
                                                </div>
                                            </div>

                                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px' }}>
                                                {type.properties?.length > 0 && (
                                                    <div className="urbot-table-container">
                                                        <table className="urbot-table">
                                                            <thead>
                                                                <tr>
                                                                    <th>Property</th>
                                                                    <th>Data Type</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                {type.properties.map((p: any) => (
                                                                    <tr key={p.name}>
                                                                        <td style={{ fontWeight: '600' }}><code>{p.name}</code></td>
                                                                        <td><span className="schema-tag">{p.type || 'string'}</span></td>
                                                                    </tr>
                                                                ))}
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                )}

                                                {type.relationships?.length > 0 && (
                                                    <div className="urbot-table-container">
                                                        <table className="urbot-table">
                                                            <thead>
                                                                <tr>
                                                                    <th>Relationship</th>
                                                                    <th>Target Entity</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                {type.relationships.map((r: any) => (
                                                                    <tr key={r.name}>
                                                                        <td style={{ fontWeight: '600' }}><code>{r.name}</code></td>
                                                                        <td>
                                                                            <span className="schema-tag rel">{r.targetType}</span>
                                                                        </td>
                                                                    </tr>
                                                                ))}
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {!loading && tab === 'graph' && (
                        <div className="graph-view" style={{ animation: 'fadeIn 0.5s ease-out' }}>
                            <div className="stat-card" style={{ marginBottom: '24px', minHeight: 'auto', padding: '16px 24px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                    <Network size={24} className="text-accent" />
                                    <span>Exploring <strong>Global Context</strong>. This graph visualizes relationships between all indexed documents, entities, and memories.</span>
                                </div>
                            </div>
                            <KnowledgeGraph height={700} />
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
        </div >
    )
}
