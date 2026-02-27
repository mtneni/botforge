export interface Citation {
    uri: string;
    title: string;
    snippet: string;
}

export type SseEventHandler = {
    onProgress?: (data: any) => void;
    onToken?: (data: any) => void;
    onMessage?: (data: any) => void;
    onMemory?: (data: any) => void;
    onError?: (data: any) => void;
    onConnected?: () => void;
};

export function connectSse(handlers: SseEventHandler): EventSource {
    const source = new EventSource('/api/chat/stream', { withCredentials: true });

    source.addEventListener('connected', () => {
        handlers.onConnected?.();
    });

    source.addEventListener('progress', (event) => {
        const data = JSON.parse(event.data);
        handlers.onProgress?.(data);
    });

    source.addEventListener('token', (event) => {
        const data = JSON.parse(event.data);
        handlers.onToken?.(data);
    });

    source.addEventListener('message', (event) => {
        const data = JSON.parse(event.data);
        handlers.onMessage?.(data);
    });

    source.addEventListener('memory', (event) => {
        const data = JSON.parse(event.data);
        handlers.onMemory?.(data);
    });

    source.addEventListener('error', (event) => {
        // SSE error — don't treat reconnects as fatal
        if (source.readyState === EventSource.CLOSED) {
            handlers.onError?.('Connection lost');
        }
    });

    return source;
}
