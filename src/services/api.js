const configuredApi = String(import.meta.env?.VITE_API_URL || '').trim();
const localApi = /^https?:\/\/(localhost|127\.0\.0\.1)(?::\d+)?(?:\/|$)/i.test(configuredApi);
const productionFallback = `${globalThis.location?.origin || ''}/api/v1`;
const API_BASE = (configuredApi && !(import.meta.env?.PROD && localApi)
  ? configuredApi
  : (import.meta.env?.PROD ? productionFallback : 'http://localhost:8080/api/v1')).replace(/\/$/, '');
let csrfToken = '';

export class ApiError extends Error {
  constructor(message, status, payload) { super(message); this.name = 'ApiError'; this.status = status; this.payload = payload; }
}

async function request(url, init) {
  try {
    return await fetch(url, init);
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiError('No se pudo conectar con PetHop. Inténtalo de nuevo en unos momentos.');
    }
    throw error;
  }
}

async function ensureCsrf() {
  if (csrfToken) return csrfToken;
  const response = await request(`${API_BASE}/auth/csrf`, { credentials: 'include' });
  if (!response.ok) throw new ApiError('No se pudo iniciar una sesión segura con el servidor.', response.status);
  const token = await response.json();
  csrfToken = token.token || '';
  return csrfToken;
}

export async function api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers || {});
  const init = { ...options, method, headers, credentials: 'include' };
  if (init.body && !(init.body instanceof FormData) && typeof init.body !== 'string') {
    headers.set('Content-Type', 'application/json');
    init.body = JSON.stringify(init.body);
  }
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    headers.set('X-XSRF-TOKEN', await ensureCsrf());
  }
  const response = await request(`${API_BASE}${path.startsWith('/') ? path : `/${path}`}`, init);
  if (response.status === 401 && !/^\/?auth\/(?:me|login)(?:\?|$)/.test(path)) {
    if (typeof window !== 'undefined') window.dispatchEvent(new Event('pethop:session-expired'));
  }
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('json') ? await response.json() : await response.text();
  if (!response.ok) {
    if (response.status === 403 && method !== 'GET') csrfToken = '';
    const message = typeof payload === 'object' ? (payload.detail || payload.message || payload.title) : payload;
    throw new ApiError(message || `Error del servidor (${response.status}).`, response.status, payload);
  }
  return payload;
}

export function clearApiSession() { csrfToken = ''; }
export { API_BASE };
