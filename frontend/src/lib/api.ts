const API_BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost').replace(/\/$/, '');

const TOKEN_KEY = 'inventory_token';
const REFRESH_TOKEN_KEY = 'inventory_refresh_token';

interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
}

interface TokenPair {
  token: string;
  refreshToken: string;
}

export class ApiError<T = unknown> extends Error {
  readonly status: number;
  readonly data: T | null;

  constructor(message: string, status: number, data: T | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

export function storeTokens(pair: TokenPair) {
  localStorage.setItem(TOKEN_KEY, pair.token);
  localStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

// Concurrent requests that all hit a 401 at once must not each fire their own
// /api/auth/refresh call — the backend rotates the refresh token on every use,
// so a second concurrent call would present an already-rotated (now invalid)
// token and fail. This promise is shared so only the first 401 triggers a
// refresh; everyone else awaits the same in-flight result.
let refreshInFlight: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (refreshInFlight) {
    return refreshInFlight;
  }

  refreshInFlight = (async () => {
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!storedRefreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: storedRefreshToken }),
    });

    const payload = (await response.json().catch(() => null)) as ApiResponse<TokenPair> | null;
    if (!response.ok || !payload?.success || !payload.data) {
      throw new Error('Session expired');
    }

    storeTokens(payload.data);
    return payload.data.token;
  })();

  try {
    return await refreshInFlight;
  } finally {
    refreshInFlight = null;
  }
}

function redirectToLogin() {
  clearTokens();
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

async function doFetch<T>(path: string, options: RequestInit, token: string | null): Promise<Response> {
  return fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const isAuthEndpoint = path.startsWith('/api/auth/');
  const token = typeof window === 'undefined' ? null : localStorage.getItem(TOKEN_KEY);

  let response = await doFetch<T>(path, options, token);

  // On a 401 from a non-auth endpoint, try exactly one silent refresh-and-retry
  // before giving up. Auth endpoints (login/refresh/register/etc.) are excluded
  // so a failed login attempt or a genuinely-dead refresh token doesn't loop
  // back into another refresh attempt.
  if (response.status === 401 && !isAuthEndpoint && typeof window !== 'undefined') {
    try {
      const newToken = await refreshAccessToken();
      response = await doFetch<T>(path, options, newToken);
    } catch {
      redirectToLogin();
      throw new ApiError('Session expired. Please log in again.', 401);
    }
  }

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;
  if (!response.ok || !payload?.success) {
    if (response.status === 401 && !isAuthEndpoint) {
      redirectToLogin();
    }
    throw new ApiError(
      payload?.message || `Request failed (${response.status})`,
      response.status,
      payload?.data ?? null,
    );
  }

  return payload.data;
}

export { API_BASE_URL };