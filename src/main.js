import { createApp } from 'vue';
import App from './App.vue';
import router from './router/index.js';
import './services/storage.js';
import '../css/styles.css';
import '../css/acceso.css';
import '../css/solicitud.css';

try {
  const savedTheme = localStorage.getItem('pethop_theme');
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches;
  document.documentElement.dataset.theme = ['dark', 'light'].includes(savedTheme) ? savedTheme : prefersDark ? 'dark' : 'light';
} catch {
  document.documentElement.dataset.theme = 'light';
}

createApp(App).use(router).mount('#app');
