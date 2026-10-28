<script setup>
import { onMounted, onUnmounted, ref } from 'vue';

const THEME_KEY = 'pethop_theme';
const darkMode = ref(document.documentElement.dataset.theme === 'dark');

function setTheme(isDark, persist = true) {
  darkMode.value = isDark;
  document.documentElement.dataset.theme = isDark ? 'dark' : 'light';
  if (persist) {
    try { localStorage.setItem(THEME_KEY, isDark ? 'dark' : 'light'); } catch { /* Theme stays active for this page. */ }
  }
}

function toggleTheme() { setTheme(!darkMode.value); }
function syncTheme(event) {
  if (event.key !== THEME_KEY && event.key !== null) return;
  const savedTheme = event.key === null ? null : event.newValue;
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches;
  setTheme(savedTheme ? savedTheme === 'dark' : Boolean(prefersDark), false);
}

onMounted(() => window.addEventListener('storage', syncTheme));
onUnmounted(() => window.removeEventListener('storage', syncTheme));
</script>

<template>
  <button class="theme-toggle" type="button" :aria-pressed="darkMode" @click="toggleTheme">
    <span class="theme-toggle-icon" aria-hidden="true">{{ darkMode ? '☀' : '☾' }}</span>
    <span>{{ darkMode ? 'Modo claro' : 'Modo oscuro' }}</span>
  </button>
</template>

<style scoped>
.theme-toggle { display:flex; width:100%; min-height:44px; align-items:center; gap:11px; padding:0 12px; border:1px solid transparent; border-radius:12px; background:transparent; color:var(--text-soft); cursor:pointer; font:700 .82rem "Montserrat",Arial,sans-serif; text-align:left; transition:background-color .18s ease,color .18s ease,border-color .18s ease; }
.theme-toggle:hover { border-color:var(--border); background:var(--primary-soft); color:var(--primary-strong); }
.theme-toggle:focus-visible { outline:3px solid var(--primary); outline-offset:2px; }
.theme-toggle-icon { display:grid; width:20px; flex:0 0 20px; place-items:center; color:var(--primary-strong); font-size:1.18rem; line-height:1; }
</style>
