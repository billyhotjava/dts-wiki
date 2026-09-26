// Minimal fetch client replacing axios (design 10 S4.5 bundle budget: saves ~11 KB gz).
// Same contract the hooks rely on: same-origin cookies, JHipster CSRF header,
// 401 -> full-page login redirect, errors shaped as { response: { status, data } }.
export interface ApiError {
  response: { status: number; data: unknown };
}

function xsrfToken(): string | null {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return match === null ? null : decodeURIComponent(match[1]);
}

function toLogin(): never {
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/oauth2/authorization/oidc';
  }
  throw { response: { status: 401, data: null } } as ApiError;
}

async function request<T>(method: string, url: string, body?: unknown, params?: Record<string, string>): Promise<{ data: T }> {
  let full = url;
  if (params !== undefined) {
    const query = new URLSearchParams(params).toString();
    if (query !== '') full += (url.includes('?') ? '&' : '?') + query;
  }
  const headers: Record<string, string> = {};
  const token = xsrfToken();
  if (token !== null && method !== 'GET' && method !== 'HEAD') {
    headers['X-XSRF-TOKEN'] = token;
  }
  let payload: BodyInit | undefined;
  if (body !== undefined) {
    if (body instanceof FormData) {
      payload = body;
    } else {
      headers['Content-Type'] = 'application/json';
      payload = JSON.stringify(body);
    }
  }
  const response = await fetch(full, { method, headers, body: payload, credentials: 'same-origin' });
  if (response.status === 401) {
    toLogin();
  }
  const text = await response.text();
  const data = text === '' ? null : (JSON.parse(text) as T);
  if (!response.ok) {
    throw { response: { status: response.status, data } } as ApiError;
  }
  return { data: data as T };
}

export const api = {
  get: <T>(url: string, config?: { params?: Record<string, string> }) => request<T>('GET', url, undefined, config?.params),
  post: <T>(url: string, body?: unknown) => request<T>('POST', url, body),
  put: <T>(url: string, body?: unknown) => request<T>('PUT', url, body),
  patch: <T>(url: string, body?: unknown) => request<T>('PATCH', url, body),
  delete: <T>(url: string) => request<T>('DELETE', url),
};

export interface Account {
  login: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  authorities: string[];
}

export async function fetchAccount(): Promise<Account> {
  const { data } = await api.get<Account>('/api/account');
  return data;
}
