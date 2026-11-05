<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import { API_BASE, api } from '../services/api.js';
import { currentUser, loadCurrentUser } from '../services/auth.js';

const router = useRouter();
const pending = ref([]); const selected = ref(null); const history = ref([]);
const loading = ref(true); const saving = ref(false); const error = ref(''); const notice = ref(''); const reason = ref('');
const shelters = ref([]); const users = ref([]); const reports = ref([]); const audit = ref([]); const activeSection = ref('pets'); const userSearch = ref('');
const previewImage = ref(null); const checklist = ref({});
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
  const [s, u, r, a] = await Promise.all([api('/admin/shelters/pending'), api(`/admin/users?size=50${query}`), api('/admin/reports?size=50'), api('/admin/audit?size=50')]);
  shelters.value = s || []; users.value = u.content || []; reports.value = r.content || []; audit.value = a.content || [];
}
async function shelterDecision(item, decision) { const text = decision === 'REJECT' ? window.prompt('Motivo del rechazo:') : null; if (decision === 'REJECT' && !text?.trim()) return; try { await api(`/admin/shelters/${item.id}/verification`, { method: 'PATCH', body: { decision, reason: text || null } }); await loadAdminData(); } catch (e) { error.value = e.message; } }
async function userDecision(item, action) { const text = action === 'SUSPEND' ? window.prompt('Motivo de la suspensión:') : null; if (action === 'SUSPEND' && !text?.trim()) return; try { await api(`/admin/users/${item.id}/status`, { method: 'PATCH', body: { action, reason: text || null } }); await loadAdminData(); } catch (e) { error.value = e.message; } }
async function reportDecision(item, action) { const text = window.prompt(action === 'START_REVIEW' ? 'Nota de revisión (opcional):' : 'Detalles de la resolución:'); if (action !== 'START_REVIEW' && !text?.trim()) return; try { await api(`/admin/reports/${item.id}`, { method: 'PATCH', body: { action, details: text || null } }); await loadAdminData(); } catch (e) { error.value = e.message; } }
onMounted(async () => { await loadCurrentUser(); if (!isAdmin.value) { router.replace('/'); return; } await loadQueue(); try { await loadAdminData(); } catch (e) { error.value = e.message || 'No se pudo cargar el panel administrativo.'; } });
</script>

