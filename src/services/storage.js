export const storageService = {
  getItem(key) {
    try { return globalThis.localStorage?.getItem(key) ?? null; }
    catch { return null; }
  },
  setItem(key, value) {
    try { globalThis.localStorage?.setItem(key, String(value)); return true; }
    catch { return false; }
  },
  removeItem(key) {
    try { globalThis.localStorage?.removeItem(key); return true; }
    catch { return false; }
  },
  read(key, fallback = null) {
    try {
      const value = this.getItem(key);
      return value === null ? fallback : JSON.parse(value);
    } catch { return fallback; }
  },
  write(key, value) {
    try { return this.setItem(key, JSON.stringify(value)); }
    catch { return false; }
  },
  remove(key) {
    return this.removeItem(key);
  }
};
