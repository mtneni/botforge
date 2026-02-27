import { Sparkles, Palette, Zap, Shield, Wand2, Star, Check, Plus, Terminal } from 'lucide-react'

interface PersonaPreset {
    id: string
    displayName: string
    objective: string
    behaviour: string
    description: string
    icon: string
    active: boolean
}

interface StudioListProps {
    personas: PersonaPreset[]
    loading: boolean
    switching: string | null
    activePersona?: PersonaPreset
    onSwitch: (id: string) => void
    onInspect: (p: PersonaPreset) => void
    onCreate: () => void
}

export function StudioList({
    personas,
    loading,
    switching,
    activePersona,
    onSwitch,
    onInspect,
    onCreate
}: StudioListProps) {

    const getIcon = (id: string) => {
        switch (id) {
            case 'assistant': return <Sparkles size={24} />
            case 'astrid': return <Wand2 size={24} />
            case 'security': return <Shield size={24} />
            case 'developer': return <Zap size={24} />
            default: return <Terminal size={24} />
        }
    }

    const getColorClass = (id: string) => {
        switch (id) {
            case 'astrid': return '#e879f9' // Fuchsia
            case 'assistant': return '#8ab4f8' // Blue
            case 'security': return '#4ade80' // Green
            case 'developer': return '#fcd34d' // Amber
            default: return 'var(--color-accent)'
        }
    }

    if (loading) {
        return (
            <div className="data-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))' }}>
                {[1, 2, 3, 4].map(i => <div key={i} className="stat-card skeleton" style={{ height: '180px', background: 'var(--color-bg-secondary)' }} />)}
            </div>
        )
    }

    return (
        <div className="data-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))' }}>
            {personas.map((p) => (
                <div
                    key={p.id}
                    className={`data-card ${p.active ? 'active' : ''}`}
                    onClick={() => onInspect(p)}
                    style={{
                        cursor: switching ? 'wait' : 'pointer',
                        opacity: switching && switching !== p.id ? 0.6 : 1,
                        padding: '20px',
                        borderColor: p.active ? getColorClass(p.id) : '',
                        background: p.active ? `linear-gradient(135deg, rgba(255,255,255,0.05), transparent)` : ''
                    }}
                >
                    <div className="card-top-row" style={{ marginBottom: '16px' }}>
                        <div className="card-icon" style={{
                            background: p.active ? `${getColorClass(p.id)}20` : 'rgba(255,255,255,0.05)',
                            color: p.active ? getColorClass(p.id) : 'var(--color-text-muted)',
                            width: '40px',
                            height: '40px'
                        }}>
                            {getIcon(p.id)}
                        </div>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            {p.active ? (
                                <div className="schema-tag" style={{ background: `${getColorClass(p.id)}30`, color: '#fff', border: 'none' }}>
                                    <Check size={12} style={{ marginRight: '4px' }} />
                                    Operational
                                </div>
                            ) : (
                                <button
                                    className="secondary-btn"
                                    style={{ fontSize: '0.75rem', padding: '4px 12px' }}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onSwitch(p.id);
                                    }}
                                >
                                    {switching === p.id ? 'Loading...' : 'Deploy'}
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="card-info">
                        <h3 className="card-name" style={{ fontSize: '1.1rem', marginBottom: '4px' }}>{p.displayName}</h3>
                        <p className="card-meta" style={{ fontSize: '0.85rem', lineHeight: '1.4', minHeight: '48px' }}>
                            {p.description || 'Standard reasoning profile optimized for accuracy and tool use.'}
                        </p>
                    </div>

                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '16px' }}>
                        <span className="schema-tag" style={{ opacity: 0.7, fontSize: '0.65rem' }}>RAG</span>
                        <span className="schema-tag" style={{ opacity: 0.7, fontSize: '0.65rem' }}>Tool-Call</span>
                        {p.id === 'astrid' && <span className="schema-tag" style={{ color: '#e879f9', background: 'rgba(232, 121, 249, 0.1)', fontSize: '0.65rem' }}>Creative Suite</span>}
                        {p.active && <div style={{ display: 'flex', marginLeft: 'auto', gap: '4px' }}><Star size={12} fill={getColorClass(p.id)} color={getColorClass(p.id)} /></div>}
                    </div>
                </div>
            ))}

            <div
                className="data-card"
                onClick={onCreate}
                style={{
                    borderStyle: 'dashed',
                    background: 'transparent',
                    padding: '20px',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    minHeight: '200px'
                }}
            >
                <div className="card-top-row" style={{ justifyContent: 'center', marginBottom: '16px' }}>
                    <div className="card-icon" style={{ background: 'rgba(255,255,255,0.05)', color: 'var(--color-text-muted)', width: '48px', height: '48px' }}>
                        <Plus size={24} />
                    </div>
                </div>
                <div style={{ textAlign: 'center' }}>
                    <h3 style={{ fontSize: '1.1rem', marginBottom: '4px' }}>Forge Personality</h3>
                    <p style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>Define custom directives & voice.</p>
                </div>
            </div>
        </div>
    )
}