<template>
  <SiteHeader />
  <main class="page-shell" aria-labelledby="admin-title">
    <section class="page-hero"><p class="eyebrow">ADMINISTRACIÓN</p><h1 id="admin-title">Moderación de publicaciones</h1><p>Revisa la información antes de que una mascota aparezca en el catálogo público.</p></section>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p><p v-if="notice" class="form-success" role="status">{{ notice }}</p>
    <section class="admin-panel" aria-label="Cola de publicaciones">
      <div class="section-heading"><div><h2>Cola pendiente</h2><p>{{ pending.length }} publicación{{ pending.length === 1 ? '' : 'es' }} esperando revisión.</p></div><button class="primary-button" type="button" @click="loadQueue">Actualizar</button></div>
      <p v-if="loading">Cargando publicaciones…</p><p v-else-if="!pending.length">No hay publicaciones pendientes.</p>
      <div v-else class="admin-list"><button v-for="pet in pending" :key="pet.id" type="button" class="admin-list-item" @click="openPet(pet)"><span><strong>{{ pet.name }}</strong><small>{{ pet.species }} · {{ pet.shelterName }}</small></span><span class="status-pill">{{ pet.moderationStatus }}</span></button></div>
    </section>
    <nav class="admin-tabs" aria-label="Secciones administrativas"><button v-for="tab in [['pets','Mascotas'],['shelters','Refugios'],['users','Usuarios'],['reports','Reportes'],['audit','Actividad']]" :key="tab[0]" type="button" :class="{ active: activeSection === tab[0] }" @click="activeSection = tab[0]">{{ tab[1] }}</button></nav>
    <section v-if="activeSection === 'shelters'" class="admin-panel"><h2>Refugios pendientes de verificación</h2><p v-if="!shelters.length">No hay perfiles pendientes.</p><div v-for="item in shelters" :key="item.id" class="admin-row"><span><strong>{{ item.organizationName }}</strong><small>{{ item.city || 'Ubicación no indicada' }} · {{ item.verificationStatus }}</small></span><span><button class="primary-button compact" type="button" @click="shelterDecision(item,'APPROVE')">Aprobar</button><button class="danger-button compact" type="button" @click="shelterDecision(item,'REJECT')">Rechazar</button></span></div></section>
    <section v-if="activeSection === 'users'" class="admin-panel"><div class="section-heading"><div><h2>Usuarios</h2><p>Suspende cuentas con motivo y conserva sus registros.</p></div><input v-model="userSearch" class="admin-search" placeholder="Buscar usuario" @change="loadAdminData"></div><div v-for="item in users" :key="item.id" class="admin-row"><span><strong>{{ item.fullName }}</strong><small>{{ item.email }} · {{ item.role }} · {{ item.accountStatus }}</small></span><button v-if="item.accountStatus === 'ACTIVE'" class="danger-button compact" type="button" @click="userDecision(item,'SUSPEND')">Suspender</button><button v-else class="primary-button compact" type="button" @click="userDecision(item,'REACTIVATE')">Reactivar</button></div></section>
    <section v-if="activeSection === 'reports'" class="admin-panel"><h2>Reportes</h2><p v-if="!reports.length">No hay reportes.</p><div v-for="item in reports" :key="item.id" class="admin-row"><span><strong>{{ item.petName }} · {{ item.category }}</strong><small>{{ item.description }} · {{ item.status }}</small></span><span><button v-if="item.status === 'SUBMITTED'" class="secondary-button compact" type="button" @click="reportDecision(item,'START_REVIEW')">Revisar</button><button v-if="item.status === 'UNDER_REVIEW'" class="primary-button compact" type="button" @click="reportDecision(item,'RESOLVE')">Resolver</button><button v-if="item.status === 'UNDER_REVIEW'" class="danger-button compact" type="button" @click="reportDecision(item,'DISMISS')">Descartar</button></span></div></section>
    <section v-if="activeSection === 'audit'" class="admin-panel"><h2>Actividad administrativa</h2><div v-for="item in audit" :key="item.id" class="admin-row"><span><strong>{{ item.action }} · {{ item.entityType }}</strong><small>{{ item.adminName }} · {{ item.reason || 'Sin motivo' }}</small></span><time>{{ new Date(item.createdAt).toLocaleString() }}</time></div></section>
    <div v-if="selected" class="dynamic-modal open" role="presentation" @click.self="selected = null"><section class="dynamic-modal-content admin-review" role="dialog" aria-modal="true" aria-labelledby="review-title"><button class="dynamic-modal-close" type="button" aria-label="Cerrar" @click="selected = null">×</button><p class="eyebrow">REVISIÓN DE PUBLICACIÓN</p><div class="review-heading"><div><h2 id="review-title">{{ selected.name || 'Publicación sin nombre' }}</h2><p>Evalúa la evidencia disponible antes de tomar una decisión.</p></div><span class="status-pill">{{ selected.moderationStatus || 'Sin estado' }}</span></div>
      <div class="review-layout"><section class="review-gallery" aria-labelledby="gallery-title"><div class="review-section-heading"><h3 id="gallery-title">Fotografías</h3><span>{{ selected.images?.length || 0 }} imagen{{ selected.images?.length === 1 ? '' : 'es' }}</span></div><div v-if="selected.images?.length" class="review-image-grid"><button v-for="image in selected.images" :key="image.id || image.url" type="button" class="review-image-button" :aria-label="`Ver fotografía ${image.displayOrder + 1}`" @click="previewImage = image"><img :src="imageUrl(image)" :alt="image.originalFilename || `Fotografía de ${selected.name || 'la mascota'}`"><small v-if="image.primary">Principal</small></button></div><div v-else class="review-empty">Esta publicación no tiene fotografías cargadas.</div></section>
        <div class="review-details"><section v-for="section in reviewSections" :key="section.title" class="review-section"><h3>{{ section.title }}</h3><dl class="review-facts"><div v-for="field in section.fields" :key="field[0]"><dt>{{ field[0] }}</dt><dd>{{ field[1] }}</dd></div></dl></section><section class="review-section"><h3>Publicador</h3><dl class="review-facts"><div><dt>Refugio u organización</dt><dd>{{ selected.shelterName || 'No indicado' }}</dd></div></dl><p class="review-data-note">La consulta actual solo proporciona el nombre e identificador del refugio. Estado de cuenta, verificación, contacto y otras publicaciones no están disponibles en este endpoint.</p></section></div>
      </div><section class="review-section review-checklist"><h3>Lista de revisión</h3><p>Marca los puntos que hayas comprobado. Es una ayuda para la decisión y no cambia el resultado automáticamente.</p><label v-for="item in reviewChecks" :key="item[0]"><input v-model="checklist[item[0]]" type="checkbox"> <span>{{ item[1] }}</span></label></section><label class="form-field"><span>Motivo o indicaciones</span><textarea v-model="reason" maxlength="2000" rows="4" placeholder="Obligatorio para rechazo o correcciones"></textarea></label><div class="admin-actions"><button class="primary-button" type="button" :disabled="saving" @click="decide('APPROVE')">Aprobar</button><button class="secondary-button" type="button" :disabled="saving" @click="decide('REQUEST_CORRECTIONS')">Solicitar correcciones</button><button class="danger-button" type="button" :disabled="saving" @click="decide('REJECT')">Rechazar</button></div><section class="review-section"><h3>Historial de moderación</h3><ul class="admin-history"><li v-for="item in history" :key="item.id"><strong>{{ item.action }}</strong><span>{{ item.previousStatus || '—' }} → {{ item.newStatus || '—' }}</span><small>{{ item.adminName || 'Administrador' }} · {{ item.createdAt ? new Date(item.createdAt).toLocaleString('es-MX') : 'Fecha no disponible' }}</small><p v-if="item.reason">{{ item.reason }}</p></li><li v-if="!history.length">Sin decisiones previas.</li></ul></section></section><div v-if="previewImage" class="review-lightbox" role="dialog" aria-modal="true" aria-label="Vista ampliada de fotografía" @click.self="previewImage = null"><button type="button" class="dynamic-modal-close" aria-label="Cerrar fotografía" @click="previewImage = null">×</button><img :src="imageUrl(previewImage)" :alt="previewImage.originalFilename || `Fotografía de ${selected.name || 'la mascota'}`"></div></div>
  </main>
