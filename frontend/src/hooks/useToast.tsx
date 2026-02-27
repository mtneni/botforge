import { useState, useCallback, useMemo, createContext, useContext, type ReactNode } from 'react'
import { CheckCircle, AlertCircle, Info, AlertTriangle, X } from 'lucide-react'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

interface Toast {
    id: number
    type: ToastType
    message: string
    exiting?: boolean
}

interface ToastContextType {
    toast: (type: ToastType, message: string) => void
    success: (message: string) => void
    error: (message: string) => void
    info: (message: string) => void
}

const ToastContext = createContext<ToastContextType | null>(null)

let nextId = 0

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toasts, setToasts] = useState<Toast[]>([])

    const removeToast = useCallback((id: number) => {
        setToasts((prev) => prev.map((t) => (t.id === id ? { ...t, exiting: true } : t)))
        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== id))
        }, 300)
    }, [])

    const addToast = useCallback(
        (type: ToastType, message: string) => {
            const id = ++nextId
            setToasts((prev) => [...prev, { id, type, message }])
            setTimeout(() => removeToast(id), 4000)
        },
        [removeToast],
    )

    const icons = {
        success: CheckCircle,
        error: AlertCircle,
        info: Info,
        warning: AlertTriangle,
    }

    const value = useMemo(() => ({
        toast: addToast,
        success: (m: string) => addToast('success', m),
        error: (m: string) => addToast('error', m),
        info: (m: string) => addToast('info', m),
    }), [addToast])

    return (
        <ToastContext.Provider value={value}>
            {children}
            <div className="toast-container">
                {toasts.map((t) => {
                    const Icon = icons[t.type]
                    return (
                        <div key={t.id} className={`toast ${t.type} ${t.exiting ? 'exiting' : ''}`}>
                            <div className="toast-icon">
                                <Icon size={18} />
                            </div>
                            <span className="toast-message">{t.message}</span>
                            <button className="toast-dismiss" onClick={() => removeToast(t.id)}>
                                <X size={14} />
                            </button>
                        </div>
                    )
                })}
            </div>
        </ToastContext.Provider>
    )
}

export function useToast() {
    const ctx = useContext(ToastContext)
    if (!ctx) throw new Error('useToast must be inside ToastProvider')
    return ctx
}
