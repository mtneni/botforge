import React, { useState, useEffect } from 'react'
import { Shield, Clock, Search, List } from 'lucide-react'
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

export function AdminPage() {
    const { user } = useAuth()
    const [logs, setLogs] = useState<AuditLog[]>([])
    const [loading, setLoading] = useState(true)
    const [page, setPage] = useState(0)

    useEffect(() => {
        loadLogs()
    }, [page])

    const loadLogs = async () => {
        setLoading(true)
        try {
            const resp = await api.get<any>(`/api/admin/audit?page=${page}&size=20`)
            // Spring Data Page structure
            if (resp && resp.content) {
                setLogs(resp.content)
            } else if (Array.isArray(resp)) {
                setLogs(resp)
            }
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    if (!user) return null

    return (
        <div className="resource-page">
            <header className="resource-header">
                <div className="resource-header-inner">
                    <div className="resource-header-left">
                        <h1 className="resource-title"><Shield size={24} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '8px', color: '#f87171' }} />Admin Dashboard</h1>
                        <p className="resource-subtitle">Audit logging, security events, and compliance tracking.</p>
                    </div>
                </div>
            </header>

            <main className="resource-container">
                <div className="resource-content">
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
                                                <th>User ID</th>
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
                </div>
            </main>
        </div>
    )
}
