import { useState, type FormEvent } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useConfig } from '../hooks/useConfig'
import { LogIn } from 'lucide-react'
import logo from '../assets/logo.png'
import '../styles/login.css'

export function LoginPage() {
    const { login } = useAuth()
    const { config } = useConfig()

    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const persona = config?.persona || 'Assistant'
    const tagline = config?.tagline || 'Chatbot with RAG and memory'
    const users = config?.users || []

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        if (!username.trim()) return
        setError('')
        setLoading(true)
        try {
            await login(username, password)
        } catch {
            setError('Invalid credentials — please try again')
        } finally {
            setLoading(false)
        }
    }

    const handleDemoLogin = async (name: string) => {
        setUsername(name)
        setPassword(name)
        setError('')
        setLoading(true)
        try {
            await login(name, name)
        } catch {
            setError('Login failed')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="login-page">
            <div className="login-card">
                <div className="login-header">
                    <img
                        src={logo}
                        alt=""
                        className="login-logo"
                        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
                    />
                    <div className="login-branding">
                        <h1>{persona}</h1>
                        <p>{tagline}</p>
                    </div>
                </div>

                <form className="login-form" onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label">Username</label>
                        <input
                            type="text"
                            className="form-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Enter your username"
                            autoFocus
                            autoComplete="username"
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Password</label>
                        <input
                            type="password"
                            className="form-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter your password"
                            autoComplete="current-password"
                        />
                    </div>

                    {error && <div className="login-error">{error}</div>}

                    <button type="submit" className="login-btn" disabled={loading || !username.trim()}>
                        {loading ? 'Signing in…' : (
                            <>
                                <LogIn size={16} style={{ display: 'inline', verticalAlign: -3, marginRight: 6 }} />
                                Sign In
                            </>
                        )}
                    </button>
                </form>

                {users.length > 0 && (
                    <div className="demo-section">
                        <div className="demo-label">Demo Accounts</div>
                        <div className="demo-users">
                            {users.map((u: { displayName: string; username: string }) => (
                                <button
                                    key={u.username}
                                    className="demo-user-btn"
                                    onClick={() => handleDemoLogin(u.username)}
                                    disabled={loading}
                                >
                                    <div className="demo-user-avatar">
                                        {u.displayName.charAt(0).toUpperCase()}
                                    </div>
                                    <span className="demo-user-name">{u.displayName}</span>
                                    <span className="demo-user-creds">{u.username} / {u.username}</span>
                                </button>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}