</template>

<style scoped>
.admin-panel,.admin-review { background:var(--white); border:1px solid var(--border); border-radius:18px; padding:24px; }
.admin-tabs { display:flex; flex-wrap:wrap; gap:8px; margin:22px 0 12px; }.admin-tabs button { border:1px solid var(--border); border-radius:10px; background:var(--white); padding:10px 14px; cursor:pointer; font-weight:750; }.admin-tabs button.active { background:var(--primary-strong); color:var(--white); }.admin-row { display:flex; align-items:center; justify-content:space-between; gap:16px; padding:14px 0; border-bottom:1px solid var(--border); }.admin-row > span:first-child { display:grid; gap:4px; }.admin-row small { color:var(--text-soft); }.compact { min-height:36px; padding:0 11px; margin-left:6px; font-size:.72rem; }.admin-search { min-height:40px; border:1px solid var(--border); border-radius:9px; padding:0 10px; }
.section-heading { display:flex; align-items:center; justify-content:space-between; gap:18px; margin-bottom:18px; }
.admin-list { display:grid; gap:9px; }.admin-list-item { display:flex; align-items:center; justify-content:space-between; width:100%; padding:15px; border:1px solid var(--border); border-radius:12px; background:var(--background); color:var(--text); text-align:left; cursor:pointer; }.admin-list-item:hover { border-color:var(--primary); }.admin-list-item span:first-child { display:grid; gap:4px; }.admin-list-item small { color:var(--text-soft); }.status-pill { color:var(--primary-strong); font-size:.72rem; font-weight:800; }.admin-review { width:min(1080px,100%); max-height:92vh; overflow:auto; }.review-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; margin-bottom:20px; }.review-heading p { margin:4px 0 0; color:var(--text-soft); }.review-layout { display:grid; grid-template-columns:minmax(260px,.85fr) minmax(360px,1.15fr); gap:22px; }.review-gallery,.review-section { border:1px solid var(--border); border-radius:14px; padding:18px; }.review-section + .review-section { margin-top:16px; }.review-section-heading { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:14px; }.review-section-heading h3,.review-section h3 { margin:0; }.review-section-heading span { color:var(--text-soft); font-size:.85rem; }.review-image-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }.review-image-button { position:relative; min-height:150px; padding:0; overflow:hidden; border:1px solid var(--border); border-radius:10px; background:var(--background); cursor:zoom-in; }.review-image-button img { display:block; width:100%; height:150px; object-fit:cover; }.review-image-button small { position:absolute; left:7px; bottom:7px; padding:4px 7px; border-radius:6px; background:rgb(0 0 0 / 68%); color:var(--white); }.review-empty,.review-data-note { color:var(--text-soft); line-height:1.5; }.review-facts { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:13px 18px; margin:15px 0 0; }.review-facts div { min-width:0; }.review-facts dt { color:var(--text-soft); font-size:.78rem; font-weight:750; }.review-facts dd { margin:3px 0 0; overflow-wrap:anywhere; white-space:pre-line; }.review-data-note { margin:16px 0 0; font-size:.88rem; }.review-checklist p { color:var(--text-soft); margin:8px 0 14px; }.review-checklist label { display:flex; gap:9px; align-items:flex-start; margin:10px 0; }.review-checklist input { width:18px; min-width:18px; height:18px; min-height:18px; flex:0 0 18px; margin:3px 0 0; padding:0; accent-color:var(--primary-strong); }.admin-actions { display:flex; flex-wrap:wrap; gap:9px; margin:18px 0 26px; }.secondary-button,.danger-button { min-height:42px; padding:0 15px; border-radius:10px; border:1px solid var(--border); background:var(--white); cursor:pointer; font-weight:750; }.danger-button { border-color:var(--danger); color:var(--danger); }.admin-history { padding-left:20px; color:var(--text-soft); }.admin-history li { margin:10px 0; }.admin-history li span,.admin-history li small { display:block; margin-top:3px; }.admin-history li p { margin:4px 0 0; color:var(--text); }.review-lightbox { position:fixed; inset:0; z-index:20; display:grid; place-items:center; padding:30px; background:rgb(0 0 0 / 82%); }.review-lightbox img { max-width:min(100%,1100px); max-height:90vh; object-fit:contain; }.review-lightbox .dynamic-modal-close { color:var(--white); border-color:var(--white); }
@media (max-width:760px) { .review-layout { grid-template-columns:1fr; }.review-facts { grid-template-columns:1fr; }.review-heading { flex-direction:column; }.review-image-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }.admin-review { padding:18px; } }
</style>
