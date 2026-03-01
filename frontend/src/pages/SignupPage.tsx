import { useState, type FormEvent } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useConfig } from '../hooks/useConfig'
import { LogIn, UserPlus } from 'lucide-react'
import { Link } from 'react-router-dom'
import logo from '../assets/logo.png'
import '../styles/login.css'

export function SignupPage() {
    const { register } = useAuth()
    const { config } = useConfig()

    const [username, setUsername] = useState('')
    const [displayName, setDisplayName] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const persona = config?.persona || 'BotForge Enterprise'
    const tagline = config?.tagline || 'Create your account'

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        if (!username.trim() || !password.trim() || !displayName.trim()) return
        setError('')
        setLoading(true)
        try {
            await register(username, displayName, password)
        } catch (err: any) {
            setError(err?.response?.data || 'Failed to register — please try again')
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
                            placeholder="Choose a username"
                            autoFocus
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Display Name</label>
                        <input
                            type="text"
                            className="form-input"
                            value={displayName}
                            onChange={(e) => setDisplayName(e.target.value)}
                            placeholder="Your full name"
                            autoComplete="name"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Password</label>
                        <input
                            type="password"
                            className="form-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Choose a password"
                            autoComplete="new-password"
                            required
                        />
                    </div>

                    {error && <div className="login-error">{error}</div>}

                    <button type="submit" className="login-btn" disabled={loading || !username.trim() || !password.trim() || !displayName.trim()}>
                        {loading ? 'Creating account…' : (
                            <>
                                <UserPlus size={16} style={{ display: 'inline', verticalAlign: -3, marginRight: 6 }} />
                                Sign Up
                            </>
                        )}
                    </button>

                    <div style={{ marginTop: '24px', textAlign: 'center', fontSize: '13px', color: 'var(--color-text-muted)' }}>
                        Already have an account?{' '}
                        <Link to="/login" style={{ color: 'var(--color-accent)', textDecoration: 'none' }}>
                            Sign In
                        </Link>
                    </div>
                </form>
            </div>
        </div>
    )
}
