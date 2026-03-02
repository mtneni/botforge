const API_BASE = '';

/**
 * Read the XSRF-TOKEN cookie set by Spring Security's CookieCsrfTokenRepository.
 * The token must be sent back as the X-XSRF-TOKEN header on mutating requests.
 */
function getCsrfToken(): string | null {
    const match = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='));
    return match ? decodeURIComponent(match.split('=')[1]) : null;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...(options?.headers as Record<string, string>),
    };

    // Attach CSRF token on mutating methods
    const method = options?.method?.toUpperCase() ?? 'GET';
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        const csrf = getCsrfToken();
        if (csrf) {
            headers['X-XSRF-TOKEN'] = csrf;
        }
    }

    const res = await fetch(`${API_BASE}${path}`, {
        credentials: 'include',
        headers,
        ...options,
    });

    if (!res.ok) {
        // Handle session expiration or insufficient permissions
        if (res.status === 401) {
            if (!window.location.pathname.startsWith('/login')) {
                window.location.href = '/login';
            }
        } else if (res.status === 403) {
            // Already logged in but no permission for this specific resource
            console.error('Forbidden: You do not have permission to access this resource.');
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

        const headers: Record<string, string> = {};
        const csrf = getCsrfToken();
        if (csrf) {
            headers['X-XSRF-TOKEN'] = csrf;
        }

        const res = await fetch(`${API_BASE}${path}`, {
            method: 'POST',
            credentials: 'include',
            headers,
            body: form,
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({ error: res.statusText }));
            throw new Error(err.error || res.statusText);
        }
        return res.json();
    },
};
