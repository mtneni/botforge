import { useState, useEffect, useMemo } from 'react';
import { api } from '../../api/client';
import { X, Layers, Maximize2, Minimize2, Search, FileText } from 'lucide-react';

interface Chunk {
    index: number;
    text: string;
    metadata: Record<string, any>;
}

interface ChunkVisualizationProps {
    uri: string;
    onClose: () => void;
}

export function ChunkVisualization({ uri, onClose }: ChunkVisualizationProps) {
    const [chunks, setChunks] = useState<Chunk[]>([]);
    const [loading, setLoading] = useState(true);
    const [expandedIdx, setExpandedIdx] = useState<number | null>(null);
    const [query, setQuery] = useState('');

    useEffect(() => {
        api.get<{ chunks: Chunk[] }>(`/api/documents/chunks?uri=${encodeURIComponent(uri)}`)
            .then(data => {
                setChunks(data.chunks || []);
            })
            .catch(() => { })
            .finally(() => setLoading(false));
    }, [uri]);

    const filtered = useMemo(() => {
        if (!query.trim()) return chunks;
        return chunks.filter(c => c.text.toLowerCase().includes(query.toLowerCase()));
    }, [chunks, query]);

    return (
        <div className="chunk-visualization">
            <div className="chunk-viz-header">
                <div className="chunk-viz-title">
                    <Layers size={16} />
                    <span>Semantic Fragments</span>
                </div>
                <button className="chunk-viz-close" onClick={onClose}>
                    <X size={16} />
                </button>
            </div>

            <div className="chunk-viz-body">
                <div className="chunk-viz-meta-row">
                    <p className="chunk-viz-uri"><FileText size={12} /> {uri}</p>
                    <div className="chunk-search-wrapper">
                        <Search size={14} className="search-icon" />
                        <input
                            type="text"
                            placeholder="Search fragments..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                        />
                    </div>
                </div>

                {loading ? (
                    <div className="chunk-skeleton">
                        {[1, 2, 3].map(i => <div key={i} className="skeleton skeleton-card" />)}
                    </div>
                ) : filtered.length === 0 ? (
                    <div className="drawer-empty">
                        {chunks.length === 0 ? 'No fragments found.' : 'No fragments match your search.'}
                    </div>
                ) : (
                    <div className="chunk-list">
                        {filtered.map((chunk, idx) => (
                            <div key={idx} className={`chunk-card ${expandedIdx === idx ? 'expanded' : ''}`}>
                                <div className="chunk-card-header">
                                    <div className="chunk-info-chip">
                                        <span className="chunk-index">#{chunk.index}</span>
                                        <span className="chunk-stats-label">
                                            {chunk.text.length} chars • {chunk.text.split(/\s+/).length} words
                                        </span>
                                    </div>
                                    <button
                                        className="chunk-expand-btn"
                                        onClick={() => setExpandedIdx(expandedIdx === idx ? null : idx)}
                                    >
                                        {expandedIdx === idx ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
                                    </button>
                                </div>
                                <div className="chunk-text">
                                    {chunk.text}
                                </div>
                                {expandedIdx === idx && chunk.metadata && Object.keys(chunk.metadata).length > 0 && (
                                    <div className="chunk-metadata">
                                        <h4>Contextual Metadata</h4>
                                        <div className="urbot-table-container">
                                            <table className="urbot-table">
                                                <thead>
                                                    <tr>
                                                        <th>Property</th>
                                                        <th>Value</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {Object.entries(chunk.metadata).map(([key, val]) => (
                                                        <tr key={key}>
                                                            <td><code>{key}</code></td>
                                                            <td>{typeof val === 'object' ? JSON.stringify(val) : String(val)}</td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
