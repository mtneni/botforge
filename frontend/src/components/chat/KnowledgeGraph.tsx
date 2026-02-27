import { useEffect, useState, useRef } from 'react'
import ForceGraph2D from 'react-force-graph-2d'
import { api } from '../../api/client'
import { RefreshCw, X, Maximize2, Minimize2 } from 'lucide-react'

interface GraphNode {
    id: string
    name: string
    labels: string[]
    properties: any
    val?: number
    color?: string
}

interface GraphLink {
    source: string
    target: string
    type: string
}

interface GraphData {
    nodes: GraphNode[]
    links: GraphLink[]
}

interface KnowledgeGraphProps {
    contextId?: string
    height?: number | string
}

export function KnowledgeGraph({ contextId, height = 600 }: KnowledgeGraphProps) {
    const [data, setData] = useState<GraphData>({ nodes: [], links: [] })
    const [loading, setLoading] = useState(true)
    const [fullscreen, setFullscreen] = useState(false)
    const fgRef = useRef<any>(null)

    const loadData = async () => {
        setLoading(true)
        try {
            const res = await api.get<GraphData>(`/api/graph/data${contextId ? `?contextId=${contextId}` : ''}`)

            // Post-process nodes for visualization
            const processedNodes = res.nodes.map(node => ({
                ...node,
                val: node.labels.includes('ContentRoot') ? 5 : 2,
                color: getNodeColor(node.labels)
            }))

            setData({ nodes: processedNodes, links: res.links })
        } catch (error) {
            console.error('Failed to load graph data', error)
        } finally {
            setLoading(false)
        }
    }

    const getNodeColor = (labels: string[]) => {
        if (labels.includes('ContentRoot')) return '#8ab4f8' // Blue
        if (labels.includes('Chunk')) return '#aecbfa' // Light Blue
        if (labels.includes('NamedEntity')) return '#9b72cb' // Purple
        if (labels.includes('Proposition')) return '#81c995' // Green
        return '#9aa0a6' // Gray
    }

    useEffect(() => {
        loadData()
    }, [contextId])

    const handleNodeClick = (node: any) => {
        // Center on node
        fgRef.current.centerAt(node.x, node.y, 1000)
        fgRef.current.zoom(2, 1000)
    }

    return (
        <div className={`knowledge-graph-container glass-card ${fullscreen ? 'fullscreen' : ''}`} style={{ height, position: 'relative', overflow: 'hidden' }}>
            <div className="graph-controls" style={{ position: 'absolute', top: 16, right: 16, zIndex: 10, display: 'flex', gap: 8 }}>
                <button className="secondary-btn icon-only" onClick={loadData} title="Refresh">
                    <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
                </button>
                <button className="secondary-btn icon-only" onClick={() => setFullscreen(!fullscreen)} title={fullscreen ? 'Minimize' : 'Fullscreen'}>
                    {fullscreen ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
                </button>
            </div>

            {loading && (
                <div className="graph-loader" style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.2)', zIndex: 5 }}>
                    <div className="skeleton-circle animate-pulse" style={{ width: 40, height: 40, background: 'var(--color-accent-subtle)' }} />
                </div>
            )}

            <ForceGraph2D
                ref={fgRef}
                graphData={data}
                nodeLabel={(node: any) => `
                    <div class="graph-tooltip">
                        <strong>${node.name}</strong><br/>
                        <small>${node.labels.join(', ')}</small>
                    </div>
                `}
                nodeColor={(node: any) => node.color}
                nodeRelSize={6}
                linkColor={() => 'rgba(255, 255, 255, 0.15)'}
                linkDirectionalParticles={2}
                linkDirectionalParticleSpeed={d => 0.005}
                onNodeClick={handleNodeClick}
                backgroundColor="transparent"
                width={fullscreen ? window.innerWidth : undefined}
                height={fullscreen ? window.innerHeight : (typeof height === 'number' ? height : undefined)}
            />

            <div className="graph-legend" style={{ position: 'absolute', bottom: 16, left: 16, zIndex: 10, background: 'rgba(0,0,0,0.4)', padding: '8px 12px', borderRadius: 12, border: '1px solid var(--color-glass-border)', backdropFilter: 'blur(8px)' }}>
                <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: 6, textTransform: 'uppercase' }}>Legend</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <LegendItem color="#8ab4f8" label="Document" />
                    <LegendItem color="#aecbfa" label="Chunk" />
                    <LegendItem color="#9b72cb" label="Entity" />
                    <LegendItem color="#81c995" label="Memory" />
                </div>
            </div>
        </div>
    )
}

function LegendItem({ color, label }: { color: string, label: string }) {
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={{ width: 8, height: 8, borderRadius: '50%', background: color }} />
            <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{label}</span>
        </div>
    )
}
