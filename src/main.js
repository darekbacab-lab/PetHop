import { createApp } from 'vue';
import App from './App.vue';
import router from './router/index.js';
import { initAnalytics } from './services/analytics.js';
import './services/storage.js';
import '../css/styles.css';
import '../css/acceso.css';
import '../css/solicitud.css';

document.documentElement.dataset.theme = 'light';

initAnalytics();
createApp(App).use(router).mount('#app');
