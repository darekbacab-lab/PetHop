<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listFavorites, removeFavorite as deleteFavorite } from '../services/petApi.js';
import { currentUser, loadCurrentUser, logout } from '../services/auth.js';
import { api } from '../services/api.js';
import Icon from './Icon.vue';
import { useFocusTrap } from '../services/dialogFocus.js';
const route = useRoute();
const router = useRouter();
const open = ref(false);
const menuToggle = ref(null);
const accountDisplayName = ref('');
const favoritesOpen = ref(false);
const favorites = ref([]);
const favoritesLoading = ref(false);
const favoritesError = ref('');
const favoritesLoginRequired = ref(false);
const favoritesActionError = ref('');
const notificationsOpen = ref(false);
const favoritesDialog = ref(null);
const notificationsDialog = ref(null);
useFocusTrap(favoritesDialog, favoritesOpen, () => { favoritesOpen.value = false; });
useFocusTrap(notificationsDialog, notificationsOpen, () => { notificationsOpen.value = false; });
const notifications = ref([]);
const notificationsLoading = ref(false);
const notificationsError = ref('');
const unreadNotifications = computed(() => notifications.value.filter(item => !item.readAt).length);
const count = computed(() => favorites.value.length);
const accountRoute = computed(() => '/perfil.html');
const profileLabel = computed(() => 'Mi perfil');
const isHome = computed(() => route.name === 'inicio');
const pageTitleParts = computed(() => {
  const title = route.meta?.pageTitle;
  if (!title) return null;
  const words = String(title).split(' ');
  if (words.length === 1) return { dark: '> ', accent: words[0] };
  return { dark: `> ${words.slice(0, -1).join(' ')} `, accent: words.at(-1) };
});
const links = computed(() => [
  { label: 'Inicio', to: '/inicio.html', icon: 'house' },
  { label: 'Explorar', to: '/explorar.html', icon: 'search' },
  { label: 'Mensajes', to: currentUser.value?.role === 'SHELTER' ? '/refugio.html?section=mensajes' : '/mensajes.html', icon: 'messages-square' },
  { label: 'Seguimiento', to: currentUser.value?.role === 'SHELTER' ? '/refugio.html?section=solicitudes' : '/seguimiento.html', icon: 'paw-print', hint: currentUser.value?.role === 'SHELTER' ? 'Seguimiento: solicitudes recibidas por tu refugio' : 'Seguimiento: tus solicitudes de adopción' },
  { label: 'Aliados', to: '/aliados.html', icon: 'building-2' },
  { label: 'Dar en adopción', to: '/refugio.html', icon: 'hand-heart' }
]);
function isNavActive(item) {
  if (item.to.includes('?')) return route.fullPath === item.to;
  if (item.to === '/refugio.html' && route.query.section) return false;
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
function closeMenu(returnFocus = false) {
  open.value = false;
  if (returnFocus) nextTick(() => menuToggle.value?.focus());
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
  favoritesLoginRequired.value = false;
  try {
    favorites.value = await listFavorites();
  } catch (error) {
    favorites.value = [];
    favoritesLoginRequired.value = error.status === 401;
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
function notificationDestination(item) {
  const refId = item.referenceId || '';
  switch (item.type) {
    case 'APPLICATION_SUBMITTED': return '/refugio.html?section=solicitudes';
    case 'APPLICATION_STATUS_CHANGED': return refId ? `/seguimiento.html?application=${refId}` : '/seguimiento.html';
    case 'ADOPTION_FORMALIZED':
    case 'FOLLOW_UP_CREATED': return refId ? `/seguimiento.html?application=${refId}` : '/seguimiento.html';
    case 'NEW_MESSAGE': return currentUser.value?.role === 'SHELTER' ? '/refugio.html?section=mensajes' : (refId ? `/mensajes.html?conversation=${refId}` : '/mensajes.html');
    case 'PET_MODERATION': return refId ? `/mascota.html?pet=${refId}` : '/refugio.html?section=mascotas';
    case 'HEALTH_REMINDER': return refId ? `/salud.html?pet=${refId}` : '/salud.html';
    case 'SHELTER_VERIFICATION': return '/refugio.html?section=perfil';
    default: return '';
  }
}
async function openNotification(item) {
  const destination = notificationDestination(item);
  if (!item.readAt) {
    try { await api(`/notifications/${item.id}/read`, { method: 'PATCH' }); item.readAt = new Date().toISOString(); }
    catch (error) { notificationsError.value = error.message || 'No se pudo marcar la notificación.'; }
  }
  notificationsOpen.value = false;
  if (destination) await router.push(destination);
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
  <a class="skip-link" href="#main-content">Saltar al contenido</a>
  <header class="site-header" @keydown.esc.window="closeMenu(true)"><div class="header-container">
    <button ref="menuToggle" type="button" class="menu-toggle" :class="{ 'menu-toggle-open': open }" :aria-expanded="open" aria-controls="main-menu" :aria-label="open ? 'Cerrar menú' : 'Abrir menú'" @click="open = !open"><Icon :name="open ? 'x' : 'menu'" /></button>
    <RouterLink to="/inicio.html" class="brand" aria-label="PetHop"><img class="brand-logo-color" src="/img/image1.png" alt=""><img class="brand-logo-mono" src="/img/Logomonocr.png" alt=""></RouterLink>
    <p v-if="pageTitleParts" class="header-page-title"><span class="title-dark">{{ pageTitleParts.dark }}</span><span class="title-accent">{{ pageTitleParts.accent }}</span></p>
    <nav id="main-menu" class="main-nav" :class="{ open }" :inert="!open" aria-label="Navegación principal">
      <div class="menu-drawer-header"><div class="menu-drawer-brand"><img class="brand-logo-color" src="/img/image1.png" alt=""><img class="brand-logo-mono" src="/img/Logomonocr.png" alt=""></div><button type="button" class="menu-drawer-close" aria-label="Cerrar menú" @click="closeMenu(true)"><Icon name="x" /></button></div>
      <template v-for="(item, index) in links" :key="item.to"><RouterLink class="nav-link" :class="{ active: isNavActive(item) }" :to="item.to" :aria-current="isNavActive(item) ? 'page' : undefined" :aria-label="item.hint || undefined" :title="item.hint || undefined" @click="closeMenu"><Icon :name="item.icon" class="nav-item-icon" /><span>{{ item.label }}</span></RouterLink><button v-if="index === 1 && (!currentUser || currentUser.role === 'ADOPTER')" type="button" class="favorites-button menu-favorites" @click="openFavorites"><Icon name="heart" class="nav-item-icon" /><span class="favorites-label">Favoritos</span><span class="favorites-count" aria-live="polite">{{ count }}</span></button></template>
      <template v-if="currentUser"><RouterLink class="nav-link account-nav-link" :to="accountRoute" :aria-current="route.path === accountRoute ? 'page' : undefined" @click="closeMenu"><Icon name="user-round" class="nav-item-icon" /><span>{{ profileLabel }}</span></RouterLink></template>
      <RouterLink v-if="currentUser?.role === 'ADMIN'" class="nav-link account-nav-link" to="/admin.html" :aria-current="route.path === '/admin.html' ? 'page' : undefined" @click="closeMenu"><Icon name="shield-check" class="nav-item-icon" /><span>Administración</span></RouterLink>
      <button v-if="currentUser" type="button" class="nav-link account-nav-signout" @click="signOut"><Icon name="log-out" class="nav-item-icon" /><span>Cerrar sesión</span></button>
    </nav>
    <div class="header-right"><RouterLink to="/inicio.html#como-funciona" class="how-button">Cómo funciona</RouterLink><span v-if="currentUser" class="account-display-name" :title="accountDisplayName">{{ accountDisplayName }}</span><RouterLink v-if="!currentUser" to="/acceso.html" class="login-button"><Icon name="log-in" />Ingresar</RouterLink><template v-else><RouterLink :to="accountRoute" class="account-trigger" aria-label="Abrir mi perfil"><span class="account-avatar"><Icon name="user-round" /></span><span class="account-presence" aria-label="Sesión activa"></span></RouterLink><button type="button" class="notification-trigger" :aria-label="unreadNotifications ? `Abrir notificaciones, ${unreadNotifications} sin leer` : 'Abrir notificaciones'" :aria-expanded="notificationsOpen" @click="openNotifications"><Icon name="bell" /><span v-if="unreadNotifications" class="notification-badge">{{ unreadNotifications > 9 ? '9+' : unreadNotifications }}</span></button></template></div>
  </div></header>
  <nav v-if="isHome" class="side-rail" aria-label="Menú principal">
    <RouterLink to="/inicio.html" class="side-rail-brand" aria-label="PetHop, ir a Inicio"><Icon name="paw-print" class="side-rail-brand-icon" /></RouterLink>
    <div class="side-rail-group">
      <template v-for="item in links" :key="item.to"><RouterLink class="side-rail-item" :class="{ active: isNavActive(item) }" :to="item.to" :aria-current="isNavActive(item) ? 'page' : undefined" :aria-label="item.label"><Icon :name="item.icon" class="side-rail-icon" /><span class="side-rail-label" aria-hidden="true">{{ item.label }}</span></RouterLink></template>
      <button v-if="!currentUser || currentUser.role === 'ADOPTER'" type="button" class="side-rail-item" aria-label="Favoritos" @click="openFavorites"><Icon name="heart" class="side-rail-icon" /><span v-if="count" class="side-rail-badge">{{ count > 9 ? '9+' : count }}</span><span class="side-rail-label" aria-hidden="true">Favoritos</span></button>
    </div>
    <div class="side-rail-group side-rail-bottom">
      <button v-if="currentUser" type="button" class="side-rail-item" :aria-label="unreadNotifications ? `Abrir notificaciones, ${unreadNotifications} sin leer` : 'Abrir notificaciones'" :aria-expanded="notificationsOpen" @click="openNotifications"><Icon name="bell" class="side-rail-icon" /><span v-if="unreadNotifications" class="side-rail-badge">{{ unreadNotifications > 9 ? '9+' : unreadNotifications }}</span><span class="side-rail-label" aria-hidden="true">Notificaciones</span></button>
      <RouterLink v-if="currentUser" class="side-rail-item" :class="{ active: route.path === accountRoute }" :to="accountRoute" :aria-current="route.path === accountRoute ? 'page' : undefined" aria-label="Mi perfil"><Icon name="user-round" class="side-rail-icon" /><span class="side-rail-label" aria-hidden="true">Mi perfil</span></RouterLink>
      <RouterLink v-if="currentUser?.role === 'ADMIN'" class="side-rail-item" :class="{ active: route.path === '/admin.html' }" to="/admin.html" :aria-current="route.path === '/admin.html' ? 'page' : undefined" aria-label="Administración"><Icon name="shield-check" class="side-rail-icon" /><span class="side-rail-label" aria-hidden="true">Administración</span></RouterLink>
      <RouterLink v-if="!currentUser" class="side-rail-item" to="/acceso.html" aria-label="Ingresar"><Icon name="log-in" class="side-rail-icon" /><span class="side-rail-label" aria-hidden="true">Ingresar</span></RouterLink>
      <button v-if="currentUser" type="button" class="side-rail-item side-rail-signout" aria-label="Cerrar sesión" @click="signOut"><Icon name="log-out" class="side-rail-icon" /><span class="side-rail-label" aria-hidden="true">Cerrar sesión</span></button>
    </div>
  </nav>
  <button v-if="open" class="menu-backdrop is-visible" type="button" aria-label="Cerrar menú" @click="closeMenu(true)"></button>
  <Transition name="side-panel"><div v-if="favoritesOpen" class="dynamic-modal open favorites-modal side-panel-shell" role="presentation" @click.self="favoritesOpen=false" @keydown.esc.window="favoritesOpen=false"><section ref="favoritesDialog" class="dynamic-modal-content favorites-modal-content side-panel-content" role="dialog" aria-modal="true" aria-labelledby="favorites-title" :aria-busy="favoritesLoading"><button type="button" class="dynamic-modal-close" aria-label="Cerrar favoritos" @click="favoritesOpen=false"><Icon name="x" /></button><span class="favorites-modal-eyebrow">TUS MASCOTAS GUARDADAS</span><h2 id="favorites-title">Favoritos <span class="favorites-modal-count">{{ count }}</span></h2><p v-if="favoritesLoading" class="favorites-empty-copy" role="status">Cargando tus mascotas guardadas…</p><p v-else-if="favoritesError" class="favorites-empty-copy" role="alert">{{ favoritesError }}</p><RouterLink v-if="favoritesLoginRequired" class="button button-primary favorites-login-link" to="/acceso.html" @click="favoritesOpen=false">Ingresar</RouterLink><p v-else-if="!favorites.length" class="favorites-empty-copy">Aún no tienes mascotas guardadas. Explora y guarda las que te gustaría conocer.</p><p v-if="favoritesActionError" class="favorites-empty-copy" role="alert">{{ favoritesActionError }}</p><template v-if="!favoritesLoading && !favoritesError && favorites.length"><p class="favorites-modal-intro">Tus mascotas guardadas están aquí para que vuelvas a ellas cuando quieras.</p><div class="dynamic-modal-list favorites-modal-list"><article v-for="pet in favorites" :key="pet.id" class="dynamic-modal-list-item favorites-modal-item"><img v-if="petPhoto(pet)" class="favorites-pet-thumbnail" :src="petPhoto(pet)" :alt="`Fotografía de ${pet.name}`" loading="lazy"><span v-else class="favorites-pet-thumbnail favorites-pet-thumbnail-empty" aria-hidden="true"><Icon name="paw-print" /></span><div class="favorites-pet-copy"><strong>{{ pet.name }}</strong><span>{{ pet.species }}<template v-if="pet.location"> · {{ pet.location }}</template></span></div><div class="favorites-pet-actions"><RouterLink class="pet-button favorites-profile-link" :to="{ path: '/mascota.html', query: { pet: pet.id } }" @click="favoritesOpen=false">Ver perfil</RouterLink><button class="favorites-remove-button" type="button" :aria-label="`Quitar a ${pet.name} de favoritos`" @click="removeFavorite(pet.id)"><Icon name="heart" :filled="true" /><span>Quitar</span></button></div></article></div></template><div v-if="!favoritesLoading && !favoritesError && !favorites.length" class="favorites-empty-state"><span class="favorites-empty-icon"><Icon name="paw-print" :size="27" /></span><RouterLink to="/explorar.html" class="primary-button favorites-explore-link" @click="favoritesOpen=false">Explorar mascotas<Icon name="arrow-right" :size="17" /></RouterLink></div></section></div></Transition>
  <Transition name="side-panel"><div v-if="notificationsOpen" class="dynamic-modal open favorites-modal side-panel-shell" role="presentation" @click.self="notificationsOpen=false" @keydown.esc.window="notificationsOpen=false"><section ref="notificationsDialog" class="dynamic-modal-content favorites-modal-content side-panel-content notification-modal" role="dialog" aria-modal="true" aria-labelledby="notifications-title" :aria-busy="notificationsLoading"><button type="button" class="dynamic-modal-close" aria-label="Cerrar notificaciones" @click="notificationsOpen=false"><Icon name="x" /></button><span class="favorites-modal-eyebrow">ACTIVIDAD DE PET HOP</span><h2 id="notifications-title">Notificaciones <span class="favorites-modal-count">{{ unreadNotifications }}</span></h2><p v-if="notificationsLoading" class="favorites-empty-copy" role="status">Cargando notificaciones…</p><p v-else-if="notificationsError" class="favorites-empty-copy" role="alert">{{ notificationsError }}</p><p v-else-if="!notifications.length" class="favorites-empty-copy">No tienes notificaciones nuevas.</p><div v-else class="notification-list"><button v-for="item in notifications" :key="item.id" type="button" class="notification-item" :class="{ 'is-unread': !item.readAt }" @click="openNotification(item)"><span class="notification-icon"><Icon name="bell" /></span><span><strong>{{ item.title }}</strong><small>{{ item.message }}</small><time>{{ new Date(item.createdAt).toLocaleString('es-MX') }}</time></span></button></div></section></div></Transition>
</template>

<style scoped>
.header-page-title { margin:0 0 0 6px; overflow:hidden; font:800 1.4rem/1.2 "Montserrat",sans-serif; letter-spacing:-.02em; text-overflow:ellipsis; white-space:nowrap; }
.header-page-title .title-dark { color:var(--hero-title-dark); }
.header-page-title .title-accent { color:var(--primary); }
@media(max-width:900px) { .header-page-title { display:none; } }
.brand-logo-mono { display:none!important; }
.account-trigger { position:relative; display:grid; width:48px; height:48px; place-items:center; padding:0; border:1px solid var(--border); border-radius: var(--radius-sm); background:var(--white); color:var(--primary-strong); text-decoration:none; }
.account-trigger:hover { border-color:var(--primary); background:var(--background); }
.account-trigger:focus-visible { outline:3px solid color-mix(in srgb,var(--primary) 45%,transparent); outline-offset:2px; }
.account-avatar { display:grid; width:31px; height:31px; place-items:center; border-radius:50%; background:var(--tint); }
.account-avatar :deep(svg) { width:18px; height:18px; }
.account-presence { position:absolute; right:3px; bottom:3px; width:10px; height:10px; border:2px solid var(--white); border-radius:50%; background:var(--success); }
.account-display-name { max-width:170px; color:var(--text); font-size:.78rem; font-weight:750; }
.notification-trigger { position:relative; display:grid; width:48px; height:48px; place-items:center; border:1px solid var(--border); border-radius: var(--radius-sm); background:var(--white); color:var(--primary-strong); cursor:pointer; }.notification-trigger:hover { border-color:var(--primary); background:var(--background); }.notification-trigger :deep(svg) { width:18px; height:18px; }.notification-badge { position:absolute; top:-5px; right:-5px; display:grid; min-width:18px; height:18px; place-items:center; padding:0 4px; border:2px solid var(--white); border-radius:99px; background:var(--danger); color:var(--white); font-size:.75rem; font-weight:800; }
.side-panel-shell { align-items:stretch; justify-content:flex-end; padding:0; overflow:hidden; }
.side-panel-content { width:min(420px,92vw); height:100dvh; max-height:none; overflow-y:auto; padding:32px; border-top:0; border-right:0; border-bottom:0; border-radius:20px 0 0 20px; box-shadow:-12px 0 30px rgba(0,0,0,.25); transform:translateX(100%); transition:transform .25s ease-out; }
.dynamic-modal.open .side-panel-content { transform:translateX(0); }
.side-panel-enter-active,.side-panel-leave-active { transition:opacity .25s ease-out; }
.side-panel-enter-from,.side-panel-leave-to { opacity:0; }
.side-panel-shell.side-panel-enter-from .side-panel-content,.side-panel-shell.side-panel-leave-active .side-panel-content { transform:translateX(100%); }
.side-panel-enter-active .side-panel-content,.side-panel-leave-active .side-panel-content { transition:transform .25s ease-out; }
.main-nav .account-nav-signout { width:100%; min-height:44px; display:flex; align-items:center; gap:10px; padding:10px 14px; border:0; border-radius: var(--radius-sm); background:transparent; color:var(--danger); cursor:pointer; font:750 .82rem "Montserrat",Arial,sans-serif; text-align:left; transition:var(--transition); }
.main-nav .account-nav-signout:hover { background:var(--primary-soft); color:var(--danger); }
.main-nav .account-nav-signout:focus-visible { outline:3px solid color-mix(in srgb,var(--primary) 45%,transparent); outline-offset:2px; }
.main-nav .account-nav-signout .nav-item-icon { width:19px; height:19px; flex:0 0 auto; }
/* Animación al pasar el cursor sobre el menú lateral: el fondo entra
   deslizándose desde la izquierda y el contenido se mueve un poco a la
   derecha, como la etiqueta que aparece en el menú de la app. */
.main-nav a,
.main-nav .favorites-button,
.main-nav .account-nav-signout {
    position: relative;
    overflow: hidden;
}
.main-nav a::before,
.main-nav .favorites-button::before,
.main-nav .account-nav-signout::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: var(--tint);
    transform: translateX(-102%);
    transition: transform .25s ease;
    pointer-events: none;
}
.main-nav a:hover::before,
.main-nav .favorites-button:hover::before,
.main-nav .account-nav-signout:hover::before {
    transform: translateX(0);
}
.main-nav a.active::before {
    display: none;
}
.main-nav a:hover:not(.active),
.main-nav .favorites-button:hover,
.main-nav .account-nav-signout:hover {
    background: transparent;
}
.main-nav a > *,
.main-nav .favorites-button > *,
.main-nav .account-nav-signout > * {
    position: relative;
    transition: transform .25s ease;
}
.main-nav a:hover > *,
.main-nav .favorites-button:hover > *,
.main-nav .account-nav-signout:hover > * {
    transform: translateX(3px);
}
.main-nav .favorites-button {
    transition: color .25s ease;
}
/* Menú lateral fijo en Inicio (desktop): como el menú de la app, siempre
   visible a la izquierda, con la etiqueta que se desliza al pasar el
   cursor sobre cada icono. */
.side-rail {
    display: none;
}
@media (min-width: 1101px) {
    .home-page .side-rail {
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        z-index: 1100;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        width: 72px;
        padding: 14px 0 12px;
        background: var(--white);
        border-right: 1px solid var(--border);
    }
}
.side-rail-brand {
    display: grid;
    place-items: center;
    width: 48px;
    height: 48px;
    margin-bottom: 8px;
    border-radius: var(--radius-sm);
    color: var(--primary-strong);
    text-decoration: none;
    transition: background var(--transition);
}
.side-rail-brand:hover,
.side-rail-brand:focus-visible {
    background: var(--tint);
}
.side-rail-brand:focus-visible {
    outline: 3px solid color-mix(in srgb, var(--primary) 45%, transparent);
    outline-offset: 2px;
}
.side-rail-brand-icon {
    width: 26px;
    height: 26px;
}
.side-rail-group {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
}
.side-rail-bottom {
    margin-top: auto;
}
.side-rail-item {
    position: relative;
    display: grid;
    place-items: center;
    width: 48px;
    height: 48px;
    border: 0;
    border-radius: var(--radius-sm);
    background: transparent;
    color: var(--text-soft);
    cursor: pointer;
    text-decoration: none;
    transition: background var(--transition), color var(--transition);
}
.side-rail-item:hover,
.side-rail-item:focus-visible {
    background: var(--tint);
    color: var(--primary-strong);
}
.side-rail-item:focus-visible {
    outline: 3px solid color-mix(in srgb, var(--primary) 45%, transparent);
    outline-offset: 2px;
}
.side-rail-item.active {
    background: var(--tint);
    color: var(--primary-strong);
    box-shadow: inset 3px 0 var(--primary-strong);
}
.side-rail-icon {
    width: 22px;
    height: 22px;
}
.side-rail-signout {
    color: var(--danger);
}
.side-rail-signout:hover,
.side-rail-signout:focus-visible {
    background: var(--danger-bg);
    color: var(--danger);
}
.side-rail-badge {
    position: absolute;
    top: 2px;
    right: 2px;
    display: grid;
    min-width: 18px;
    height: 18px;
    place-items: center;
    padding: 0 4px;
    border: 2px solid var(--white);
    border-radius: 99px;
    background: var(--danger);
    color: var(--white);
    font-size: .75rem;
    font-weight: 800;
    line-height: 1;
}
.side-rail-label {
    position: absolute;
    left: calc(100% + 12px);
    top: 50%;
    z-index: 1;
    padding: 7px 11px;
    border-radius: var(--radius-xs);
    background: var(--primary-dark);
    color: var(--white);
    font-size: .8rem;
    font-weight: 700;
    white-space: nowrap;
    opacity: 0;
    visibility: hidden;
    transform: translate(-8px, -50%);
    transition: opacity .2s ease, transform .2s ease, visibility .2s ease;
    pointer-events: none;
}
.side-rail-item:hover .side-rail-label,
.side-rail-item:focus-visible .side-rail-label {
    opacity: 1;
    visibility: visible;
    transform: translate(0, -50%);
}
@media (prefers-reduced-motion: reduce) {
    .side-rail-label {
        transition: none;
    }
}
.favorites-modal-content { width:min(560px,100%); padding:32px; }
.favorites-modal-eyebrow { display:block; margin-bottom:5px; color:var(--primary-strong); font-size:.75rem; font-weight:800; letter-spacing:.09em; }
.favorites-modal h2 { display:flex; align-items:center; gap:10px; margin-bottom:5px; font-size:1.75rem; }
.favorites-modal-count { display:grid; place-items:center; min-width:27px; height:27px; padding:0 7px; border-radius: var(--radius-md); background:var(--primary-soft); color:var(--primary-strong); font-size:.76rem; }
 .favorites-modal-count { background:var(--button-primary-bg); color:var(--white); }
.favorites-modal .favorites-modal-intro { margin-bottom:18px; font-size:.82rem; }
.favorites-modal-list { max-height:min(440px,55vh); overflow-y:auto; gap:10px; padding:1px 3px 1px 1px; }
.favorites-modal-item { display:grid; grid-template-columns:62px minmax(0,1fr) auto; gap:13px; min-height:82px; padding:10px; border-radius: var(--radius-sm); background:var(--white); }
.favorites-pet-thumbnail { width:62px; height:62px; border-radius: var(--radius-xs); object-fit:cover; background:var(--background); }
.favorites-pet-thumbnail-empty { display:grid; place-items:center; color:var(--primary-strong); }
.favorites-pet-copy { display:flex; min-width:0; flex-direction:column; justify-content:center; gap:3px; }
.favorites-pet-copy strong { font-size:.98rem; line-height:1.3; }
.favorites-pet-copy span { overflow:hidden; color:var(--text-soft); font-size:.75rem; text-overflow:ellipsis; white-space:nowrap; }
.favorites-pet-actions { display:flex; align-items:center; justify-content:flex-end; gap:8px; }
.favorites-profile-link { display:inline-flex; min-height:38px; align-items:center; justify-content:center; padding:0 12px; border-radius: var(--radius-xs); background:var(--primary-strong); color:var(--white); font-size:.75rem; font-weight:800; text-decoration:none; white-space:nowrap; }
.favorites-profile-link:hover { background:var(--primary-strong-hover); }
.favorites-remove-button { display:inline-flex; min-height:38px; align-items:center; justify-content:center; gap:5px; padding:0 10px; border:1px solid var(--border); border-radius: var(--radius-xs); background:var(--white); color:var(--text-soft); cursor:pointer; font:700 .75rem "Montserrat",Arial,sans-serif; white-space:nowrap; }
.favorites-remove-button:hover { border-color:var(--danger); color:var(--danger); }
.favorites-remove-button :deep(svg) { width:15px; height:15px; }
.favorites-empty-copy { margin:12px 0 0!important; font-size:.84rem!important; line-height:1.6; }
.favorites-login-link { display:inline-flex; margin-top:12px; }
.favorites-empty-state { display:grid; justify-items:center; gap:14px; padding:22px 0 3px; }
.favorites-empty-icon { display:grid; width:58px; height:58px; place-items:center; border-radius:50%; border:1px solid var(--primary); background:var(--primary-soft); color:var(--primary-strong); }
.favorites-explore-link { display:inline-flex; align-items:center; justify-content:center; gap:9px; text-decoration:none; }
.notification-list { display:grid; max-height:min(480px,58vh); overflow:auto; gap:7px; margin-top:14px; }.notification-item { display:grid; grid-template-columns:34px minmax(0,1fr); gap:10px; width:100%; padding:12px; border:1px solid transparent; border-radius: var(--radius-sm); background:var(--background); color:var(--text); text-align:left; cursor:pointer; }.notification-item:hover,.notification-item.is-unread { border-color:var(--border); background:var(--background); }.notification-icon { display:grid; width:32px; height:32px; place-items:center; border-radius: var(--radius-sm); border:1px solid var(--primary); background:var(--primary-soft); color:var(--primary-strong); }.notification-item > span:last-child { display:grid; gap:3px; min-width:0; }.notification-item strong { font-size:.76rem; }.notification-item small { color:var(--text-soft); font-size:.75rem; line-height:1.45; }.notification-item time { color:var(--text-soft); font-size:.75rem; }
@media(max-width:520px) {
  .account-display-name { max-width:105px; }
  .side-panel-content { width:min(420px,96vw); padding:26px 18px 20px; border-radius:16px 0 0 16px; }
  .favorites-modal-item { grid-template-columns:54px minmax(0,1fr); gap:10px; }
  .favorites-pet-thumbnail { width:54px; height:54px; }
  .favorites-pet-actions { grid-column:1/-1; justify-content:stretch; }
  .favorites-profile-link,.favorites-remove-button { flex:1; }
  .favorites-modal-list { max-height:50dvh; }
}
@media(max-width:380px) { .header-right { gap:5px; } .account-display-name { max-width:55px; } .account-trigger,.notification-trigger { width:44px; height:44px; border-radius: var(--radius-sm); } }
</style>
