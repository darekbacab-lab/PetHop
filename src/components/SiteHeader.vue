<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listFavorites, removeFavorite as deleteFavorite } from '../services/petApi.js';
import { currentUser, loadCurrentUser, logout } from '../services/auth.js';
import { api } from '../services/api.js';
import Icon from './Icon.vue';
const route = useRoute();
const router = useRouter();
const open = ref(false);
const menuToggle = ref(null);
const accountDisplayName = ref('');
const favoritesOpen = ref(false);
const favorites = ref([]);
const favoritesLoading = ref(false);
const favoritesError = ref('');
const favoritesActionError = ref('');
const notificationsOpen = ref(false);
const notifications = ref([]);
const notificationsLoading = ref(false);
const notificationsError = ref('');
const unreadNotifications = computed(() => notifications.value.filter(item => !item.readAt).length);
const count = computed(() => favorites.value.length);
const accountRoute = computed(() => '/perfil.html');
const profileLabel = computed(() => 'Mi perfil');
const links = [
  { label: 'Inicio', to: '/inicio.html', icon: 'house' },
  { label: 'Explorar', to: '/explorar.html', icon: 'search' },
  { label: 'Mensajes', to: '/mensajes.html', icon: 'messages-square' },
  { label: 'Seguimiento', to: '/seguimiento.html', icon: 'paw-print' },
  { label: 'Aliados', to: '/aliados.html', icon: 'building-2' },
  { label: 'Dar en adopción', to: '/proveedor.html', icon: 'hand-heart' }
];
function isNavActive(item) {
  if (item.to.includes('?')) return route.fullPath === item.to;
  if (item.to === '/proveedor.html' && route.query.section === 'mensajes') return false;
  return route.path === item.to;
}
watch(open, value => document.body.classList.toggle('menu-open', value));
watch(() => route.fullPath, () => closeMenu());
async function refreshAccountDisplayName() {
  const user = currentUser.value;
  accountDisplayName.value = user?.fullName || '';
  if (user?.role !== 'SHELTER') return;
  try {
    const shelter = await api('/shelters/me');
    if (currentUser.value?.id === user.id) accountDisplayName.value = shelter.organizationName || user.fullName || '';
  } catch { /* A new shelter account may not have its organization profile yet. */ }
}
watch(() => [currentUser.value?.id, currentUser.value?.role], refreshAccountDisplayName, { immediate: true });
async function refreshNotificationCount() {
  if (!currentUser.value) { notifications.value = []; return; }
  try { const page = await api('/notifications?page=0&size=30'); notifications.value = page?.content || []; } catch { /* The full panel reports the actionable error. */ }
}
watch(() => currentUser.value?.id, refreshNotificationCount, { immediate: true });
function onShelterUpdated() { refreshAccountDisplayName(); }
function closeMenu() {
  open.value = false;
  nextTick(() => menuToggle.value?.focus());
}
async function signOut() {
  try { await logout(); }
  finally { closeMenu(); await router.push('/acceso.html'); }
}
async function openFavorites() {
  closeMenu();
  favoritesOpen.value = true;
  favoritesLoading.value = true;
  favoritesError.value = '';
  try {
    favorites.value = await listFavorites();
  } catch (error) {
    favorites.value = [];
    favoritesError.value = error.status === 401
      ? 'Inicia sesión como adoptante para consultar tus mascotas guardadas.'
      : error.status === 403
        ? 'Los favoritos están disponibles para cuentas adoptantes.'
        : error.message || 'No se pudieron cargar tus favoritos.';
  } finally {
    favoritesLoading.value = false;
  }
}
async function removeFavorite(id) {
  favoritesActionError.value = '';
  try {
    await deleteFavorite(id);
    favorites.value = favorites.value.filter(pet => String(pet.id) !== String(id));
  } catch (error) {
    favoritesActionError.value = error.message || 'No se pudo quitar la mascota de favoritos.';
  }
}
async function openNotifications() {
  closeMenu(); notificationsOpen.value = true; notificationsLoading.value = true; notificationsError.value = '';
  try { const page = await api('/notifications?page=0&size=30'); notifications.value = page?.content || []; }
  catch (error) { notifications.value = []; notificationsError.value = error.message || 'No se pudieron cargar tus notificaciones.'; }
  finally { notificationsLoading.value = false; }
}
async function markNotificationRead(item) {
  if (item.readAt) return;
  try { await api(`/notifications/${item.id}/read`, { method: 'PATCH' }); item.readAt = new Date().toISOString(); }
  catch (error) { notificationsError.value = error.message || 'No se pudo marcar la notificación.'; }
}
function petPhoto(pet) {
  const photo = pet.photos?.[0] || pet.image;
  if (!photo) return '';
  return /^(data:|https?:\/\/|\/)/i.test(photo) ? photo : `/img/${photo}`;
}
onUnmounted(() => document.body.classList.remove('menu-open'));
onMounted(() => {
  window.addEventListener('pethop:shelter-updated', onShelterUpdated);
  loadCurrentUser().catch(() => {});
});
onUnmounted(() => window.removeEventListener('pethop:shelter-updated', onShelterUpdated));
</script>
<template>
  <header class="site-header" @keydown.esc.window="closeMenu"><div class="header-container">
    <button ref="menuToggle" type="button" class="menu-toggle" :class="{ 'menu-toggle-open': open }" :aria-expanded="open" aria-controls="main-menu" :aria-label="open ? 'Cerrar menú' : 'Abrir menú'" @click="open = !open"><Icon :name="open ? 'x' : 'menu'" /></button>
    <RouterLink to="/inicio.html" class="brand" aria-label="PetHop"><img class="brand-logo-color" src="/img/image1.png" alt=""><img class="brand-logo-mono" src="/img/Logomonocr.png" alt=""></RouterLink>
    <nav id="main-menu" class="main-nav" :class="{ open }" :inert="!open" aria-label="Navegación principal">
      <div class="menu-drawer-header"><div class="menu-drawer-brand"><img class="brand-logo-color" src="/img/image1.png" alt=""><img class="brand-logo-mono" src="/img/Logomonocr.png" alt=""></div><button type="button" class="menu-drawer-close" aria-label="Cerrar menú" @click="closeMenu"><Icon name="x" /></button></div>
      <template v-for="(item, index) in links" :key="item.to"><RouterLink class="nav-link" :class="{ active: isNavActive(item) }" :to="item.to" @click="closeMenu"><Icon :name="item.icon" class="nav-item-icon" /><span>{{ item.label }}</span></RouterLink><button v-if="index === 1" type="button" class="favorites-button menu-favorites" @click="openFavorites"><Icon name="heart" class="nav-item-icon" /><span class="favorites-label">Favoritos</span><span class="favorites-count">{{ count }}</span></button></template>
      <template v-if="currentUser"><RouterLink class="nav-link account-nav-link" :to="accountRoute" @click="closeMenu"><Icon name="user-round" class="nav-item-icon" /><span>{{ profileLabel }}</span></RouterLink></template>
      <RouterLink v-if="currentUser?.role === 'ADMIN'" class="nav-link account-nav-link" to="/admin.html" @click="closeMenu"><Icon name="shield-check" class="nav-item-icon" /><span>Administración</span></RouterLink>
      <button v-if="currentUser" type="button" class="nav-link account-nav-signout" @click="signOut"><Icon name="log-out" class="nav-item-icon" /><span>Cerrar sesión</span></button>
    </nav>
    <div class="header-right"><RouterLink to="/inicio.html#como-funciona" class="how-button">Cómo funciona</RouterLink><span v-if="currentUser" class="account-display-name" :title="accountDisplayName">{{ accountDisplayName }}</span><RouterLink v-if="!currentUser" to="/acceso.html" class="login-button"><Icon name="log-in" />Ingresar</RouterLink><template v-else><RouterLink :to="accountRoute" class="account-trigger" aria-label="Abrir mi perfil"><span class="account-avatar"><Icon name="user-round" /></span><span class="account-presence" aria-label="Sesión activa"></span></RouterLink><button type="button" class="notification-trigger" aria-label="Abrir notificaciones" :aria-expanded="notificationsOpen" @click="openNotifications"><Icon name="bell" /><span v-if="unreadNotifications" class="notification-badge">{{ unreadNotifications > 9 ? '9+' : unreadNotifications }}</span></button></template></div>
  </div></header><button v-if="open" class="menu-backdrop is-visible" type="button" aria-label="Cerrar menú" @click="closeMenu"></button>
  <div v-if="favoritesOpen" class="dynamic-modal open favorites-modal" role="presentation" @click.self="favoritesOpen=false"><section class="dynamic-modal-content favorites-modal-content" role="dialog" aria-modal="true" aria-labelledby="favorites-title" :aria-busy="favoritesLoading" @keydown.esc="favoritesOpen=false"><button type="button" class="dynamic-modal-close" aria-label="Cerrar favoritos" @click="favoritesOpen=false"><Icon name="x" /></button><span class="favorites-modal-eyebrow">TUS MASCOTAS GUARDADAS</span><h2 id="favorites-title">Favoritos <span class="favorites-modal-count">{{ count }}</span></h2><p v-if="favoritesLoading" class="favorites-empty-copy" role="status">Cargando tus mascotas guardadas…</p><p v-else-if="favoritesError" class="favorites-empty-copy" role="alert">{{ favoritesError }}</p><p v-else-if="!favorites.length" class="favorites-empty-copy">Aún no tienes mascotas guardadas. Explora y guarda las que te gustaría conocer.</p><p v-if="favoritesActionError" class="favorites-empty-copy" role="alert">{{ favoritesActionError }}</p><template v-if="!favoritesLoading && !favoritesError && favorites.length"><p class="favorites-modal-intro">Tus mascotas guardadas están aquí para que vuelvas a ellas cuando quieras.</p><div class="dynamic-modal-list favorites-modal-list"><article v-for="pet in favorites" :key="pet.id" class="dynamic-modal-list-item favorites-modal-item"><img v-if="petPhoto(pet)" class="favorites-pet-thumbnail" :src="petPhoto(pet)" :alt="`Fotografía de ${pet.name}`" loading="lazy"><span v-else class="favorites-pet-thumbnail favorites-pet-thumbnail-empty" aria-hidden="true"><Icon name="paw-print" /></span><div class="favorites-pet-copy"><strong>{{ pet.name }}</strong><span>{{ pet.species }}<template v-if="pet.location"> · {{ pet.location }}</template></span></div><div class="favorites-pet-actions"><RouterLink class="pet-button favorites-profile-link" :to="{ path: '/mascota.html', query: { pet: pet.id } }" @click="favoritesOpen=false">Ver perfil</RouterLink><button class="favorites-remove-button" type="button" :aria-label="`Quitar a ${pet.name} de favoritos`" @click="removeFavorite(pet.id)"><Icon name="heart" :filled="true" /><span>Quitar</span></button></div></article></div></template><div v-if="!favoritesLoading && !favoritesError && !favorites.length" class="favorites-empty-state"><span class="favorites-empty-icon"><Icon name="paw-print" :size="27" /></span><RouterLink to="/explorar.html" class="primary-button favorites-explore-link" @click="favoritesOpen=false">Explorar mascotas<Icon name="arrow-right" :size="17" /></RouterLink></div></section></div>
  <div v-if="notificationsOpen" class="dynamic-modal open favorites-modal" role="presentation" @click.self="notificationsOpen=false"><section class="dynamic-modal-content favorites-modal-content notification-modal" role="dialog" aria-modal="true" aria-labelledby="notifications-title" :aria-busy="notificationsLoading"><button type="button" class="dynamic-modal-close" aria-label="Cerrar notificaciones" @click="notificationsOpen=false"><Icon name="x" /></button><span class="favorites-modal-eyebrow">ACTIVIDAD DE PET HOP</span><h2 id="notifications-title">Notificaciones <span class="favorites-modal-count">{{ unreadNotifications }}</span></h2><p v-if="notificationsLoading" class="favorites-empty-copy" role="status">Cargando notificaciones…</p><p v-else-if="notificationsError" class="favorites-empty-copy" role="alert">{{ notificationsError }}</p><p v-else-if="!notifications.length" class="favorites-empty-copy">No tienes notificaciones nuevas.</p><div v-else class="notification-list"><button v-for="item in notifications" :key="item.id" type="button" class="notification-item" :class="{ 'is-unread': !item.readAt }" @click="markNotificationRead(item)"><span class="notification-icon"><Icon name="bell" /></span><span><strong>{{ item.title }}</strong><small>{{ item.message }}</small><time>{{ new Date(item.createdAt).toLocaleString('es-MX') }}</time></span></button></div></section></div>
