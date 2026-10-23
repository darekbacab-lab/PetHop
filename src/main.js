import { createApp } from 'vue';
import App from './App.vue';
import router from './router/index.js';
import './services/storage.js';
import '../css/styles.css';
import '../css/acceso.css';
import '../css/solicitud.css';

createApp(App).use(router).mount('#app');
