import { useState, useEffect } from 'react';
import { api } from '../api/client';

export interface AppConfig {
    persona: string;
    displayName: string;
    tagline: string;
    stylesheet: string;
    logoUrl: string;
    neo4jHttpPort: number;
    memoryEnabled: boolean;
    users: Array<{ username: string; displayName: string }>;
}

export function useConfig() {
    const [config, setConfig] = useState<AppConfig | null>(null);

    useEffect(() => {
        api.get<AppConfig>('/api/config').then(setConfig).catch(() => { });
    }, []);

    return { config };
}

