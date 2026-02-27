import { Component, type ReactNode } from 'react'

interface Props {
    children: ReactNode
    fallback?: ReactNode
}

interface State {
    hasError: boolean
    error?: Error
}

export class ErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props)
        this.state = { hasError: false }
    }

    static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error }
    }

    componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
        console.error('ErrorBoundary caught:', error, errorInfo)
    }

    render() {
        if (this.state.hasError) {
            return this.props.fallback || (
                <div style={{
                    padding: '24px',
                    textAlign: 'center',
                    color: 'var(--color-text-muted, #9898b0)',
                    fontSize: 13,
                }}>
                    <p>Something went wrong.</p>
                    <button
                        onClick={() => this.setState({ hasError: false })}
                        style={{
                            marginTop: 12,
                            padding: '6px 16px',
                            background: 'var(--color-accent, #6366f1)',
                            color: 'white',
                            border: 'none',
                            borderRadius: 8,
                            cursor: 'pointer',
                            fontSize: 13,
                            fontFamily: 'inherit',
                        }}
                    >
                        Try again
                    </button>
                </div>
            )
        }

        return this.props.children
    }
}
