<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import Icon from '../components/Icon.vue';
import { API_BASE, api } from '../services/api.js';
import { currentUser, loadCurrentUser } from '../services/auth.js';

const router = useRouter();
const pending = ref([]); const selected = ref(null); const history = ref([]);
const loading = ref(true); const saving = ref(false); const error = ref(''); const notice = ref(''); const reason = ref('');
const shelters = ref([]); const users = ref([]); const reports = ref([]); const audit = ref([]); const affiliates = ref([]); const activeSection = ref('pets'); const userSearch = ref('');
const affiliateDraft = ref({ name: '', category: 'VETERINARY', description: '', location: '', websiteUrl: '', discountCode: '', discountText: '', active: true }); const affiliateSaving = ref(false);
const previewImage = ref(null); const checklist = ref({});
const sidebarOpen = ref(false);
const adminSections = [
  { id: 'pets', label: 'Mascotas', icon: 'paw-print' },
  { id: 'shelters', label: 'Refugios', icon: 'building-2' },
  { id: 'users', label: 'Usuarios', icon: 'user-round' },
  { id: 'reports', label: 'Reportes', icon: 'circle-help' },
  { id: 'audit', label: 'Actividad', icon: 'clipboard-list' },
  { id: 'affiliates', label: 'Aliados', icon: 'building-2' }
];
const reviewChecks = [
  ['coherent', 'La información de la mascota es coherente.'],
  ['images', 'Las fotografías parecen relevantes y genuinas.'],
  ['description', 'La descripción es suficientemente completa.'],
  ['duplicate', 'No hay una publicación duplicada evidente.'],
  ['publisher', 'La información del publicador es consistente.'],
  ['suspicious', 'No hay información contradictoria o evidentemente sospechosa.']
];
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
const reviewSections = computed(() => {
  const pet = selected.value || {};
  const booleanValue = value => value === null || value === undefined ? 'No indicado' : value ? 'Sí' : 'No';
  const age = pet.ageValue === null || pet.ageValue === undefined ? null : `${pet.ageValue}${pet.ageUnit ? ` ${pet.ageUnit}` : ''}`;
  const traits = Array.isArray(pet.personalityTraits) && pet.personalityTraits.length ? pet.personalityTraits.join(', ') : null;
  const fields = (items, includeMissing = false) => items.filter(([, value]) => includeMissing || (value !== null && value !== undefined && String(value).trim() !== ''));
  return [
    { title: 'Identidad y características', fields: fields([
      ['Nombre', pet.name], ['Especie', pet.species], ['Raza', pet.breed], ['Etapa de vida', pet.lifeStage], ['Edad', age],
      ['Sexo', pet.sex], ['Tamaño', pet.size], ['Peso', pet.weight === null || pet.weight === undefined ? null : `${pet.weight} kg`], ['Color', pet.color], ['Ubicación', pet.location]
    ]) },
    { title: 'Salud y cuidados', fields: fields([
      ['Condición médica', pet.healthCondition], ['Resumen de salud', pet.healthSummary], ['Vacunación', booleanValue(pet.vaccinated)],
      ['Esterilización', booleanValue(pet.sterilized)], ['Desparasitación', booleanValue(pet.dewormed)]
    ]) },
    { title: 'Adopción y moderación', fields: fields([
      ['Estado de adopción', pet.adoptionStatus], ['Estado de moderación', pet.moderationStatus],
      ['Publicada en', pet.publishedAt ? new Date(pet.publishedAt).toLocaleString('es-MX') : null]
    ]) },
    { title: 'Historia y personalidad', fields: fields([
      ['Descripción', pet.description], ['Personalidad', pet.personality], ['Rasgos', traits], ['Historia', pet.story], ['Antecedentes', pet.background]
    ]) },
    { title: 'Requisitos de adopción', fields: fields([
      ['Tipo de hogar', pet.homeType], ['Condiciones necesarias', pet.conditions], ['Experiencia recomendada', pet.experience], ['Otros requisitos', pet.requirements]
    ]) }
  ].filter(section => section.fields.length);
});
function imageUrl(image) {
  if (!image?.url) return '';
  return /^https?:\/\//i.test(image.url) ? image.url : `${API_BASE.replace(/\/api\/v1$/, '')}${image.url}`;
}
function resetChecklist() { checklist.value = Object.fromEntries(reviewChecks.map(([key]) => [key, false])); }
async function loadQueue() {
  loading.value = true; error.value = '';
  try { const page = await api('/admin/pets/pending?size=50'); pending.value = page.content || []; }
  catch (e) { error.value = e.message || 'No se pudo cargar la cola de moderación.'; }
  finally { loading.value = false; }
}
async function openPet(pet) {
  error.value = ''; notice.value = '';
  try { selected.value = await api(`/admin/pets/${pet.id}`); history.value = await api(`/admin/pets/${pet.id}/moderation-history`); reason.value = ''; previewImage.value = null; resetChecklist(); }
  catch (e) { error.value = e.message || 'No se pudo abrir la publicación.'; }
}
async function decide(action) {
  if (!selected.value || saving.value) return;
  if ((action === 'REJECT' || action === 'REQUEST_CORRECTIONS') && !reason.value.trim()) { error.value = 'Indica el motivo de la decisión.'; return; }
  saving.value = true; error.value = '';
  try { await api(`/admin/pets/${selected.value.id}/moderation`, { method: 'PATCH', body: { action, reason: reason.value.trim() || null } }); notice.value = 'Decisión guardada y notificada al responsable.'; selected.value = null; await loadQueue(); }
  catch (e) { error.value = e.message || 'No se pudo guardar la decisión.'; }
  finally { saving.value = false; }
}
async function loadAdminData() {
  const query = userSearch.value.trim() ? `&search=${encodeURIComponent(userSearch.value.trim())}` : '';
  const results = await Promise.allSettled([
    api('/admin/shelters/pending'), api(`/admin/users?size=50${query}`), api('/admin/reports?size=50'), api('/admin/audit?size=50')
  ]);
  const [s, u, r, a] = results;
  if (s.status === 'fulfilled') shelters.value = s.value || [];
  if (u.status === 'fulfilled') users.value = u.value?.content || [];
  if (r.status === 'fulfilled') reports.value = r.value?.content || [];
  if (a.status === 'fulfilled') audit.value = a.value?.content || [];
  const failedSections = results.flatMap((result, index) => result.status === 'rejected' ? [adminSections[index + 1].label.toLowerCase()] : []);
  if (failedSections.length) error.value = `No se pudo cargar: ${failedSections.join(', ')}.`;
}
async function shelterDecision(item, decision) { const text = decision === 'REJECT' ? window.prompt('Motivo del rechazo:') : null; if (decision === 'REJECT' && !text?.trim()) return; try { await api(`/admin/shelters/${item.id}/verification`, { method: 'PATCH', body: { decision, reason: text || null } }); await loadAdminData(); } catch (e) { error.value = e.message; } }
async function userDecision(item, action) { const text = action === 'SUSPEND' ? window.prompt('Motivo de la suspensión:') : null; if (action === 'SUSPEND' && !text?.trim()) return; try { await api(`/admin/users/${item.id}/status`, { method: 'PATCH', body: { action, reason: text || null } }); await loadAdminData(); } catch (e) { error.value = e.message; } }
async function reportDecision(item, action) { const text = window.prompt(action === 'START_REVIEW' ? 'Nota de revisión (opcional):' : 'Detalles de la resolución:'); if (action !== 'START_REVIEW' && !text?.trim()) return; try { await api(`/admin/reports/${item.id}`, { method: 'PATCH', body: { action, details: text || null } }); await loadAdminData(); } catch (e) { error.value = e.message; } }
async function loadAffiliates() { try { affiliates.value = await api('/admin/affiliates'); } catch (e) { error.value = e.message || 'No se pudieron cargar los aliados.'; } }
async function saveAffiliate() { if (affiliateSaving.value) return; affiliateSaving.value = true; error.value = ''; try { await api('/admin/affiliates', { method: 'POST', body: { ...affiliateDraft.value, name: affiliateDraft.value.name.trim(), category: affiliateDraft.value.category.trim().toUpperCase() } }); affiliateDraft.value = { name: '', category: 'VETERINARY', description: '', location: '', websiteUrl: '', discountCode: '', discountText: '', active: true }; notice.value = 'Aliado publicado.'; await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo guardar el aliado.'; } finally { affiliateSaving.value = false; } }
async function toggleAffiliate(item) { try { await api(`/admin/affiliates/${item.id}`, { method: 'PATCH', body: { name: item.name, category: item.category, description: item.description, location: item.location, websiteUrl: item.websiteUrl, discountCode: item.discountCode, discountText: item.discountText, active: !item.active } }); await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo actualizar el aliado.'; } }
async function deleteAffiliate(item) { if (!window.confirm(`¿Eliminar a ${item.name}?`)) return; try { await api(`/admin/affiliates/${item.id}`, { method: 'DELETE' }); await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo eliminar el aliado.'; } }
onMounted(async () => { await loadCurrentUser(); if (!isAdmin.value) { router.replace('/'); return; } await loadQueue(); try { await loadAdminData(); await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo cargar el panel administrativo.'; } });
</script>

<template>
  <div class="admin-app">
    <header class="admin-topbar">
      <button class="admin-menu-toggle" type="button" :aria-expanded="sidebarOpen" aria-controls="admin-sidebar" aria-label="Abrir menú" @click="sidebarOpen = !sidebarOpen"><Icon :name="sidebarOpen ? 'x' : 'menu'" /></button>
      <RouterLink to="/inicio.html" class="admin-brand" aria-label="PetHop, inicio"><img src="/img/image1.png" alt="PetHop"></RouterLink>
      <p class="admin-breadcrumb"><span aria-hidden="true">›</span> Panel <strong>Administrativo</strong></p>
      <div class="admin-user"><span>{{ currentUser?.fullName || 'Administrador' }}</span><span class="admin-avatar"><Icon name="user-round" /></span></div>
    </header>
    <div class="admin-workspace">
      <button v-if="sidebarOpen" type="button" class="admin-sidebar-backdrop" aria-label="Cerrar menú" @click="sidebarOpen = false"></button>
      <aside id="admin-sidebar" class="admin-sidebar" :class="{ 'is-open': sidebarOpen }">
        <nav aria-label="Secciones administrativas"><button v-for="item in adminSections" :key="item.id" type="button" class="admin-nav-item" :class="{ active: activeSection === item.id }" :aria-current="activeSection === item.id ? 'page' : undefined" @click="activeSection = item.id; sidebarOpen = false"><Icon :name="item.icon" /><span>{{ item.label }}</span><b v-if="item.id === 'pets' && pending.length">{{ pending.length }}</b></button></nav>
      </aside>
      <main class="admin-main" aria-labelledby="admin-title">
        <p v-if="error" class="form-error" role="alert">{{ error }}</p><p v-if="notice" class="form-success" role="status">{{ notice }}</p>
        <template v-if="activeSection === 'pets'">
          <section class="admin-section-heading"><div><p class="eyebrow">MODERACIÓN</p><h1 id="admin-title">Cola pendiente</h1><p>{{ pending.length }} esperando la revisión</p></div><button class="primary-button" type="button" :disabled="loading" @click="loadQueue">{{ loading ? 'Actualizando…' : 'Actualizar' }}</button></section>
          <section class="admin-panel" aria-label="Cola de publicaciones">
            <p v-if="loading" class="admin-empty">Cargando publicaciones…</p><p v-else-if="!pending.length" class="admin-empty">No hay publicaciones pendientes.</p>
            <div v-else class="admin-list"><button v-for="pet in pending" :key="pet.id" type="button" class="admin-list-item" @click="openPet(pet)"><span><strong>{{ pet.name }}</strong><small>{{ pet.species }} · {{ pet.shelterName }}</small></span><span class="status-pill">{{ pet.moderationStatus }}</span></button></div>
          </section>
        </template>
        <template v-else-if="activeSection === 'shelters'">
          <section class="admin-section-heading"><div><p class="eyebrow">ORGANIZACIONES</p><h1 id="admin-title">Refugios</h1><p>Perfiles pendientes de verificación</p></div><button class="primary-button" type="button" @click="loadAdminData">Actualizar</button></section>
          <section class="admin-panel"><p v-if="!shelters.length" class="admin-empty">No hay perfiles pendientes.</p><div v-for="item in shelters" :key="item.id" class="admin-row"><span><strong>{{ item.organizationName }}</strong><small>{{ item.city || 'Ubicación no indicada' }} · {{ item.verificationStatus }}</small></span><span><button class="primary-button compact" type="button" @click="shelterDecision(item,'APPROVE')">Aprobar</button><button class="danger-button compact" type="button" @click="shelterDecision(item,'REJECT')">Rechazar</button></span></div></section>
        </template>
        <template v-else-if="activeSection === 'users'">
          <section class="admin-section-heading"><div><p class="eyebrow">CUENTAS</p><h1 id="admin-title">Usuarios</h1><p>Suspende cuentas con motivo y conserva sus registros.</p></div><input v-model="userSearch" class="admin-search" placeholder="Buscar usuario" @change="loadAdminData"></section>
          <section class="admin-panel"><p v-if="!users.length" class="admin-empty">No hay usuarios que coincidan.</p><div v-for="item in users" :key="item.id" class="admin-row"><span><strong>{{ item.fullName }}</strong><small>{{ item.email }} · {{ item.role }} · {{ item.accountStatus }}</small></span><button v-if="item.accountStatus === 'ACTIVE'" class="danger-button compact" type="button" @click="userDecision(item,'SUSPEND')">Suspender</button><button v-else class="primary-button compact" type="button" @click="userDecision(item,'REACTIVATE')">Reactivar</button></div></section>
        </template>
        <template v-else-if="activeSection === 'reports'"><section class="admin-section-heading"><div><p class="eyebrow">SEGURIDAD</p><h1 id="admin-title">Reportes</h1><p>Revisa los reportes enviados por la comunidad.</p></div><button class="primary-button" type="button" @click="loadAdminData">Actualizar</button></section><section class="admin-panel"><p v-if="!reports.length" class="admin-empty">No hay reportes.</p><div v-for="item in reports" :key="item.id" class="admin-row"><span><strong>{{ item.petName }} · {{ item.category }}</strong><small>{{ item.description }} · {{ item.status }}</small></span><span><button v-if="item.status === 'SUBMITTED'" class="secondary-button compact" type="button" @click="reportDecision(item,'START_REVIEW')">Revisar</button><button v-if="item.status === 'UNDER_REVIEW'" class="primary-button compact" type="button" @click="reportDecision(item,'RESOLVE')">Resolver</button><button v-if="item.status === 'UNDER_REVIEW'" class="danger-button compact" type="button" @click="reportDecision(item,'DISMISS')">Descartar</button></span></div></section></template>
        <template v-else-if="activeSection === 'affiliates'"><section class="admin-section-heading"><div><p class="eyebrow">RED DE CONFIANZA</p><h1 id="admin-title">Aliados y beneficios</h1><p>Administra los comercios que aparecen en el directorio público.</p></div><button class="primary-button" type="button" @click="loadAffiliates">Actualizar</button></section><section class="admin-panel affiliate-admin-create"><h2>Agregar aliado</h2><form class="affiliate-admin-form" @submit.prevent="saveAffiliate"><input v-model="affiliateDraft.name" required maxlength="180" placeholder="Nombre del comercio"><input v-model="affiliateDraft.category" required maxlength="40" placeholder="Categoría"><input v-model="affiliateDraft.location" maxlength="180" placeholder="Ubicación"><input v-model="affiliateDraft.websiteUrl" type="url" maxlength="500" placeholder="https://sitio.com"><input v-model="affiliateDraft.discountText" maxlength="240" placeholder="Beneficio"><input v-model="affiliateDraft.discountCode" maxlength="80" placeholder="Código"><textarea v-model="affiliateDraft.description" maxlength="4000" placeholder="Descripción"></textarea><button class="primary-button compact" type="submit" :disabled="affiliateSaving">{{ affiliateSaving ? 'Guardando…' : 'Publicar' }}</button></form></section><section class="admin-panel"><p v-if="!affiliates.length" class="admin-empty">No hay aliados registrados.</p><div v-for="item in affiliates" :key="item.id" class="admin-row"><span><strong>{{ item.name }}</strong><small>{{ item.category }} · {{ item.active ? 'Publicado' : 'Oculto' }}</small></span><span><button class="secondary-button compact" type="button" @click="toggleAffiliate(item)">{{ item.active ? 'Ocultar' : 'Publicar' }}</button><button class="danger-button compact" type="button" @click="deleteAffiliate(item)">Eliminar</button></span></div></section></template>
        <template v-else><section class="admin-section-heading"><div><p class="eyebrow">REGISTRO</p><h1 id="admin-title">Actividad</h1><p>Historial de acciones administrativas</p></div><button class="primary-button" type="button" @click="loadAdminData">Actualizar</button></section><section class="admin-panel"><p v-if="!audit.length" class="admin-empty">No hay actividad registrada.</p><div v-for="item in audit" :key="item.id" class="admin-row"><span><strong>{{ item.action }} · {{ item.entityType }}</strong><small>{{ item.adminName }} · {{ item.reason || 'Sin motivo' }}</small></span><time>{{ new Date(item.createdAt).toLocaleString() }}</time></div></section></template>
    <div v-if="selected" class="dynamic-modal open" role="presentation" @click.self="selected = null"><section class="dynamic-modal-content admin-review" role="dialog" aria-modal="true" aria-labelledby="review-title"><button class="dynamic-modal-close" type="button" aria-label="Cerrar" @click="selected = null">×</button><p class="eyebrow">REVISIÓN DE PUBLICACIÓN</p><div class="review-heading"><div><h2 id="review-title">{{ selected.name || 'Publicación sin nombre' }}</h2><p>Evalúa la evidencia disponible antes de tomar una decisión.</p></div><span class="status-pill">{{ selected.moderationStatus || 'Sin estado' }}</span></div>
      <div class="review-layout"><section class="review-gallery" aria-labelledby="gallery-title"><div class="review-section-heading"><h3 id="gallery-title">Fotografías</h3><span>{{ selected.images?.length || 0 }} imagen{{ selected.images?.length === 1 ? '' : 'es' }}</span></div><div v-if="selected.images?.length" class="review-image-grid"><button v-for="image in selected.images" :key="image.id || image.url" type="button" class="review-image-button" :aria-label="`Ver fotografía ${image.displayOrder + 1}`" @click="previewImage = image"><img :src="imageUrl(image)" :alt="image.originalFilename || `Fotografía de ${selected.name || 'la mascota'}`"><small v-if="image.primary">Principal</small></button></div><div v-else class="review-empty">Esta publicación no tiene fotografías cargadas.</div></section>
        <div class="review-details"><section v-for="section in reviewSections" :key="section.title" class="review-section"><h3>{{ section.title }}</h3><dl class="review-facts"><div v-for="field in section.fields" :key="field[0]"><dt>{{ field[0] }}</dt><dd>{{ field[1] }}</dd></div></dl></section><section class="review-section"><h3>Publicador</h3><dl class="review-facts"><div><dt>Refugio u organización</dt><dd>{{ selected.shelterName || 'No indicado' }}</dd></div></dl><p class="review-data-note">La consulta actual solo proporciona el nombre e identificador del refugio. Estado de cuenta, verificación, contacto y otras publicaciones no están disponibles en este endpoint.</p></section></div>
      </div><section class="review-section review-checklist"><h3>Lista de revisión</h3><p>Marca los puntos que hayas comprobado. Es una ayuda para la decisión y no cambia el resultado automáticamente.</p><label v-for="item in reviewChecks" :key="item[0]"><input v-model="checklist[item[0]]" type="checkbox"> <span>{{ item[1] }}</span></label></section><label class="form-field"><span>Motivo o indicaciones</span><textarea v-model="reason" maxlength="2000" rows="4" placeholder="Obligatorio para rechazo o correcciones"></textarea></label><div class="admin-actions"><button class="primary-button" type="button" :disabled="saving" @click="decide('APPROVE')">Aprobar</button><button class="secondary-button" type="button" :disabled="saving" @click="decide('REQUEST_CORRECTIONS')">Solicitar correcciones</button><button class="danger-button" type="button" :disabled="saving" @click="decide('REJECT')">Rechazar</button></div><section class="review-section"><h3>Historial de moderación</h3><ul class="admin-history"><li v-for="item in history" :key="item.id"><strong>{{ item.action }}</strong><span>{{ item.previousStatus || '—' }} → {{ item.newStatus || '—' }}</span><small>{{ item.adminName || 'Administrador' }} · {{ item.createdAt ? new Date(item.createdAt).toLocaleString('es-MX') : 'Fecha no disponible' }}</small><p v-if="item.reason">{{ item.reason }}</p></li><li v-if="!history.length">Sin decisiones previas.</li></ul></section></section><div v-if="previewImage" class="review-lightbox" role="dialog" aria-modal="true" aria-label="Vista ampliada de fotografía" @click.self="previewImage = null"><button type="button" class="dynamic-modal-close" aria-label="Cerrar fotografía" @click="previewImage = null">×</button><img :src="imageUrl(previewImage)" :alt="previewImage.originalFilename || `Fotografía de ${selected.name || 'la mascota'}`"></div></div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-app { min-height:100vh; background:var(--background); color:var(--text); }
.admin-topbar { position:sticky; top:0; z-index:10; display:flex; align-items:center; gap:22px; min-height:78px; padding:0 clamp(18px,4vw,64px); border-bottom:1px solid var(--border); background:var(--white); }
.admin-menu-toggle { display:grid; width:40px; height:40px; place-items:center; border:1px solid var(--border); border-radius:10px; background:transparent; color:var(--primary-strong); cursor:pointer; }
.admin-brand { display:flex; flex:0 0 auto; align-items:center; }.admin-brand img { display:block; width:112px; max-height:48px; object-fit:contain; }
.admin-breadcrumb { display:flex; align-items:center; gap:12px; margin:0; color:var(--text); font-size:1.15rem; font-weight:750; }.admin-breadcrumb > span { color:var(--text-soft); font-size:1.6rem; }.admin-breadcrumb strong { color:var(--primary); }
.admin-user { display:flex; align-items:center; gap:14px; margin-left:auto; font-size:.9rem; font-weight:750; }.admin-avatar { display:grid; width:48px; height:48px; place-items:center; border:1px solid var(--border); border-radius:14px; background:var(--background); color:var(--primary-strong); }
.admin-workspace { display:grid; grid-template-columns:250px minmax(0,1fr); min-height:calc(100vh - 78px); }
.admin-sidebar { position:sticky; top:78px; align-self:start; display:flex; flex-direction:column; height:calc(100vh - 78px); padding:34px 14px 20px; border-right:1px solid var(--border); background:var(--white); }.admin-sidebar nav { display:grid; gap:6px; }
.admin-nav-item { display:flex; align-items:center; gap:12px; min-height:52px; padding:0 14px; border:0; border-radius:10px; background:transparent; color:var(--text-soft); text-align:left; font-weight:700; cursor:pointer; }.admin-nav-item svg { color:var(--primary-strong); }.admin-nav-item.active { background:var(--background); color:var(--text); }.admin-nav-item b { display:grid; min-width:22px; height:22px; place-items:center; margin-left:auto; border-radius:99px; background:var(--primary); color:#fff; font-size:.75rem; }
.admin-main { width:min(100%,1500px); margin:0 auto; padding:clamp(28px,4vw,58px) clamp(20px,3vw,48px); }.admin-section-heading { display:flex; align-items:center; justify-content:space-between; gap:20px; margin-bottom:26px; }.admin-section-heading .eyebrow { margin:0 0 10px; }.admin-section-heading h1 { margin:0; font-size:clamp(1.7rem,2.5vw,2.4rem); }.admin-section-heading p:last-child { margin:8px 0 0; color:var(--text-soft); }.admin-section-heading>.primary-button { min-width:150px; }
.admin-sidebar-backdrop { display:none; }
.admin-panel,.admin-review { background:var(--white); border:1px solid var(--border); border-radius:18px; padding:24px; }
.admin-empty { display:grid; min-height:280px; place-items:center; margin:0; color:var(--text-soft); text-align:center; }.admin-row { display:flex; align-items:center; justify-content:space-between; gap:16px; padding:14px 0; border-bottom:1px solid var(--border); }.admin-row > span:first-child { display:grid; gap:4px; }.admin-row small { color:var(--text-soft); }.compact { min-height:36px; padding:0 11px; margin-left:6px; font-size:.72rem; }.admin-search { min-height:44px; border:1px solid var(--border); border-radius:10px; padding:0 14px; background:var(--white); color:var(--text); }
.section-heading { display:flex; align-items:center; justify-content:space-between; gap:18px; margin-bottom:18px; }
.admin-list { display:grid; gap:9px; }.admin-list-item { display:flex; align-items:center; justify-content:space-between; width:100%; padding:15px; border:1px solid var(--border); border-radius:12px; background:var(--background); color:var(--text); text-align:left; cursor:pointer; }.admin-list-item:hover { border-color:var(--primary); }.admin-list-item span:first-child { display:grid; gap:4px; }.admin-list-item small { color:var(--text-soft); }.status-pill { color:var(--primary-strong); font-size:.72rem; font-weight:800; }.admin-review { width:min(1080px,100%); max-height:92vh; overflow:auto; }.review-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; margin-bottom:20px; }.review-heading p { margin:4px 0 0; color:var(--text-soft); }.review-layout { display:grid; grid-template-columns:minmax(260px,.85fr) minmax(360px,1.15fr); gap:22px; }.review-gallery,.review-section { border:1px solid var(--border); border-radius:14px; padding:18px; }.review-section + .review-section { margin-top:16px; }.review-section-heading { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:14px; }.review-section-heading h3,.review-section h3 { margin:0; }.review-section-heading span { color:var(--text-soft); font-size:.85rem; }.review-image-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }.review-image-button { position:relative; min-height:150px; padding:0; overflow:hidden; border:1px solid var(--border); border-radius:10px; background:var(--background); cursor:zoom-in; }.review-image-button img { display:block; width:100%; height:150px; object-fit:cover; }.review-image-button small { position:absolute; left:7px; bottom:7px; padding:4px 7px; border-radius:6px; background:rgb(0 0 0 / 68%); color:var(--white); }.review-empty,.review-data-note { color:var(--text-soft); line-height:1.5; }.review-facts { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:13px 18px; margin:15px 0 0; }.review-facts div { min-width:0; }.review-facts dt { color:var(--text-soft); font-size:.78rem; font-weight:750; }.review-facts dd { margin:3px 0 0; overflow-wrap:anywhere; white-space:pre-line; }.review-data-note { margin:16px 0 0; font-size:.88rem; }.review-checklist p { color:var(--text-soft); margin:8px 0 14px; }.review-checklist label { display:flex; gap:9px; align-items:flex-start; margin:10px 0; }.review-checklist input { width:18px; min-width:18px; height:18px; min-height:18px; flex:0 0 18px; margin:3px 0 0; padding:0; accent-color:var(--primary-strong); }.admin-actions { display:flex; flex-wrap:wrap; gap:9px; margin:18px 0 26px; }.secondary-button,.danger-button { min-height:42px; padding:0 15px; border-radius:10px; border:1px solid var(--border); background:var(--white); cursor:pointer; font-weight:750; }.danger-button { border-color:var(--danger); color:var(--danger); }.admin-history { padding-left:20px; color:var(--text-soft); }.admin-history li { margin:10px 0; }.admin-history li span,.admin-history li small { display:block; margin-top:3px; }.admin-history li p { margin:4px 0 0; color:var(--text); }.review-lightbox { position:fixed; inset:0; z-index:20; display:grid; place-items:center; padding:30px; background:rgb(0 0 0 / 82%); }.review-lightbox img { max-width:min(100%,1100px); max-height:90vh; object-fit:contain; }.review-lightbox .dynamic-modal-close { color:var(--white); border-color:var(--white); }
.affiliate-admin-create { margin-bottom:16px; }.affiliate-admin-create h2 { margin:0 0 14px; font-size:1.05rem; }.affiliate-admin-form { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }.affiliate-admin-form input,.affiliate-admin-form textarea { min-height:42px; padding:9px 11px; border:1px solid var(--border); border-radius:9px; background:var(--background); color:var(--text); font:500 .78rem "Montserrat",sans-serif; }.affiliate-admin-form textarea { grid-column:1/-1; min-height:80px; resize:vertical; }.affiliate-admin-form button { justify-self:start; }
@media (max-width:760px) { .admin-topbar { gap:12px; min-height:68px; padding:0 14px; }.admin-brand img { width:88px; }.admin-breadcrumb { gap:6px; font-size:.95rem; }.admin-user>span:first-child { display:none; }.admin-avatar { width:40px; height:40px; }.admin-workspace { display:block; min-height:calc(100vh - 68px); }.admin-sidebar { position:fixed; z-index:12; inset:68px auto 0 0; width:min(300px,84vw); height:auto; transform:translateX(-102%); transition:transform .2s ease; box-shadow:0 14px 38px rgb(0 0 0 / 16%); }.admin-sidebar.is-open { transform:translateX(0); }.admin-sidebar-backdrop { position:fixed; z-index:11; inset:68px 0 0; display:block; border:0; background:rgb(0 0 0 / 35%); }.admin-main { padding:28px 16px; }.admin-section-heading { align-items:flex-start; }.admin-section-heading>.primary-button { min-width:auto; }.admin-panel { padding:18px; }.admin-row { align-items:flex-start; flex-wrap:wrap; }.review-layout { grid-template-columns:1fr; }.review-facts { grid-template-columns:1fr; }.review-heading { flex-direction:column; }.review-image-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }.admin-review { padding:18px; } }
</style>
