import { createRouter, createWebHistory } from 'vue-router';
import { ref } from 'vue';

export const routeLoading = ref(false);
export const routeLoadingName = ref('inicio');
let pendingFullPath = '';

const routes = [
  { path: '/', name: 'inicio', meta: { title: 'PetHop | Adopta, conecta, cambia vidas' }, component: () => import('../views/HomeView.vue') },
  { path: '/index.html', redirect: '/' },
  { path: '/acceso.html', name: 'acceso', meta: { title: 'Ingresar | PetHop' }, component: () => import('../views/AccessView.vue') },
  { path: '/explorar.html', name: 'explorar', meta: { title: 'Explorar mascotas | PetHop' }, component: () => import('../views/ExploreView.vue') },
  { path: '/mascota.html', name: 'mascota', meta: { title: 'Mascota | PetHop' }, component: () => import('../views/PetView.vue') },
  { path: '/solicitud.html', name: 'solicitud', meta: { title: 'Solicitud de adopción | PetHop' }, component: () => import('../views/AdoptionView.vue') },
  { path: '/seguimiento.html', name: 'seguimiento', meta: { title: 'Seguimiento | PetHop' }, component: () => import('../views/FollowUpView.vue') },
  { path: '/mensajes.html', name: 'mensajes', meta: { title: 'Mensajes | PetHop' }, component: () => import('../views/AdopterMessagesView.vue') },
  { path: '/proveedor.html', name: 'proveedor', meta: { title: 'Dar en adopción | PetHop' }, component: () => import('../views/ProviderView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
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

router.afterEach(to => {
  if (to.fullPath === pendingFullPath) routeLoading.value = false;
  document.title = to.meta.title || 'PetHop';
});

router.onError(() => { routeLoading.value = false; });

export default router;
