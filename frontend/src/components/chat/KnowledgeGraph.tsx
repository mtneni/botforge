import { useEffect, useState, useRef, useMemo } from 'react'
import ForceGraph2D from 'react-force-graph-2d'
import { api } from '../../api/client'
import { RefreshCw, X, Maximize2, Minimize2 } from 'lucide-react'
import '../../styles/graph.css'

interface GraphNode {
    id: string
    name: string
    labels: string[]
    properties: any
    val?: number
    color?: string
    x?: number
    y?: number
}

interface GraphLink {
    source: string | GraphNode
    target: string | GraphNode
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
    const [hoverNode, setHoverNode] = useState<GraphNode | null>(null)
    const fgRef = useRef<any>(null)

    const loadData = async () => {
        setLoading(true)
        try {
            const res = await api.get<GraphData>(`/api/graph/data${contextId ? `?contextId=${contextId}` : ''}`)

            // Post-process nodes for visualization
            const processedNodes = res.nodes.map(node => ({
                ...node,
                val: node.labels.includes('ContentRoot') ? 12 : 6,
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
        if (labels.includes('ContentRoot')) return '#8ab4f8' // Gemini Blue
        if (labels.includes('Chunk')) return '#aecbfa' // Light Blue
        if (labels.includes('NamedEntity')) return '#9b72cb' // Purple
        if (labels.includes('Proposition')) return '#81c995' // Green
        return '#9aa0a6' // Gray
    }

    // Connected nodes/links for hover highlighting
    const connectedData = useMemo(() => {
        if (!hoverNode) return { neighNodes: new Set(), neighLinks: new Set() }
        const neighNodes = new Set()
        const neighLinks = new Set()

        data.links.forEach(link => {
            const sourceId = typeof link.source === 'object' ? link.source.id : link.source
            const targetId = typeof link.target === 'object' ? link.target.id : link.target

            if (sourceId === hoverNode.id) {
                neighNodes.add(targetId)
                neighLinks.add(link)
            } else if (targetId === hoverNode.id) {
                neighNodes.add(sourceId)
                neighLinks.add(link)
            }
        })
        return { neighNodes, neighLinks }
    }, [hoverNode, data.links])

    useEffect(() => {
        loadData()
    }, [contextId])

    const handleNodeClick = (node: any) => {
        fgRef.current.centerAt(node.x, node.y, 1000)
        fgRef.current.zoom(2.5, 1000)
    }

    return (
        <div className={`knowledge-graph-container ${fullscreen ? 'fullscreen' : ''}`} style={{ height, position: 'relative' }}>
            <div className="graph-controls" style={{ position: 'absolute', top: 16, right: 16, zIndex: 10 }}>
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
                        <strong>${node.name}</strong>
                        <small>${node.labels.join(', ')}</small>
                    </div>
                `}
                nodeCanvasObject={(node: any, ctx, globalScale) => {
                    const label = node.name;
                    const fontSize = 12 / globalScale;
                    ctx.font = `${fontSize}px var(--font-sans)`;

                    const isHighlighted = !hoverNode || hoverNode.id === node.id || connectedData.neighNodes.has(node.id);
                    const alpha = isHighlighted ? 1 : 0.15;

                    // Draw Glow
                    if (isHighlighted && node.color) {
                        ctx.shadowColor = node.color;
                        ctx.shadowBlur = 10;
                    }

                    // Main Circle
                    ctx.beginPath();
                    ctx.arc(node.x, node.y, node.val / globalScale + 1, 0, 2 * Math.PI, false);
                    ctx.fillStyle = node.color || '#9aa0a6';
                    ctx.globalAlpha = alpha;
                    ctx.fill();

                    // Reset shadow for text
                    ctx.shadowBlur = 0;

                    // Label
                    if (globalScale > 1.5 || isHighlighted) {
                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillStyle = isHighlighted ? 'rgba(255, 255, 255, 0.8)' : 'rgba(255, 255, 255, 0.1)';
                        ctx.fillText(label, node.x, node.y + (node.val / globalScale) + fontSize + 2);
                    }

                    ctx.globalAlpha = 1;
                }}
                linkCanvasObjectMode={() => 'always'}
                linkCanvasObject={(link: any, ctx, globalScale) => {
                    const isHighlighted = !hoverNode || connectedData.neighLinks.has(link);
                    const alpha = isHighlighted ? 0.3 : 0.05;
                    const color = isHighlighted ? 'rgba(255, 255, 255, ' + alpha + ')' : 'rgba(255, 255, 255, 0.05)';

                    ctx.strokeStyle = color;
                    ctx.lineWidth = isHighlighted ? 2 / globalScale : 0.5 / globalScale;
                    ctx.beginPath();
                    ctx.moveTo(link.source.x, link.source.y);
                    ctx.lineTo(link.target.x, link.target.y);
                    ctx.stroke();

                    if (isHighlighted && globalScale > 2) {
                        const midX = (link.source.x + link.target.x) / 2;
                        const midY = (link.source.y + link.target.y) / 2;
                        ctx.font = `${8 / globalScale}px var(--font-sans)`;
                        ctx.fillStyle = 'rgba(255, 255, 255, 0.5)';
                        ctx.fillText(link.type || '', midX, midY);
                    }
                }}
                onNodeClick={handleNodeClick}
                onNodeHover={setHoverNode}
                backgroundColor="transparent"
                width={fullscreen ? window.innerWidth : undefined}
                height={fullscreen ? window.innerHeight : (typeof height === 'number' ? height : undefined)}
                d3AlphaDecay={0.02}
                d3VelocityDecay={0.3}
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
