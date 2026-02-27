import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import { api } from '../api/client';

interface User {
    id: string;
    displayName: string;
    username: string;
    currentContext: string;
}

interface AuthContextType {
    user: User | null;
    loading: boolean;
    login: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        api.get<User>('/api/auth/me')
            .then(setUser)
            .catch(() => setUser(null))
            .finally(() => setLoading(false));
    }, []);

    const login = useCallback(async (username: string, password: string) => {
        const u = await api.post<User>('/api/auth/login', { username, password });
        setUser(u);
    }, []);

    const logout = useCallback(async () => {
        await api.post('/api/auth/logout');
        setUser(null);
    }, []);

    return (
        <AuthContext.Provider value={{ user, loading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be inside AuthProvider');
    return ctx;
}
