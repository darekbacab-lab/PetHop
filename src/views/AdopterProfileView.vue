<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import { api } from '../services/api.js';
import { currentUser, loadCurrentUser, logout } from '../services/auth.js';
import { listFavorites } from '../services/petApi.js';

const router = useRouter();
const route = useRoute();
const user = ref(null);
const applications = ref([]);
const adoptions = ref([]);
const favorites = ref([]);
const totals = ref({ applications: 0, adoptions: 0 });
const loading = ref(true);
const pageError = ref('');
const activityErrors = ref({});
const editing = ref(false);
const saving = ref(false);
const form = ref({ fullName: '', phone: '' });
const feedback = ref('');
const passwordForm = ref({ currentPassword: '', newPassword: '', confirm: '' });
const passwordFeedback = ref('');
const passwordSaving = ref(false);
const selectedActivity = ref(null);
const activityLoading = ref(false);
const activityError = ref('');
const initials = computed(() => String(user.value?.fullName || '').trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toLocaleUpperCase('es-MX') || 'P');
const pendingCount = computed(() => applications.value.filter(item => ['SUBMITTED', 'UNDER_REVIEW'].includes(item.status)).length);
const isAdmin = computed(() => user.value?.role === 'ADMIN');

async function loadProfile() {
  loading.value = true;
  pageError.value = '';
  try {
    const authenticated = await loadCurrentUser();
    if (!authenticated) { await router.replace({ path: '/acceso.html', query: { redirect: '/perfil.html' } }); return; }
    if (authenticated.role === 'SHELTER') { await router.replace('/proveedor.html?section=perfil'); return; }
    if (!['ADOPTER', 'ADMIN'].includes(authenticated.role)) { pageError.value = 'Este perfil está disponible para cuentas adoptantes y administradores.'; return; }
    user.value = await api('/users/me');
    currentUser.value = user.value;
    form.value = { fullName: user.value.fullName || '', phone: user.value.phone || '' };
    if (authenticated.role === 'ADMIN') return;
    const results = await Promise.allSettled([
      api('/adoption-applications/mine?page=0&size=50'),
      api('/adoptions/mine?page=0&size=50'),
      listFavorites()
    ]);
    if (results[0].status === 'fulfilled') {
      applications.value = results[0].value.content || [];
      totals.value.applications = results[0].value.totalElements ?? applications.value.length;
    } else activityErrors.value.applications = results[0].reason?.message || 'No se pudieron consultar tus solicitudes.';
    if (results[1].status === 'fulfilled') {
      adoptions.value = results[1].value.content || [];
      totals.value.adoptions = results[1].value.totalElements ?? adoptions.value.length;
    } else activityErrors.value.adoptions = results[1].reason?.message || 'No se pudieron consultar tus adopciones.';
    if (results[2].status === 'fulfilled') favorites.value = results[2].value;
    else activityErrors.value.favorites = results[2].reason?.message || 'No se pudieron consultar tus favoritos.';
  } catch (error) {
    pageError.value = error.message || 'No se pudo cargar tu perfil.';
  } finally { loading.value = false; }
}

function startEditing() { form.value = { fullName: user.value?.fullName || '', phone: user.value?.phone || '' }; feedback.value = ''; editing.value = true; }
function cancelEditing() { editing.value = false; feedback.value = ''; }
async function signOut() {
  try { await logout(); }
  finally { await router.push('/'); }
}
async function saveProfile() {
  saving.value = true;
  feedback.value = '';
  try {
    user.value = await api('/users/me', { method: 'PATCH', body: { fullName: form.value.fullName.trim(), phone: form.value.phone.trim() || null } });
    currentUser.value = user.value;
    editing.value = false;
    feedback.value = 'Tu perfil se guardó correctamente.';
  } catch (error) { feedback.value = error.message || 'No se pudo guardar el perfil. Tus cambios siguen disponibles para reintentar.'; }
  finally { saving.value = false; }
}
async function changePassword() {
  passwordFeedback.value = '';
  if (passwordForm.value.newPassword !== passwordForm.value.confirm) { passwordFeedback.value = 'Las contraseñas nuevas no coinciden.'; return; }
  passwordSaving.value = true;
  try {
    await api('/users/me/password', { method: 'PATCH', body: { currentPassword: passwordForm.value.currentPassword, newPassword: passwordForm.value.newPassword } });
    passwordForm.value = { currentPassword: '', newPassword: '', confirm: '' };
    passwordFeedback.value = 'Tu contraseña se actualizó correctamente.';
  } catch (error) { passwordFeedback.value = error.message || 'No se pudo actualizar la contraseña.'; }
  finally { passwordSaving.value = false; }
}

