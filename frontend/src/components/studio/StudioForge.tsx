import { useState } from 'react'
import { Check, X, Terminal } from 'lucide-react'

interface StudioForgeProps {
    editingId: string | null
    initialForm: {
        displayName: string
        objective: string
        behaviour: string
        description: string
        systemPrompt: string
        toolIds: string
        tone?: number
        voice?: number
    }
    availableTools: Array<{ id: string, name: string, description: string }>
    onSave: (form: any) => void
    onCancel: () => void
}

export function StudioForge({ editingId, initialForm, availableTools, onSave, onCancel }: StudioForgeProps) {
    const [form, setForm] = useState(initialForm)

    const handleSubmit = () => {
        onSave(form)
    }

    return (
        <div className="glass-card animate-in" style={{ padding: '32px', maxWidth: '640px', margin: '0 auto' }}>
            <div style={{ marginBottom: '24px' }}>
                <h2 style={{ fontSize: '1.4rem', marginBottom: '8px' }}>
                    {editingId ? 'Recalibrate Neural Pattern' : 'Neural Pattern Designer'}
                </h2>
                <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>
                    {editingId
                        ? 'Adjust the core identity and reasoning boundaries of this profile.'
                        : 'Define the core identity, reasoning boundaries, and tone of your custom engine.'}
                </p>
            </div>

            <div className="url-form" style={{ display: 'flex', flexDirection: 'column', gap: '20px', padding: 0 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Identity Name</label>
                    <input
                        type="text"
                        className="url-input"
                        placeholder="e.g. Expert Code Auditor"
                        value={form.displayName}
                        onChange={e => setForm({ ...form, displayName: e.target.value })}
                        style={{ width: '100%' }}
                    />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Primary Directive (Short Summary)</label>
                    <textarea
                        className="url-input"
                        placeholder="Define its internal objective... (e.g. Always prioritize security, be concise, use technical terminology)"
                        value={form.objective}
                        onChange={e => setForm({ ...form, objective: e.target.value })}
                        style={{ width: '100%', minHeight: '80px', borderRadius: '12px', padding: '12px' }}
                    />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>System Prompt (Detailed Instructions)</label>
                    <textarea
                        className="url-input"
                        placeholder="Detailed system instructions... (e.g. You are a senior software architect. Follow C4 modeling principles...)"
                        value={form.systemPrompt}
                        onChange={e => setForm({ ...form, systemPrompt: e.target.value })}
                        style={{ width: '100%', minHeight: '160px', borderRadius: '12px', padding: '12px', border: '1px solid var(--color-accent)' }}
                    />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Capabilities (Tools)</label>
                    <div className="glass-card" style={{ padding: '12px', display: 'flex', flexWrap: 'wrap', gap: '8px', background: 'rgba(255,255,255,0.02)' }}>
                        {availableTools.map(tool => {
                            const isSelected = form.toolIds.split(',').includes(tool.id);
                            return (
                                <button
                                    key={tool.id}
                                    onClick={() => {
                                        const current = form.toolIds ? form.toolIds.split(',') : [];
                                        const next = isSelected
                                            ? current.filter(id => id !== tool.id)
                                            : [...current, tool.id];
                                        setForm({ ...form, toolIds: next.filter(Boolean).join(',') });
                                    }}
                                    className={`schema-tag ${isSelected ? 'active' : ''}`}
                                    style={{
                                        cursor: 'pointer',
                                        transition: 'all 0.2s',
                                        background: isSelected ? 'var(--color-accent)' : 'rgba(255,255,255,0.05)',
                                        color: isSelected ? '#fff' : 'var(--color-text-secondary)',
                                        borderColor: isSelected ? 'var(--color-accent)' : 'transparent'
                                    }}
                                    title={tool.description}
                                >
                                    {tool.name}
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* Advanced Sliders for Tone/Voice */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', margin: '8px 0' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <label style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Reasoning Density</label>
                            <span style={{ fontSize: '0.75rem', color: 'var(--color-accent)' }}>{form.tone || 50}%</span>
                        </div>
                        <input
                            type="range"
                            min="0" max="100"
                            value={form.tone || 50}
                            onChange={e => setForm({ ...form, tone: parseInt(e.target.value) })}
                        />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <label style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Creativity Shift</label>
                            <span style={{ fontSize: '0.75rem', color: 'var(--color-accent)' }}>{form.voice || 50}%</span>
                        </div>
                        <input
                            type="range"
                            min="0" max="100"
                            value={form.voice || 50}
                            onChange={e => setForm({ ...form, voice: parseInt(e.target.value) })}
                        />
                    </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Base Tagline</label>
                    <input
                        type="text"
                        className="url-input"
                        placeholder="A short description for the UI"
                        value={form.description}
                        onChange={e => setForm({ ...form, description: e.target.value })}
                        style={{ width: '100%' }}
                    />
                </div>

                <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                    <button className="primary-btn" onClick={handleSubmit} style={{ flex: 1 }}>
                        <Check size={16} />
                        {editingId ? 'Apply Calibration' : 'Deploy Custom Pattern'}
                    </button>
                    <button className="secondary-btn" onClick={() => onSave({ ...form, isTry: true })} style={{ flex: 1 }}>
                        <Terminal size={16} />
                        Try Simulation
                    </button>
                    <button className="secondary-btn" onClick={onCancel} style={{ flex: 0.5 }}>
                        <X size={16} />
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    )
}
