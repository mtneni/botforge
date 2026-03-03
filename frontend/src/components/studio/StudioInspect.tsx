import { X, Sparkles, Wand2, Shield, Zap, Terminal } from 'lucide-react'

interface PersonaPreset {
    id: string
    displayName: string
    objective: string
    behaviour: string
    description: string
    icon: string
    active: boolean
    systemPrompt?: string
    toolIds?: string
}

interface StudioInspectProps {
    persona: PersonaPreset
    onClose: () => void
    onSwitch: (id: string) => void
    onEdit: (p: PersonaPreset) => void
    onArchive: (id: string) => void
}

export function StudioInspect({ persona, onClose, onSwitch, onEdit, onArchive }: StudioInspectProps) {

    const getIcon = (id: string) => {
        switch (id) {
            case 'assistant': return <Sparkles size={24} />
            case 'architect': return <Wand2 size={24} />
            case 'security': return <Shield size={24} />
            case 'developer': return <Zap size={24} />
            default: return <Terminal size={24} />
        }
    }

    const getColorClass = (id: string) => {
        switch (id) {
            case 'architect': return '#e879f9' // Fuchsia
            case 'assistant': return '#8ab4f8' // Blue
            case 'security': return '#4ade80' // Green
            case 'developer': return '#fcd34d' // Amber
            default: return 'var(--color-accent)'
        }
    }

    return (
        <>
            <div className="page-drawer-overlay" onClick={onClose} />
            <div className="page-drawer">
                <header className="page-drawer-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                        <div className="card-icon" style={{
                            width: '48px',
                            height: '48px',
                            background: `${getColorClass(persona.id)}20`,
                            color: getColorClass(persona.id)
                        }}>
                            {getIcon(persona.id)}
                        </div>
                        <div>
                            <h2 style={{ fontSize: '1.4rem', fontWeight: 700, margin: 0 }}>{persona.displayName}</h2>
                            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>Neural Configuration</p>
                        </div>
                    </div>
                    <button className="secondary-btn icon-only" onClick={onClose}>
                        <X size={18} />
                    </button>
                </header>

                <main className="page-drawer-content">
                    <div className="detail-section">
                        <h4 style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '8px' }}>
                            Profile Tagline
                        </h4>
                        <p style={{ fontSize: '0.95rem', color: 'var(--color-text-secondary)', lineHeight: '1.5' }}>
                            {persona.description}
                        </p>
                    </div>

                    <div className="detail-section">
                        <h4 style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '8px' }}>
                            Primary Directive / System Prompt
                        </h4>
                        <div style={{
                            background: 'rgba(255,255,255,0.03)',
                            padding: '16px',
                            borderRadius: '12px',
                            border: '1px solid var(--color-glass-border)',
                            lineHeight: '1.6',
                            fontSize: '0.9rem',
                            whiteSpace: 'pre-wrap'
                        }}>
                            {persona.systemPrompt || persona.objective || 'Standard operational logic.'}
                        </div>
                    </div>

                    <div className="detail-section">
                        <h4 style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '8px' }}>
                            Neural Capabilities
                        </h4>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                            <span className="schema-tag">{persona.behaviour} MODE</span>
                            {persona.toolIds ? (
                                persona.toolIds.split(',').map(toolId => (
                                    <span key={toolId} className="schema-tag" style={{ background: 'rgba(232, 121, 249, 0.1)', color: '#e879f9' }}>
                                        {toolId.toUpperCase()}
                                    </span>
                                ))
                            ) : (
                                <span className="schema-tag">STANDARD TOOLS</span>
                            )}
                        </div>
                    </div>

                    {persona.id.startsWith('custom_') && (
                        <div className="detail-section" style={{ marginTop: 'auto', borderTop: '1px solid var(--color-glass-border)', paddingTop: '24px' }}>
                            <h4 style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '12px' }}>
                                Management
                            </h4>
                            <div style={{ display: 'flex', gap: '12px' }}>
                                <button className="secondary-btn" style={{ flex: 1 }} onClick={() => onEdit(persona)}>
                                    Recalibrate Profile
                                </button>
                                <button className="secondary-btn danger" style={{ flex: 1 }} onClick={() => onArchive(persona.id)}>
                                    Archive Neural Pattern
                                </button>
                            </div>
                        </div>
                    )}
                </main>

                <footer className="page-drawer-footer">
                    {!persona.active ? (
                        <button
                            className="primary-btn"
                            style={{ width: '100%' }}
                            onClick={() => {
                                onSwitch(persona.id);
                                onClose();
                            }}
                        >
                            Deploy Profile
                        </button>
                    ) : (
                        <div className="schema-tag" style={{ textAlign: 'center', background: 'rgba(74, 222, 128, 0.1)', color: '#4ade80', width: '100%', padding: '12px' }}>
                            Profile Operational
                        </div>
                    )}
                </footer>
            </div>
        </>
    )
}