async function openActivity(type, item) {
  selectedActivity.value = { type, data: item };
  activityLoading.value = true;
  activityError.value = '';
  try {
    const endpoint = type === 'application' ? `/adoption-applications/${encodeURIComponent(item.id)}` : `/adoptions/${encodeURIComponent(item.id)}`;
    selectedActivity.value = { type, data: await api(endpoint) };
  } catch (error) { activityError.value = error.message || 'No se pudo consultar el detalle.'; }
  finally { activityLoading.value = false; }
}

function statusLabel(status) {
  return ({ SUBMITTED: 'Enviada', UNDER_REVIEW: 'En revisión', APPROVED: 'Aceptada', REJECTED: 'Rechazada', WITHDRAWN: 'Retirada' })[status] || status || 'Sin estado';
}

onMounted(async () => {
  await loadProfile();
  if (route.hash) {
    await nextTick();
    document.querySelector(route.hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
});
</script>

<template>
  <div class="adopter-profile-page">
    <SiteHeader />
    <main class="adopter-profile-main">
      <div v-if="loading" class="adopter-profile-state" role="status">Cargando tu espacio…</div>
      <div v-else-if="pageError" class="adopter-profile-state" role="alert"><Icon name="circle-alert" /><p>{{ pageError }}</p><RouterLink class="button button-primary" to="/acceso.html">Ingresar</RouterLink></div>
      <template v-else>
        <section class="adopter-identity" aria-labelledby="adopter-name">
          <div class="adopter-avatar" aria-hidden="true">{{ initials }}</div>
          <div class="adopter-identity-copy"><span class="eyebrow">MI ESPACIO EN PETHOP</span><h1 id="adopter-name">{{ user.fullName }}</h1><div class="adopter-identity-meta"><span>{{ isAdmin ? 'Administrador' : 'Adoptante' }}</span><span v-if="user.createdAt">Desde {{ new Date(user.createdAt).toLocaleDateString('es-MX', { month: 'long', year: 'numeric' }) }}</span></div></div>
          <div class="adopter-identity-actions"><button v-if="!editing" type="button" class="button button-outline adopter-edit-button" @click="startEditing"><Icon name="pencil" />Editar perfil</button><button type="button" class="button button-logout" @click="signOut"><Icon name="log-out" />Cerrar sesión</button></div>
        </section>

        <section class="adopter-personal panel" aria-labelledby="personal-title">
          <div class="adopter-section-heading"><div><span class="eyebrow">TU CUENTA</span><h2 id="personal-title">Información personal</h2></div></div>
          <form v-if="editing" class="adopter-personal-form" @submit.prevent="saveProfile">
            <label>Nombre completo<input v-model="form.fullName" required minlength="2" maxlength="160" autocomplete="name"></label>
            <label>Teléfono<input v-model="form.phone" type="tel" maxlength="32" autocomplete="tel"></label>
            <label>Correo electrónico<input :value="user.email" type="email" readonly disabled><small>El correo es un dato de cuenta y no se puede cambiar desde el perfil.</small></label>
            <div class="adopter-form-actions"><button class="button button-outline" type="button" @click="cancelEditing">Cancelar</button><button class="button button-primary" type="submit" :disabled="saving">{{ saving ? 'Guardando…' : 'Guardar cambios' }}</button></div>
          </form>
          <dl v-else class="adopter-personal-grid"><div><dt>Nombre completo</dt><dd>{{ user.fullName }}</dd></div><div><dt>Correo electrónico</dt><dd>{{ user.email }}</dd></div><div><dt>Teléfono</dt><dd>{{ user.phone || 'No proporcionado' }}</dd></div><div><dt>Tipo de cuenta</dt><dd>{{ isAdmin ? 'Administrador' : 'Adoptante' }}</dd></div></dl>
          <p v-if="feedback" class="adopter-feedback" role="status">{{ feedback }}</p>
          <form class="adopter-password-form" @submit.prevent="changePassword">
            <h3>Cambiar contraseña</h3>
            <label>Contraseña actual<input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" required></label>
            <label>Nueva contraseña<input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required></label>
            <label>Confirmar contraseña<input v-model="passwordForm.confirm" type="password" autocomplete="new-password" minlength="12" maxlength="72" required></label>
            <button class="button button-outline" type="submit" :disabled="passwordSaving">{{ passwordSaving ? 'Actualizando…' : 'Actualizar contraseña' }}</button>
            <p v-if="passwordFeedback" class="adopter-feedback" role="status">{{ passwordFeedback }}</p>
          </form>
        </section>

        <section v-if="!isAdmin" class="adopter-activity" aria-labelledby="activity-title">
          <div class="adopter-section-heading adopter-activity-heading"><div><span class="eyebrow">TU CAMINO DE ADOPCIÓN</span><h2 id="activity-title">Actividad</h2></div><span class="adopter-activity-summary">{{ totals.applications }} solicitudes · {{ totals.adoptions }} adopciones</span></div>
          <div class="adopter-activity-grid">
            <section id="solicitudes" class="panel adopter-activity-panel"><div class="adopter-panel-heading"><div><Icon name="clipboard-list" /><h3>Mis solicitudes</h3></div><span class="adopter-count">{{ totals.applications }}</span></div><p v-if="activityErrors.applications" class="adopter-inline-error" role="alert">{{ activityErrors.applications }}</p><p v-else-if="!applications.length" class="adopter-empty-copy">Aún no has enviado solicitudes.</p><div v-else class="adopter-activity-list"><button v-for="item in applications" :key="item.id" type="button" class="adopter-activity-row" @click="openActivity('application', item)"><span class="adopter-activity-pet"><strong>{{ item.petName }}</strong><small>{{ item.submittedAt ? new Date(item.submittedAt).toLocaleDateString('es-MX') : 'Fecha no disponible' }}</small></span><span class="adopter-status-pill">{{ statusLabel(item.status) }}</span><Icon name="chevron-right" /></button><p v-if="pendingCount" class="adopter-activity-footnote">{{ pendingCount }} pendientes o en revisión</p></div></section>
            <section id="adopciones" class="panel adopter-activity-panel"><div class="adopter-panel-heading"><div><Icon name="heart-handshake" /><h3>Mis adopciones</h3></div><span class="adopter-count">{{ totals.adoptions }}</span></div><p v-if="activityErrors.adoptions" class="adopter-inline-error" role="alert">{{ activityErrors.adoptions }}</p><p v-else-if="!adoptions.length" class="adopter-empty-copy">Tus adopciones formalizadas aparecerán aquí.</p><div v-else class="adopter-activity-list"><button v-for="item in adoptions" :key="item.id" type="button" class="adopter-activity-row" @click="openActivity('adoption', item)"><span class="adopter-activity-pet"><strong>{{ item.petName }}</strong><small>{{ item.adoptionDate ? new Date(`${item.adoptionDate}T12:00:00`).toLocaleDateString('es-MX') : 'Fecha no disponible' }}</small></span><span class="adopter-status-pill is-complete">Formalizada</span><Icon name="chevron-right" /></button></div></section>
            <section id="favoritos" class="panel adopter-activity-panel adopter-favorites-panel"><div class="adopter-panel-heading"><div><Icon name="heart" /><h3>Mis favoritos</h3></div><span class="adopter-count">{{ favorites.length }}</span></div><p v-if="activityErrors.favorites" class="adopter-inline-error" role="alert">{{ activityErrors.favorites }}</p><p v-else-if="!favorites.length" class="adopter-empty-copy">Guarda las mascotas que quieras volver a consultar.</p><div v-else class="adopter-favorite-list"><RouterLink v-for="pet in favorites" :key="pet.id" class="adopter-favorite-row" :to="{ path: '/mascota.html', query: { pet: pet.id } }"><img v-if="pet.photos?.[0]" :src="pet.photos[0]" :alt="`Foto de ${pet.name}`" loading="lazy"><span v-else class="adopter-favorite-placeholder"><Icon name="paw-print" /></span><span><strong>{{ pet.name }}</strong><small>{{ pet.species }}<template v-if="pet.location"> · {{ pet.location }}</template></small></span><Icon name="arrow-up-right" /></RouterLink></div><RouterLink class="adopter-explore-link" to="/explorar.html">Explorar mascotas<Icon name="arrow-right" /></RouterLink></section>
          </div>
        </section>
      </template>
    </main>
    <div v-if="selectedActivity" class="adopter-detail-backdrop" @click.self="selectedActivity=null"><section class="panel adopter-detail-modal" role="dialog" aria-modal="true" :aria-labelledby="'activity-detail-title'" @keydown.esc="selectedActivity=null"><button class="adopter-detail-close" type="button" aria-label="Cerrar detalle" @click="selectedActivity=null"><Icon name="x" /></button><span class="eyebrow">{{ selectedActivity.type === 'application' ? 'SOLICITUD DE ADOPCIÓN' : 'ADOPCIÓN FORMALIZADA' }}</span><h2 id="activity-detail-title">{{ selectedActivity.data.petName }}</h2><p v-if="activityLoading" role="status">Cargando detalle…</p><p v-else-if="activityError" role="alert">{{ activityError }}</p><dl v-else class="adopter-detail-grid"><template v-if="selectedActivity.type === 'application'"><div><dt>Estado</dt><dd>{{ statusLabel(selectedActivity.data.status) }}</dd></div><div><dt>Enviada</dt><dd>{{ selectedActivity.data.submittedAt ? new Date(selectedActivity.data.submittedAt).toLocaleString('es-MX') : 'No disponible' }}</dd></div><div><dt>Motivo</dt><dd>{{ selectedActivity.data.motivation || 'No proporcionado' }}</dd></div><div><dt>Tipo de hogar</dt><dd>{{ selectedActivity.data.housingType || 'No proporcionado' }}</dd></div><div><dt>Experiencia</dt><dd>{{ selectedActivity.data.experience || 'No proporcionada' }}</dd></div><div><dt>Disponibilidad</dt><dd>{{ selectedActivity.data.availability || 'No proporcionada' }}</dd></div><div><dt>Información adicional</dt><dd>{{ selectedActivity.data.additionalInformation || 'No proporcionada' }}</dd></div></template><template v-else><div><dt>Fecha de adopción</dt><dd>{{ selectedActivity.data.adoptionDate || 'No disponible' }}</dd></div><div><dt>Notas del acuerdo</dt><dd>{{ selectedActivity.data.agreementNotes || 'Sin notas registradas' }}</dd></div></template></dl><RouterLink v-if="selectedActivity.data.petId" class="button button-primary" :to="{ path: '/mascota.html', query: { pet: selectedActivity.data.petId } }" @click="selectedActivity=null">Ver perfil de la mascota</RouterLink></section></div>
    <SiteFooter />
  </div>
</template>

<style scoped>
.adopter-profile-page { min-height:100vh; background:var(--background); }
.adopter-profile-main { width:min(1120px,calc(100% - 40px)); margin:0 auto; padding:42px 0 70px; }
.adopter-profile-state { display:grid; min-height:280px; justify-items:center; align-content:center; gap:12px; color:var(--text-soft); text-align:center; }
.adopter-profile-state :deep(svg) { width:30px; height:30px; color:var(--primary-strong); }
.adopter-profile-page .button { display:inline-flex; min-height:42px; align-items:center; justify-content:center; gap:8px; padding:9px 14px; border:1px solid transparent; border-radius:11px; cursor:pointer; font:700 .78rem "Montserrat",sans-serif; text-decoration:none; transition:background .16s ease,border-color .16s ease,color .16s ease,transform .16s ease; }
.adopter-profile-page .button:focus-visible { outline:3px solid color-mix(in srgb,var(--primary) 42%,transparent); outline-offset:2px; }
.adopter-profile-page .button:active { transform:translateY(1px); }
.adopter-profile-page .button:disabled { cursor:wait; opacity:.65; }
.adopter-profile-page .button-primary { border-color:var(--primary-strong); background:var(--primary-strong); color:#fff; }
.adopter-profile-page .button-primary:hover { border-color:var(--primary-strong-hover); background:var(--primary-strong-hover); }
.adopter-profile-page .button-outline { border-color:var(--border-dark); background:var(--white); color:var(--primary-dark); }
.adopter-profile-page .button-outline:hover { border-color:var(--primary); background:var(--background); color:var(--primary-strong); }
.adopter-profile-page .button-logout { border-color:#f0d5d5; background:#fffafa; color:#a63d3d; }
.adopter-profile-page .button-logout:hover { border-color:#d99b9b; background:#fff1f1; }
.adopter-identity { display:flex; align-items:center; gap:20px; margin-bottom:24px; padding:25px 28px; border:1px solid var(--border); border-radius:20px; background:var(--white); }
.adopter-avatar { display:grid; width:76px; height:76px; flex:0 0 76px; place-items:center; border:4px solid #dff2f1; border-radius:50%; background:var(--primary-strong); color:#fff; font:800 1.45rem "Poppins",sans-serif; }
.adopter-identity-copy { min-width:0; flex:1; }
.adopter-identity-copy .eyebrow,.adopter-section-heading .eyebrow { margin:0 0 5px; color:var(--primary-strong); font-size:.65rem; }
.adopter-identity h1 { margin:0; color:var(--text); font:800 clamp(1.7rem,3vw,2.35rem)/1.15 "Poppins",sans-serif; }
.adopter-identity-meta { display:flex; flex-wrap:wrap; gap:8px; margin-top:9px; color:var(--text-soft); font-size:.78rem; }
.adopter-identity-meta span:first-child { padding:4px 9px; border-radius:999px; background:#e8f6f5; color:var(--primary-strong); font-weight:800; }
.adopter-profile-page :deep(.panel) { border:1px solid var(--border); border-radius:17px; background:var(--white); }
.adopter-personal { padding:21px 23px; }
.adopter-section-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px; }
.adopter-section-heading h2 { margin:0; color:var(--text); font:750 1.25rem "Poppins",sans-serif; }
.adopter-identity-actions { display:flex; flex:0 0 auto; flex-wrap:wrap; justify-content:flex-end; gap:9px; }
.adopter-personal-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px 22px; margin:0; }
.adopter-personal-grid div,.adopter-detail-grid div { min-width:0; }
.adopter-personal-grid dt,.adopter-detail-grid dt { margin-bottom:4px; color:var(--text-soft); font-size:.7rem; font-weight:700; }
.adopter-personal-grid dd,.adopter-detail-grid dd { overflow-wrap:anywhere; margin:0; color:var(--text); font-size:.87rem; font-weight:600; }
.adopter-personal-form { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px 18px; }
.adopter-personal-form label { display:grid; gap:6px; color:var(--text); font-size:.76rem; font-weight:750; }
.adopter-personal-form input { width:100%; min-height:44px; padding:10px 12px; border:1px solid var(--border); border-radius:10px; background:var(--background); color:var(--text); font:500 .85rem "Montserrat",sans-serif; }
.adopter-personal-form input:disabled { opacity:.7; }
.adopter-password-form { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px 16px; margin-top:24px; padding-top:20px; border-top:1px solid var(--border); }
.adopter-password-form h3 { grid-column:1/-1; margin:0; color:var(--text); font:750 1rem "Poppins",sans-serif; }
.adopter-password-form label { display:grid; gap:6px; color:var(--text); font-size:.76rem; font-weight:750; }
.adopter-password-form input { width:100%; min-height:44px; padding:10px 12px; border:1px solid var(--border); border-radius:10px; background:var(--background); color:var(--text); font:500 .85rem "Montserrat",sans-serif; }
.adopter-password-form button { align-self:end; justify-self:start; }
.adopter-password-form .adopter-feedback { grid-column:1/-1; }
.adopter-personal-form small { color:var(--text-soft); font-size:.67rem; font-weight:500; }
.adopter-form-actions { display:flex; grid-column:1/-1; justify-content:flex-end; gap:9px; }
.adopter-feedback { margin:12px 0 0; color:var(--primary-strong); font-size:.78rem; }
.adopter-activity { margin-top:29px; }
.adopter-activity-heading { margin-bottom:14px; }
.adopter-activity-summary { color:var(--text-soft); font-size:.76rem; }
.adopter-activity-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; align-items:start; }
.adopter-activity-panel { min-width:0; padding:18px 19px; scroll-margin-top:100px; }
.adopter-panel-heading { display:flex; align-items:center; justify-content:space-between; gap:10px; padding-bottom:13px; border-bottom:1px solid var(--border); }
.adopter-panel-heading > div { display:flex; align-items:center; gap:9px; }
.adopter-panel-heading > div > :deep(svg) { width:19px; height:19px; color:var(--primary-strong); }
.adopter-panel-heading h3 { margin:0; color:var(--text); font:700 .95rem "Poppins",sans-serif; }
.adopter-count { display:grid; min-width:27px; height:27px; place-items:center; padding:0 7px; border-radius:99px; background:#e8f6f5; color:var(--primary-strong); font-size:.72rem; font-weight:800; }
.adopter-empty-copy,.adopter-inline-error { margin:16px 0 3px; color:var(--text-soft); font-size:.8rem; line-height:1.55; }
.adopter-inline-error { color:var(--danger); }
.adopter-activity-list,.adopter-favorite-list { display:grid; }
.adopter-activity-row { display:flex; width:100%; min-height:58px; align-items:center; gap:10px; padding:9px 1px; border:0; border-bottom:1px solid var(--border); background:transparent; color:var(--text); cursor:pointer; text-align:left; }
.adopter-activity-row:hover { background:var(--background); }
.adopter-activity-pet { display:grid; min-width:0; flex:1; gap:3px; }
.adopter-activity-pet strong { overflow:hidden; font-size:.8rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-activity-pet small { color:var(--text-soft); font-size:.68rem; }
.adopter-activity-row > :deep(svg) { width:16px; color:var(--text-soft); }
.adopter-status-pill { flex:0 0 auto; padding:5px 8px; border-radius:999px; background:#fff3dc; color:#765516; font-size:.62rem; font-weight:800; }
.adopter-status-pill.is-complete { background:#e6f5ec; color:#287451; }
.adopter-activity-footnote { margin:9px 0 0; color:var(--text-soft); font-size:.68rem; }
.adopter-favorite-row { display:flex; min-height:62px; align-items:center; gap:10px; padding:8px 1px; border-bottom:1px solid var(--border); color:var(--text); text-decoration:none; }
.adopter-favorite-row:hover { color:var(--primary-strong); }
.adopter-favorite-row img,.adopter-favorite-placeholder { display:grid; width:46px; height:46px; flex:0 0 46px; place-items:center; border-radius:10px; background:var(--background); object-fit:cover; color:var(--primary-strong); }
.adopter-favorite-row > span:nth-child(2) { display:grid; min-width:0; flex:1; gap:3px; }
.adopter-favorite-row strong { overflow:hidden; font-size:.78rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-favorite-row small { overflow:hidden; color:var(--text-soft); font-size:.67rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-favorite-row > :deep(svg) { width:16px; }
.adopter-explore-link { display:inline-flex; align-items:center; gap:7px; margin-top:12px; color:var(--primary-strong); font-size:.75rem; font-weight:800; text-decoration:none; }
.adopter-explore-link :deep(svg) { width:16px; }
.adopter-detail-backdrop { position:fixed; z-index:1500; inset:0; display:grid; place-items:center; overflow:auto; padding:20px; background:rgba(14,39,45,.58); }
.adopter-detail-modal { position:relative; width:min(620px,100%); max-height:calc(100dvh - 40px); overflow:auto; padding:27px; }
.adopter-detail-modal h2 { margin:0 45px 18px 0; color:var(--text); font:750 1.45rem "Poppins",sans-serif; }
.adopter-detail-modal > .eyebrow { margin:0 42px 6px 0; color:var(--primary-strong); font-size:.65rem; }
.adopter-detail-close { position:absolute; top:17px; right:17px; display:grid; width:36px; height:36px; place-items:center; border:1px solid var(--border); border-radius:50%; background:var(--white); color:var(--text); cursor:pointer; }
.adopter-detail-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:15px; margin:0 0 22px; }
.adopter-detail-modal > .button { text-decoration:none; }
@media(max-width:760px) { .adopter-profile-main { width:min(620px,calc(100% - 28px)); padding:24px 0 50px; } .adopter-identity { flex-wrap:wrap; gap:14px; padding:19px; } .adopter-avatar { width:62px; height:62px; flex-basis:62px; } .adopter-personal-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } .adopter-activity-grid { grid-template-columns:1fr; } }
@media(max-width:480px) { .adopter-profile-main { width:calc(100% - 24px); } .adopter-identity { padding:16px; } .adopter-identity-copy { flex-basis:calc(100% - 80px); } .adopter-identity-copy h1 { font-size:1.55rem; } .adopter-identity-actions { width:100%; } .adopter-identity-actions .button { flex:1; } .adopter-personal { padding:17px 15px; } .adopter-personal-grid,.adopter-personal-form,.adopter-detail-grid,.adopter-password-form { grid-template-columns:1fr; } .adopter-form-actions { grid-column:auto; } .adopter-activity-summary { font-size:.68rem; } .adopter-activity-panel { padding:16px 14px; } .adopter-status-pill { max-width:100px; text-align:center; } .adopter-detail-modal { padding:22px 17px; } }
</style>
