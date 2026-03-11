import { useState, useEffect, useMemo, useCallback } from 'react'
import { Sparkles, Wand2, Shield, Zap, Terminal, X, Info } from 'lucide-react'
import { api } from '../api/client'
import { useToast } from '../hooks/useToast'
import { StudioList } from '../components/studio/StudioList'
import { StudioForge } from '../components/studio/StudioForge'
import { StudioInspect } from '../components/studio/StudioInspect'
import { StudioSandbox } from '../components/studio/StudioSandbox'
import '../styles/resource-pages.css'

interface PersonaPreset {
    id: string
    displayName: string
    objective: string
    behaviour: string
    description: string
    icon: string
    active: boolean
}

export function StudioPage() {
    const [personas, setPersonas] = useState<PersonaPreset[]>([])
    const [switching, setSwitching] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    const [availableTools, setAvailableTools] = useState<any[]>([])
    const [view, setView] = useState<'library' | 'forge' | 'sandbox'>('library')
    const [inspecting, setInspecting] = useState<PersonaPreset | null>(null)
    const [editingId, setEditingId] = useState<string | null>(null)
    const toast = useToast()

    // Form state for editing/creating
    const [form, setForm] = useState({
        displayName: '',
        objective: '',
        behaviour: 'default',
        description: '',
        systemPrompt: '',
        toolIds: '',
        tone: 50,
        voice: 50
    })

    const loadPersonas = useCallback(() => {
        setLoading(true)
        api.get<PersonaPreset[]>('/api/personas')
            .then(setPersonas)
            .catch(() => { toast.error('Failed to load personas') })
            .finally(() => setTimeout(() => setLoading(false), 400))
    }, [toast])

    useEffect(() => {
        loadPersonas()
        api.get<any[]>('/api/personas/tools')
            .then(setAvailableTools)
            .catch(() => toast.error('Failed to load capabilities'))
    }, [loadPersonas, toast])

    const activePersona = useMemo(() => personas.find(p => p.active), [personas])

    const handleSwitch = async (id: string) => {
        if (switching || personas.find(p => p.id === id)?.active) return
        setSwitching(id)
        try {
            await api.post<{ persona: string; displayName: string; tagline: string }>(
                '/api/personas/active',
                { personaId: id }
            )
            setPersonas(prev => prev.map(p => ({ ...p, active: p.id === id })))
            toast.success(`Personality recalibrated to ${personas.find(p => p.id === id)?.displayName}`)
        } catch (e) {
            toast.error('Neural switch failed')
        } finally {
            setSwitching(null)
        }
    }

    const handleSavePersona = async (formData: any) => {
        if (formData.isTry) {
            setForm(formData)
            setView('sandbox')
            return
        }

        if (!formData.displayName || (!formData.objective && !formData.systemPrompt)) {
            toast.error('Identity name and a primary directive or system prompt are required')
            return
        }
        try {
            if (editingId) {
                const res = await api.put<{ message: string; persona: PersonaPreset }>(`/api/personas/${editingId}`, formData)
                toast.success(res.message)
                setEditingId(null)
            } else {
                const res = await api.post<{ message: string; persona: PersonaPreset }>('/api/personas', formData)
                toast.success(res.message)
                await handleSwitch(res.persona.id)
            }
            loadPersonas()
            setView('library')
            setForm({
                displayName: '',
                objective: '',
                behaviour: 'default',
                description: '',
                systemPrompt: '',
                toolIds: '',
                tone: 50,
                voice: 50
            })
        } catch (e) {
            toast.error(editingId ? 'Recalibration failed' : 'Forge failed')
        }
    }

    const handleArchive = async (id: string) => {
        if (!window.confirm('Are you sure you want to archive this neural pattern? It will be permanently removed.')) return
        try {
            const res = await api.del<{ message: string }>(`/api/personas/${id}`)
            toast.success(res.message)
            loadPersonas()
            setInspecting(null)
        } catch (e) {
            toast.error('Archival failed')
        }
    }

    const startEdit = (p: PersonaPreset) => {
        setEditingId(p.id)
        setForm({
            displayName: p.displayName,
            objective: p.objective,
            behaviour: p.behaviour,
            description: p.description,
            systemPrompt: (p as any).systemPrompt || '',
            toolIds: (p as any).toolIds || '',
            tone: 50,
            voice: 50
        })
        setInspecting(null)
        setView('forge')
    }

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
        <div className="resource-page">
            <header className="resource-header">
                <div className="resource-header-inner">
                    <div className="resource-header-left">
                        <h1 className="resource-title">Persona Studio</h1>
                        <p className="resource-subtitle">
                            Hot-swap the engine's core personality, reasoning patterns, and communication voice.
                        </p>
                    </div>
                    {view !== 'library' && (
                        <button className="secondary-btn" onClick={() => setView('library')} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <X size={14} /> {view === 'sandbox' ? 'Back to Designer' : 'Back to Library'}
                        </button>
                    ) || inspecting && (
                        <button className="secondary-btn" onClick={() => setInspecting(null)} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <X size={14} /> Back to Library
                        </button>
                    )}
                </div>
            </header>

            <main className="resource-container">
                <div className="resource-content">
                    {view === 'library' ? (
                        <>
                            {/* Active Status Banner */}
                            <div className={`glass-card ${switching ? 'is-switching' : ''}`} style={{
                                borderLeft: `4px solid ${getColorClass(switching || activePersona?.id || '')}`,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                padding: '16px 24px',
                                marginBottom: '24px'
                            }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                    <div className="upload-icon-circle" style={{
                                        width: '40px',
                                        height: '40px',
                                        margin: 0,
                                        background: 'rgba(255,255,255,0.05)',
                                        color: getColorClass(switching || activePersona?.id || ''),
                                        border: '1px solid rgba(255,255,255,0.1)'
                                    }}>
                                        {getIcon(switching || activePersona?.id || 'assistant')}
                                    </div>
                                    <div>
                                        <h3 style={{ fontSize: '1rem', marginBottom: '2px' }}>
                                            {switching ? 'Recalibrating Neural Pathways...' : 'Active Personality'}
                                        </h3>
                                        <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>
                                            Current profile: <strong style={{ color: '#fff' }}>
                                                {switching
                                                    ? personas.find(p => p.id === switching)?.displayName
                                                    : (activePersona?.displayName || 'Standard Engine')}
                                            </strong>
                                        </p>
                                    </div>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <span className="schema-tag" style={{ background: 'rgba(74, 222, 128, 0.1)', color: '#4ade80', fontSize: '0.7rem' }}>System Live</span>
                                    <span className="schema-tag" style={{ fontSize: '0.7rem' }}>REASONING v4.2</span>
                                </div>
                            </div>

                            <StudioList
                                personas={personas}
                                loading={loading}
                                switching={switching}
                                activePersona={activePersona}
                                onSwitch={handleSwitch}
                                onInspect={setInspecting}
                                onCreate={() => setView('forge')}
                            />

                            {!loading && (
                                <div className="empty-state" style={{
                                    padding: '32px',
                                    flexDirection: 'row',
                                    justifyContent: 'flex-start',
                                    gap: '24px',
                                    background: 'rgba(255,255,255,0.01)',
                                    border: '1px solid var(--color-glass-border)',
                                    marginTop: '24px'
                                }}>
                                    <div className="card-icon" style={{ background: 'rgba(251, 191, 36, 0.1)', color: '#fbbf24' }}>
                                        <Info size={24} />
                                    </div>
                                    <div style={{ textAlign: 'left' }}>
                                        <h4 style={{ color: '#fff', marginBottom: '4px' }}>Expert Tip</h4>
                                        <p style={{ margin: 0, fontSize: '0.9rem' }}>
                                            Switch to <strong>Architect</strong> for UI design suggestions and creative copywriting, or use <strong>Security</strong> profile for deep code audits and compliance checks.
                                        </p>
                                    </div>
                                </div>
                            )}
                        </>
                    ) : view === 'forge' ? (
                        <StudioForge
                            editingId={editingId}
                            initialForm={form}
                            availableTools={availableTools}
                            onSave={handleSavePersona}
                            onCancel={() => {
                                setView('library');
                                setEditingId(null);
                                setForm({ displayName: '', objective: '', behaviour: 'default', description: '', tone: 50, voice: 50, systemPrompt: '', toolIds: '' } as any);
                            }}
                        />
                    ) : (
                        <div style={{ maxWidth: '640px', margin: '0 auto' }}>
                            <div style={{ marginBottom: '24px' }}>
                                <h2 style={{ fontSize: '1.4rem', marginBottom: '8px' }}>Neural Sandbox</h2>
                                <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>Verify the behavioral directives of <strong>{form.displayName}</strong> before deployment.</p>
                            </div>
                            <StudioSandbox
                                personaName={form.displayName}
                                objective={form.objective}
                                onClose={() => setView('forge')}
                            />
                            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center' }}>
                                <button className="primary-btn" onClick={() => handleSavePersona(form)}>
                                    Deploy this Pattern
                                </button>
                            </div>
                        </div>
                    )}

                    {inspecting && (
                        <StudioInspect
                            persona={inspecting}
                            onClose={() => setInspecting(null)}
                            onSwitch={handleSwitch}
                            onEdit={startEdit}
                            onArchive={handleArchive}
                        />
                    )}
                </div>
            </main>
        </div>
    )
}
