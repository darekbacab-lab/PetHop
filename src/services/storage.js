export function createStorageService(getDriver = () => globalThis.localStorage) {
  return {
    getItem(key) {
      try { return getDriver()?.getItem(key) ?? null; }
      catch { return null; }
    },
    setItem(key, value) {
      try { getDriver()?.setItem(key, String(value)); return true; }
      catch { return false; }
    },
    removeItem(key) {
      try { getDriver()?.removeItem(key); return true; }
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
}

export const storageService = createStorageService();
export const sessionStorageService = createStorageService(() => globalThis.sessionStorage);

if (typeof window !== 'undefined') {
  window.PetHopStorage = storageService;
  window.PetHopSessionStorage = sessionStorageService;
}
