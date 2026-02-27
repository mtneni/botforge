import { useState, useEffect, useCallback, type ChangeEvent } from 'react'
import { useToast } from '../../hooks/useToast'
import { api } from '../../api/client'
import {
    X,
    FileText,
    Upload,
    Link,
    Trash2,
    RefreshCw,
    ExternalLink,
    BookOpen,
    Layers,
} from 'lucide-react'
import { ChunkVisualization } from '../chat/ChunkVisualization'

type Tab = 'documents' | 'upload' | 'url' | 'schema'

interface KnowledgeBaseDialogProps {
    onClose: () => void
}

export function KnowledgeBaseDialog({ onClose }: KnowledgeBaseDialogProps) {
    const toast = useToast()
    const [tab, setTab] = useState<Tab>('documents')
    const [loading, setLoading] = useState(false)

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
            setLoading(false)
        }
    }, [tab])

    useEffect(() => { loadData() }, [loadData])

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
            const res = await api.upload<any>('/api/documents/upload?context=global', file)
            toast.success(res.message || `Uploaded "${file.name}"`)
            e.target.value = ''
            if (tab === 'documents') loadData()
        } catch { toast.error('Upload failed') }
    }

    const handleUrlIngest = async () => {
        if (!url.trim()) return
        try {
            await api.post('/api/documents/url?context=global', { url })
            toast.success('URL ingested')
            setUrl('')
        } catch { toast.error('Ingest failed') }
    }

    const tabs: { id: Tab; icon: typeof FileText; label: string }[] = [
        { id: 'documents', icon: FileText, label: 'Documents' },
        { id: 'upload', icon: Upload, label: 'Upload' },
        { id: 'url', icon: Link, label: 'URL' },
        { id: 'schema', icon: BookOpen, label: 'Schema' },
    ]

    return (
        <>
            <div className="modal-overlay" onClick={onClose} />
            <div className="modal-panel">
                <div className="modal-header">
                    <h2 className="modal-title">Knowledge Base</h2>
                    <button className="modal-close-btn" onClick={onClose}><X size={20} /></button>
                </div>

                {/* Neo4j link */}
                <div style={{ padding: '0 20px', paddingTop: 12 }}>
                    <a
                        href="http://localhost:7474"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="neo4j-link-row"
                    >
                        <ExternalLink size={14} />
                        Neo4j Browser
                    </a>
                </div>

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
                                    <div className="drawer-empty-icon"><FileText size={48} /></div>
                                    <p>No global documents. Upload to share across all users.</p>
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
                            <div className="memory-actions" style={{ marginBottom: 16 }}>
                                <button className="memory-action-btn" onClick={loadData}>
                                    <RefreshCw size={13} /> Refresh
                                </button>
                            </div>
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
                                Ingest a web page into the global knowledge base.
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

                    {!loading && tab === 'schema' && (
                        schema.length === 0 ? (
                            <div className="drawer-empty">
                                <div className="drawer-empty-icon"><BookOpen size={48} /></div>
                                <p>No schema types defined.</p>
                            </div>
                        ) : (
                            <div className="schema-list">
                                {schema.map((type: any, i: number) => (
                                    <div key={i} className="schema-type">
                                        <div className="schema-type-header">
                                            <div className="schema-type-name">{type.label || type.name}</div>
                                            {type.description && <div className="schema-type-desc">{type.description}</div>}
                                        </div>

                                        <div className="schema-type-content">
                                            {type.properties && type.properties.length > 0 && (
                                                <div className="schema-section">
                                                    <div className="schema-section-title">Properties</div>
                                                    <div className="schema-type-props">
                                                        {type.properties.map((prop: any) => (
                                                            <div key={prop.name} className="schema-prop">
                                                                <span className="schema-prop-name">{prop.name}</span>
                                                                <span className="schema-prop-type">{prop.type || 'string'}</span>
                                                            </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}

                                            {type.relationships && type.relationships.length > 0 && (
                                                <div className="schema-section">
                                                    <div className="schema-section-title">Relationships</div>
                                                    <div className="schema-type-props">
                                                        {type.relationships.map((rel: any) => (
                                                            <div key={rel.name} className="schema-prop">
                                                                <span className="schema-prop-name">{rel.name}</span>
                                                                <span className="schema-prop-type">→ {rel.targetType}</span>
                                                            </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )
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
