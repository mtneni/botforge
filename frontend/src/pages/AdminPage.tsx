import React, { useState, useEffect } from 'react'
import { Shield, BarChart3, List, Users, MessageSquare, Clock, TrendingUp, Activity } from 'lucide-react'
import { api } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import '../styles/resource-pages.css'

interface AuditLog {
    id: number;
    userId: string;
    action: string;
    detail: string;
    ipAddress: string;
    timestamp: string;
}

interface DashboardStats {
    totalUsers: number;
    totalConversations: number;
    totalAuditEvents: number;
    eventsToday: number;
    messagesToday: number;
    dailyTokenLimit: number;
}

interface ActivityItem {
    action: string;
    count: number;
}

interface TopUser {
    username: string;
    eventCount: number;
}

type Tab = 'overview' | 'audit'

export function AdminPage() {
    const { user } = useAuth()
    const [tab, setTab] = useState<Tab>('overview')
    const [logs, setLogs] = useState<AuditLog[]>([])
    const [loading, setLoading] = useState(true)
    const [page, setPage] = useState(0)

    // Analytics state
    const [stats, setStats] = useState<DashboardStats | null>(null)
    const [activity, setActivity] = useState<ActivityItem[]>([])
    const [topUsers, setTopUsers] = useState<TopUser[]>([])

    useEffect(() => {
        if (tab === 'audit') loadLogs()
        if (tab === 'overview') loadAnalytics()
    }, [tab, page])

    const loadLogs = async () => {
        setLoading(true)
        try {
            const resp = await api.get<any>(`/api/admin/audit?page=${page}&size=20`)
            if (resp && resp.content) setLogs(resp.content)
            else if (Array.isArray(resp)) setLogs(resp)
        } catch (err) { console.error(err) }
        finally { setLoading(false) }
    }

    const loadAnalytics = async () => {
        setLoading(true)
        try {
            const [dashboardResp, activityResp, usersResp] = await Promise.all([
                api.get<DashboardStats>('/api/admin/analytics/dashboard'),
                api.get<ActivityItem[]>('/api/admin/analytics/activity?days=7'),
                api.get<TopUser[]>('/api/admin/analytics/top-users?days=7&limit=5'),
            ])
            setStats(dashboardResp)
            setActivity(activityResp || [])
            setTopUsers(usersResp || [])
        } catch (err) { console.error(err) }
        finally { setLoading(false) }
    }

    if (!user) return null

    const maxActivity = Math.max(...activity.map(a => a.count), 1)

    const actionLabels: Record<string, string> = {
        'LOGIN': 'Logins',
        'LOGOUT': 'Logouts',
        'REGISTER': 'Sign-ups',
        'CHAT_MESSAGE': 'Messages',
        'PERSONA_SWITCH': 'Persona Switches',
        'DOCUMENT_UPLOAD': 'Uploads',
        'ADMIN_ACTION': 'Admin Actions',
    }

    const actionColors: Record<string, string> = {
        'LOGIN': '#22c55e',
        'CHAT_MESSAGE': '#3b82f6',
        'REGISTER': '#a855f7',
        'DOCUMENT_UPLOAD': '#f59e0b',
        'PERSONA_SWITCH': '#06b6d4',
        'ADMIN_ACTION': '#f87171',
        'LOGOUT': '#6b7280',
    }

    const tabs: { id: Tab; icon: any; label: string }[] = [
        { id: 'overview', icon: BarChart3, label: 'Overview' },
        { id: 'audit', icon: List, label: 'Audit Logs' },
    ]

    return (
        <div className="resource-page">
            <header className="resource-header">
                <div className="resource-header-inner">
                    <div className="resource-header-left">
                        <h1 className="resource-title">
                            <Shield size={24} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '8px', color: '#f87171' }} />
                            Admin Dashboard
                        </h1>
                        <p className="resource-subtitle">Platform analytics, audit trails, and operational intelligence.</p>
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
                    </div>

                    {/* -------- OVERVIEW TAB -------- */}
                    {tab === 'overview' && !loading && stats && (
                        <>
                            <div className="resource-stats" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))' }}>
                                <div className="stat-card">
                                    <span className="stat-label"><Users size={14} style={{ marginRight: '6px' }} />Users</span>
                                    <span className="stat-value">{stats.totalUsers}</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label"><MessageSquare size={14} style={{ marginRight: '6px' }} />Conversations</span>
                                    <span className="stat-value">{stats.totalConversations}</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label"><Activity size={14} style={{ marginRight: '6px' }} />Events Today</span>
                                    <span className="stat-value">{stats.eventsToday}</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label"><TrendingUp size={14} style={{ marginRight: '6px' }} />Messages Today</span>
                                    <span className="stat-value">{stats.messagesToday}</span>
                                </div>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginTop: '24px' }}>
                                {/* Activity Breakdown */}
                                <div className="glass-card" style={{ padding: '24px' }}>
                                    <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <BarChart3 size={18} /> Activity Breakdown (7d)
                                    </h3>
                                    {activity.length === 0 ? (
                                        <div style={{ color: 'var(--color-text-muted)', textAlign: 'center', padding: '24px' }}>No activity data yet.</div>
                                    ) : (
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                            {activity.map((item) => (
                                                <div key={item.action} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                                    <span style={{ width: '120px', fontSize: '0.8rem', color: 'var(--color-text-muted)', textAlign: 'right', flexShrink: 0 }}>
                                                        {actionLabels[item.action] || item.action}
                                                    </span>
                                                    <div style={{ flex: 1, background: 'var(--color-bg-secondary)', borderRadius: '6px', overflow: 'hidden', height: '24px' }}>
                                                        <div
                                                            style={{
                                                                width: `${(item.count / maxActivity) * 100}%`,
                                                                height: '100%',
                                                                background: actionColors[item.action] || '#6b7280',
                                                                borderRadius: '6px',
                                                                transition: 'width 0.6s ease-out',
                                                                minWidth: '2px',
                                                            }}
                                                        />
                                                    </div>
                                                    <span style={{ width: '40px', fontSize: '0.85rem', fontWeight: '600', fontFamily: 'monospace' }}>
                                                        {item.count}
                                                    </span>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* Top Users */}
                                <div className="glass-card" style={{ padding: '24px' }}>
                                    <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <Users size={18} /> Most Active Users (7d)
                                    </h3>
                                    {topUsers.length === 0 ? (
                                        <div style={{ color: 'var(--color-text-muted)', textAlign: 'center', padding: '24px' }}>No user data yet.</div>
                                    ) : (
                                        <div className="table-responsive">
                                            <table className="data-table" style={{ fontSize: '0.85rem' }}>
                                                <thead>
                                                    <tr>
                                                        <th>#</th>
                                                        <th>Username</th>
                                                        <th style={{ textAlign: 'right' }}>Events</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {topUsers.map((u, i) => (
                                                        <tr key={u.username}>
                                                            <td style={{ fontWeight: '600', color: i < 3 ? '#f59e0b' : 'inherit' }}>
                                                                {i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : i + 1}
                                                            </td>
                                                            <td>{u.username}</td>
                                                            <td style={{ textAlign: 'right', fontFamily: 'monospace', fontWeight: '600' }}>{u.eventCount}</td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </>
                    )}

                    {tab === 'overview' && loading && (
                        <div className="resource-stats">
                            {[1, 2, 3, 4].map((i) => (
                                <div key={i} className="stat-card skeleton" style={{ height: '90px', background: 'var(--color-bg-secondary)' }} />
                            ))}
                        </div>
                    )}

                    {/* -------- AUDIT LOGS TAB -------- */}
                    {tab === 'audit' && (
                        <div className="card full-width">
                            <div className="card-header border-bottom">
                                <div className="card-title">
                                    <List size={18} />
                                    <span>System Audit Logs</span>
                                </div>
                            </div>

                            <div className="card-body no-padding">
                                {loading ? (
                                    <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
                                        Loading audit records...
                                    </div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="data-table">
                                            <thead>
                                                <tr>
                                                    <th>Time</th>
                                                    <th>User</th>
                                                    <th>Action</th>
                                                    <th>Detail</th>
                                                    <th>IP Address</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {logs.map((log) => (
                                                    <tr key={log.id}>
                                                        <td className="mono">{new Date(log.timestamp).toLocaleString()}</td>
                                                        <td>{log.userId}</td>
                                                        <td><span className="badge badge-info">{log.action}</span></td>
                                                        <td className="detail-cell" title={log.detail}>{log.detail}</td>
                                                        <td className="mono muted">{log.ipAddress}</td>
                                                    </tr>
                                                ))}
                                                {logs.length === 0 && (
                                                    <tr>
                                                        <td colSpan={5} style={{ textAlign: 'center', padding: '2rem' }}>
                                                            No audit logs found.
                                                        </td>
                                                    </tr>
                                                )}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </main>
        </div>
    )
}
