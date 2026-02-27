const API_BASE = '';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const res = await fetch(`${API_BASE}${path}`, {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...options?.headers,
        },
        ...options,
    });

    if (!res.ok) {
        // Handle session expiration
        if (res.status === 401 || res.status === 403) {
            if (!window.location.pathname.startsWith('/login')) {
                window.location.href = '/login';
            }
        }
        const err = await res.json().catch(() => ({ error: res.statusText }));
        throw new Error(err.error || res.statusText);
    }

    return res.json();
}

export const api = {
    get: <T>(path: string) => request<T>(path),
    post: <T>(path: string, body?: unknown) =>
        request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
    put: <T>(path: string, body?: unknown) =>
        request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
    del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),

    upload: async <T>(path: string, file: File): Promise<T> => {
        const form = new FormData();
        form.append('file', file);
        const res = await fetch(`${API_BASE}${path}`, {
            method: 'POST',
            credentials: 'include',
            body: form,
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({ error: res.statusText }));
            throw new Error(err.error || res.statusText);
        }
        return res.json();
    },
};
