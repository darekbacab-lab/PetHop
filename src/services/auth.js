import { api, clearApiSession } from './api.js';
import { ref } from 'vue';

export const currentUser = ref(null);
export const sessionLoaded = ref(false);
let currentUserRequest;

export async function loadCurrentUser({ refresh = false } = {}) {
  if (!refresh && sessionLoaded.value) return currentUser.value;
  if (currentUserRequest) return currentUserRequest;
  currentUserRequest = api('/auth/me').then(user => {
    currentUser.value = user;
    sessionLoaded.value = true;
    return user;
  }).catch(error => {
    if (error.status === 401 || error.status === 403) {
      currentUser.value = null;
      sessionLoaded.value = true;
      return null;
    }
    throw error;
  }).finally(() => { currentUserRequest = null; });
  return currentUserRequest;
}

export async function login(credentials) {
  const user = await api('/auth/login', { method: 'POST', body: credentials });
  currentUser.value = user;
  sessionLoaded.value = true;
  clearApiSession();
  return user;
}

export async function register(account) {
  return api('/auth/register', { method: 'POST', body: account });
}

export async function requestPasswordReset(email) {
  return api('/auth/password-reset/request', { method: 'POST', body: { email } });
}

export async function confirmPasswordReset(token, newPassword) {
  return api('/auth/password-reset/confirm', { method: 'POST', body: { token, newPassword } });
}

export async function logout() {
  try { await api('/auth/logout', { method: 'POST' }); }
  finally { clearApiSession(); currentUser.value = null; sessionLoaded.value = true; }
}

if (typeof window !== 'undefined') {
  window.addEventListener('pethop:session-expired', () => {
    currentUser.value = null;
    sessionLoaded.value = true;
  });
}
