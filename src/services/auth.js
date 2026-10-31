import { api, clearApiSession } from './api.js';

export const currentUser = { value: null };

export async function loadCurrentUser() {
  try { currentUser.value = await api('/auth/me'); return currentUser.value; }
  catch (error) { if (error.status === 401 || error.status === 403) currentUser.value = null; throw error; }
}

export async function login(credentials) {
  const user = await api('/auth/login', { method: 'POST', body: credentials });
  currentUser.value = user;
  clearApiSession();
  return user;
}

export async function register(account) {
  return api('/auth/register', { method: 'POST', body: account });
}

export async function logout() {
  try { await api('/auth/logout', { method: 'POST' }); }
  finally { clearApiSession(); currentUser.value = null; }
}
