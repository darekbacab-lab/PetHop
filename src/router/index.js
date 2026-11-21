import { createRouter, createWebHistory } from 'vue-router';
import { ref } from 'vue';
import { trackPageView } from '../services/analytics.js';

export const routeLoading = ref(false);
export const routeLoadingName = ref('inicio');
let pendingFullPath = '';

const routes = [
  { path: '/', redirect: '/inicio.html' },
  { path: '/inicio.html', name: 'inicio', meta: { title: 'PetHop | Adopta, conecta, cambia vidas', description: 'PetHop conecta adoptantes con refugios y rescatistas: explora mascotas en adopción y encuentra a tu próximo compañero.' }, component: () => import('../views/HomeView.vue') },
  { path: '/index.html', redirect: '/inicio.html' },
  { path: '/acceso.html', name: 'acceso', meta: { title: 'Ingresar | PetHop', pageTitle: 'Acceso', description: 'Ingresa a PetHop o crea tu cuenta como adoptante o refugio.' }, component: () => import('../views/AccessView.vue') },
  { path: '/explorar.html', name: 'explorar', meta: { title: 'Explorar mascotas | PetHop', pageTitle: 'Explorar', description: 'Explora las mascotas publicadas por refugios y encuentra a tu próximo compañero.' }, component: () => import('../views/ExploreView.vue') },
  { path: '/mascota.html', name: 'mascota', meta: { title: 'Mascota | PetHop', pageTitle: 'Mascota', description: 'Conoce la historia, la salud y el proceso de adopción de esta mascota.' }, component: () => import('../views/PetView.vue') },
  { path: '/solicitud.html', name: 'solicitud', meta: { title: 'Solicitud de adopción | PetHop', pageTitle: 'Solicitud de adopción', description: 'Envía tu solicitud de adopción y sigue su proceso con el refugio.' }, component: () => import('../views/AdoptionView.vue') },
  { path: '/seguimiento.html', name: 'seguimiento', meta: { title: 'Seguimiento | PetHop', pageTitle: 'Seguimiento', description: 'Sigue el proceso de tus solicitudes y adopciones en PetHop.' }, component: () => import('../views/FollowUpView.vue') },
  { path: '/mensajes.html', name: 'mensajes', meta: { title: 'Mensajes | PetHop', pageTitle: 'Tus Mensajes', description: 'Tus conversaciones con los refugios sobre las mascotas.' }, component: () => import('../views/AdopterMessagesView.vue') },
  { path: '/perfil.html', name: 'perfil', meta: { title: 'Mi perfil | PetHop', pageTitle: 'Mi perfil', description: 'Tu perfil, tus favoritos y tus solicitudes de adopción.' }, component: () => import('../views/AdopterProfileView.vue') },
  { path: '/salud.html', name: 'salud', meta: { title: 'Expediente sanitario | PetHop', pageTitle: 'Expediente sanitario', description: 'Expediente sanitario de la mascota: vacunas, consultas y tratamientos.' }, component: () => import('../views/HealthRecordsView.vue') },
  { path: '/aliados.html', name: 'aliados', meta: { title: 'Aliados | PetHop', pageTitle: 'Aliados', description: 'Aliados de PetHop: servicios y beneficios para las familias que adoptan.' }, component: () => import('../views/AffiliatesView.vue') },
  { path: '/refugio.html', name: 'refugio', meta: { title: 'Dar en adopción | PetHop', pageTitle: 'Refugio', description: 'Publica mascotas, organiza solicitudes y centraliza tus adopciones.' }, component: () => import('../views/ShelterView.vue') },
  { path: '/admin.html', name: 'admin', meta: { title: 'Moderación | PetHop', pageTitle: 'Moderación', description: 'Moderación de PetHop: refugios, mascotas y solicitudes por revisar.' }, component: () => import('../views/AdminView.vue') },
  { path: '/privacidad.html', name: 'privacidad', meta: { title: 'Aviso de privacidad | PetHop', pageTitle: 'Aviso de privacidad', description: 'Cómo PetHop recaba, usa y protege los datos personales de adoptantes y refugios.' }, component: () => import('../views/PrivacyView.vue') },
  { path: '/terminos.html', name: 'terminos', meta: { title: 'Términos y condiciones | PetHop', pageTitle: 'Términos y condiciones', description: 'Reglas de uso de PetHop para adoptantes, refugios y personas administradoras.' }, component: () => import('../views/TermsView.vue') },
  { path: '/:pathMatch(.*)*', name: 'not-found', meta: { title: 'Página no encontrada | PetHop', pageTitle: 'Página no encontrada', description: 'Esta página no existe o se movió. Vuelve al inicio o explora las mascotas en adopción.' }, component: () => import('../views/NotFoundView.vue') }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition;
    if (to.hash) return { el: to.hash, behavior: 'smooth' };
    return { top: 0 };
  }
});

router.beforeEach(to => {
  pendingFullPath = to.fullPath;
  routeLoadingName.value = String(to.name || 'inicio');
  routeLoading.value = true;
});

router.afterEach((to, from) => {
  if (to.fullPath === pendingFullPath) routeLoading.value = false;
  document.title = to.meta.title || 'PetHop';
  trackPageView(to.fullPath);
  if (to.meta.description) {
    let meta = document.querySelector('meta[name="description"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.setAttribute('name', 'description');
      document.head.appendChild(meta);
    }
    meta.setAttribute('content', to.meta.description);
  }
  if (to.path !== from.path) {
    const main = document.querySelector('main');
    if (main) { main.setAttribute('tabindex', '-1'); main.focus({ preventScroll: true }); }
  }
});

router.onError(() => { routeLoading.value = false; });

export default router;