</template>

<style scoped>
.brand-logo-mono { display:none!important; }
.account-trigger { position:relative; display:grid; width:48px; height:48px; place-items:center; padding:0; border:1px solid var(--border); border-radius:14px; background:var(--white); color:var(--primary-strong); text-decoration:none; }
.account-trigger:hover { border-color:var(--primary); background:var(--background); }
.account-trigger:focus-visible { outline:3px solid color-mix(in srgb,var(--primary) 45%,transparent); outline-offset:2px; }
.account-avatar { display:grid; width:31px; height:31px; place-items:center; border-radius:50%; background:#e7f5f5; }
.account-avatar :deep(svg) { width:18px; height:18px; }
.account-presence { position:absolute; right:3px; bottom:3px; width:10px; height:10px; border:2px solid var(--white); border-radius:50%; background:#3dba7c; }
.account-display-name { max-width:170px; overflow:hidden; color:var(--text); font-size:.78rem; font-weight:750; text-overflow:ellipsis; white-space:nowrap; }
.notification-trigger { position:relative; display:grid; width:48px; height:48px; place-items:center; border:1px solid var(--border); border-radius:14px; background:var(--white); color:var(--primary-strong); cursor:pointer; }.notification-trigger:hover { border-color:var(--primary); background:var(--background); }.notification-trigger :deep(svg) { width:18px; height:18px; }.notification-badge { position:absolute; top:-5px; right:-5px; display:grid; min-width:18px; height:18px; place-items:center; padding:0 4px; border:2px solid var(--white); border-radius:99px; background:var(--danger); color:#fff; font-size:.58rem; font-weight:800; }
.main-nav .account-nav-signout { width:100%; min-height:44px; display:flex; align-items:center; gap:10px; padding:10px 14px; border:0; border-radius:10px; background:transparent; color:var(--danger); cursor:pointer; font:750 .82rem "Montserrat",Arial,sans-serif; text-align:left; transition:var(--transition); }
.main-nav .account-nav-signout:hover { background:var(--primary-soft); color:var(--danger); }
.main-nav .account-nav-signout:focus-visible { outline:3px solid color-mix(in srgb,var(--primary) 45%,transparent); outline-offset:2px; }
.main-nav .account-nav-signout .nav-item-icon { width:19px; height:19px; flex:0 0 auto; }
.favorites-modal-content { width:min(560px,100%); padding:32px; }
.favorites-modal-eyebrow { display:block; margin-bottom:5px; color:var(--primary-strong); font-size:.68rem; font-weight:800; letter-spacing:.09em; }
.favorites-modal h2 { display:flex; align-items:center; gap:10px; margin-bottom:5px; font-size:1.75rem; }
.favorites-modal-count { display:grid; place-items:center; min-width:27px; height:27px; padding:0 7px; border-radius:20px; background:#e8f6f5; color:var(--primary-strong); font-size:.76rem; }
 .favorites-modal-count { background:var(--button-primary-bg); color:#fff; }
.favorites-modal .favorites-modal-intro { margin-bottom:18px; font-size:.82rem; }
.favorites-modal-list { max-height:min(440px,55vh); overflow-y:auto; gap:10px; padding:1px 3px 1px 1px; }
.favorites-modal-item { display:grid; grid-template-columns:62px minmax(0,1fr) auto; gap:13px; min-height:82px; padding:10px; border-radius:13px; background:var(--white); }
.favorites-pet-thumbnail { width:62px; height:62px; border-radius:9px; object-fit:cover; background:var(--background); }
.favorites-pet-thumbnail-empty { display:grid; place-items:center; color:var(--primary-strong); }
.favorites-pet-copy { display:flex; min-width:0; flex-direction:column; justify-content:center; gap:3px; }
.favorites-pet-copy strong { font-size:.98rem; line-height:1.3; }
.favorites-pet-copy span { overflow:hidden; color:var(--text-soft); font-size:.72rem; text-overflow:ellipsis; white-space:nowrap; }
.favorites-pet-actions { display:flex; align-items:center; justify-content:flex-end; gap:8px; }
.favorites-profile-link { display:inline-flex; min-height:38px; align-items:center; justify-content:center; padding:0 12px; border-radius:9px; background:var(--primary-strong); color:var(--white); font-size:.72rem; font-weight:800; text-decoration:none; white-space:nowrap; }
.favorites-profile-link:hover { background:var(--primary-strong-hover); }
.favorites-remove-button { display:inline-flex; min-height:38px; align-items:center; justify-content:center; gap:5px; padding:0 10px; border:1px solid var(--border); border-radius:9px; background:var(--white); color:var(--text-soft); cursor:pointer; font:700 .7rem "Montserrat",Arial,sans-serif; white-space:nowrap; }
.favorites-remove-button:hover { border-color:var(--danger); color:var(--danger); }
.favorites-remove-button :deep(svg) { width:15px; height:15px; }
.favorites-empty-copy { margin:12px 0 0!important; font-size:.84rem!important; line-height:1.6; }
.favorites-empty-state { display:grid; justify-items:center; gap:14px; padding:22px 0 3px; }
.favorites-empty-icon { display:grid; width:58px; height:58px; place-items:center; border-radius:50%; background:#e8f6f5; color:var(--primary-strong); }
.favorites-explore-link { display:inline-flex; align-items:center; justify-content:center; gap:9px; text-decoration:none; }
.notification-list { display:grid; max-height:min(480px,58vh); overflow:auto; gap:7px; margin-top:14px; }.notification-item { display:grid; grid-template-columns:34px minmax(0,1fr); gap:10px; width:100%; padding:12px; border:1px solid transparent; border-radius:12px; background:var(--background); color:var(--text); text-align:left; cursor:pointer; }.notification-item:hover,.notification-item.is-unread { border-color:var(--border); background:#f0faf9; }.notification-icon { display:grid; width:32px; height:32px; place-items:center; border-radius:10px; background:#dff2f1; color:var(--primary-strong); }.notification-item > span:last-child { display:grid; gap:3px; min-width:0; }.notification-item strong { font-size:.76rem; }.notification-item small { color:var(--text-soft); font-size:.68rem; line-height:1.45; }.notification-item time { color:var(--text-soft); font-size:.61rem; }
@media(max-width:520px) {
  .account-display-name { max-width:105px; }
  .favorites-modal-content { max-height:calc(100dvh - 24px); padding:26px 18px 20px; }
  .favorites-modal-item { grid-template-columns:54px minmax(0,1fr); gap:10px; }
  .favorites-pet-thumbnail { width:54px; height:54px; }
  .favorites-pet-actions { grid-column:1/-1; justify-content:stretch; }
  .favorites-profile-link,.favorites-remove-button { flex:1; }
  .favorites-modal-list { max-height:50dvh; }
}
@media(max-width:380px) { .header-right { gap:5px; } .account-display-name { max-width:55px; } .account-trigger,.notification-trigger { width:44px; height:44px; border-radius:12px; } }
</style>
