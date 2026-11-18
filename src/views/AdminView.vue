<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import Icon from '../components/Icon.vue';
import { API_BASE, api } from '../services/api.js';
import SiteHeader from '../components/SiteHeader.vue';
import { currentUser, loadCurrentUser } from '../services/auth.js';
import { useFocusTrap } from '../services/dialogFocus.js';
import ConfirmDialog from '../components/ConfirmDialog.vue';

const router = useRouter();
const pending = ref([]); const selected = ref(null); const history = ref([]);
const loading = ref(true); const saving = ref(false); const error = ref(''); const notice = ref(''); const reason = ref('');
const queuePage = ref(0); const queueTotal = ref(0); const queueSize = 20; const queueError = ref('');
const queueSpecies = ref(''); const queueShelter = ref(''); const queueDate = ref('');
const queueTotalPages = computed(() => Math.max(1, Math.ceil(queueTotal.value / queueSize)));
const filteredQueue = computed(() => pending.value.filter(pet =>
  (!queueSpecies.value || pet.species === queueSpecies.value)
  && (!queueShelter.value.trim() || (pet.shelterName || '').toLocaleLowerCase('es-MX').includes(queueShelter.value.trim().toLocaleLowerCase('es-MX')))
  && (!queueDate.value || String(pet.publishedAt || '').slice(0, 10) === queueDate.value)));
const queueIndex = computed(() => filteredQueue.value.findIndex(pet => pet.id === selected.value?.id));
const sectionErrors = ref({});
const listTotals = ref({ users: 0, reports: 0, audit: 0 });
const affiliatesError = ref('');
const reasonDialog = ref(null);
const reasonText = ref('');
const reasonDialogError = ref('');
const shelters = ref([]); const users = ref([]); const reports = ref([]); const audit = ref([]); const affiliates = ref([]); const metrics = ref(null); const activeSection = ref('pets'); const userSearch = ref('');
const affiliateDraft = ref({ name: '', category: 'VETERINARY', description: '', location: '', websiteUrl: '', discountCode: '', discountText: '', active: true }); const affiliateSaving = ref(false);
const previewImage = ref(null); const checklist = ref({});
const reviewDialog = ref(null);
useFocusTrap(reviewDialog, () => !!selected.value, () => { if (previewImage.value) previewImage.value = null; else selected.value = null; });
const confirmApprove = ref(false);
function requestDecide(action) { if (action === 'APPROVE') { confirmApprove.value = true; return; } decide(action); }
function verificationLabel(status) { return ({ VERIFIED: 'Verificado', PENDING: 'Pendiente de verificación', REJECTED: 'Verificación rechazada' })[status] || 'Sin información'; }
function moderationLabel(status) { return ({ PENDING: 'En revisión', APPROVED: 'Aprobada', REJECTED: 'Rechazada', CHANGES_REQUESTED: 'Con correcciones solicitadas' })[status] || status || 'Sin estado'; }
function roleLabel(role) { return ({ ADOPTER: 'Adoptante', SHELTER: 'Refugio', ADMIN: 'Administrador' })[role] || role || 'Sin rol'; }
function accountStatusLabel(status) { return ({ ACTIVE: 'Activa', LOCKED: 'Suspendida' })[status] || status || 'Sin estado'; }
function speciesLabel(value) { return ({ DOG: 'Perro', CAT: 'Gato', OTHER: 'Otro' })[value] || value || 'Sin especie'; }
function sexLabel(value) { return ({ MALE: 'Macho', FEMALE: 'Hembra' })[value] || value || 'Sin dato'; }
function sizeLabel(value) { return ({ SMALL: 'Pequeño', MEDIUM: 'Mediano', LARGE: 'Grande' })[value] || value || 'Sin dato'; }
function lifeStageLabel(value) { return ({ PUPPY: 'Cachorro', YOUNG: 'Joven', ADULT: 'Adulto', SENIOR: 'Mayor' })[value] || value || 'Sin dato'; }
function adoptionStatusLabel(status) { return ({ AVAILABLE: 'Publicada', IN_PROCESS: 'En proceso de adopción', ADOPTED: 'Adoptada', UNAVAILABLE: 'Pausada', DRAFT: 'Borrador' })[status] || status || 'Sin estado'; }
function moderationActionLabel(action) { return ({ APPROVE: 'Aprobar', REJECT: 'Rechazar', REQUEST_CORRECTIONS: 'Solicitar correcciones', REOPEN: 'Volver a poner en revisión' })[action] || action || 'Sin acción'; }
function reportStatusLabel(status) { return ({ SUBMITTED: 'Enviado', UNDER_REVIEW: 'En revisión', RESOLVED: 'Resuelto', DISMISSED: 'Descartado' })[status] || status || 'Sin estado'; }
function reportCategoryLabel(category) { return ({ FRAUD: 'Fraude', MISLEADING_INFORMATION: 'Información engañosa', ABUSE: 'Maltrato', INAPPROPRIATE_CONTENT: 'Contenido inapropiado', OTHER: 'Otro' })[category] || category || 'Sin categoría'; }
function auditActionLabel(action) { return ({ USER_ROLE_CHANGED: 'Cambio de rol de usuario', PET_MODERATION_APPROVE: 'Moderación: publicación aprobada', PET_MODERATION_REJECT: 'Moderación: publicación rechazada', PET_MODERATION_REQUEST_CORRECTIONS: 'Moderación: correcciones solicitadas', PET_MODERATION_REOPEN: 'Moderación: vuelta a revisión', SHELTER_VERIFICATION_APPROVE: 'Verificación de refugio aprobada', SHELTER_VERIFICATION_REJECT: 'Verificación de refugio rechazada', REPORT_START_REVIEW: 'Reporte en revisión', REPORT_RESOLVE: 'Reporte resuelto', REPORT_DISMISS: 'Reporte descartado', USER_SUSPEND: 'Usuario suspendido', USER_REACTIVATE: 'Usuario reactivado' })[action] || action || 'Sin acción'; }
function entityTypeLabel(type) { return ({ PET: 'Mascota', USER: 'Usuario', SHELTER: 'Refugio', REPORT: 'Reporte' })[type] || type || 'Sin tipo'; }
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
      ['Nombre', pet.name], ['Especie', speciesLabel(pet.species)], ['Raza', pet.breed], ['Etapa de vida', lifeStageLabel(pet.lifeStage)], ['Edad', age],
      ['Sexo', sexLabel(pet.sex)], ['Tamaño', sizeLabel(pet.size)], ['Peso', pet.weight === null || pet.weight === undefined ? null : `${pet.weight} kg`], ['Color', pet.color], ['Ubicación', pet.location]
    ]) },
    { title: 'Salud y cuidados', fields: fields([
      ['Condición médica', pet.healthCondition], ['Resumen de salud', pet.healthSummary], ['Vacunación', booleanValue(pet.vaccinated)],
      ['Esterilización', booleanValue(pet.sterilized)], ['Desparasitación', booleanValue(pet.dewormed)]
    ]) },
    { title: 'Adopción y moderación', fields: fields([
      ['Estado de adopción', adoptionStatusLabel(pet.adoptionStatus)], ['Estado de moderación', moderationLabel(pet.moderationStatus)],
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
async function loadQueue(page = 0) {
  loading.value = true; queueError.value = '';
  try { const data = await api(`/admin/pets/pending?page=${page}&size=${queueSize}`); pending.value = data.content || []; queueTotal.value = data.totalElements ?? pending.value.length; queuePage.value = data.number ?? page; }
  catch (e) { queueError.value = e.message || 'No se pudo cargar la cola de moderación.'; }
  finally { loading.value = false; }
}
function stepReview(direction) {
  const list = filteredQueue.value; const index = queueIndex.value;
  const next = list[index + direction];
  if (next) openPet(next);
}
async function openPet(pet) {
  error.value = ''; notice.value = '';
  try { selected.value = await api(`/admin/pets/${pet.id}`); history.value = await api(`/admin/pets/${pet.id}/moderation-history`); previewImage.value = null; resetChecklist(); }
  catch (e) { error.value = e.message || 'No se pudo abrir la publicación.'; }
}
async function decide(action) {
  if (!selected.value || saving.value) return;
  if ((action === 'REJECT' || action === 'REQUEST_CORRECTIONS') && !reason.value.trim()) { error.value = 'Indica el motivo de la decisión.'; return; }
  saving.value = true; error.value = '';
  try { await api(`/admin/pets/${selected.value.id}/moderation`, { method: 'PATCH', body: { action, reason: reason.value.trim() || null } }); notice.value = 'Decisión guardada y notificada al responsable.'; reason.value = ''; selected.value = null; await loadQueue(queuePage.value); }
  catch (e) { error.value = e.message || 'No se pudo guardar la decisión.'; }
  finally { saving.value = false; }
}
async function loadAdminData() {
  const query = userSearch.value.trim() ? `&search=${encodeURIComponent(userSearch.value.trim())}` : '';
  sectionErrors.value = {};
  const results = await Promise.allSettled([
    api('/admin/shelters/pending'), api(`/admin/users?size=50${query}`), api('/admin/reports?size=50'), api('/admin/audit?size=50'), api('/admin/metrics')
  ]);
  const [s, u, r, a, m] = results;
  if (s.status === 'fulfilled') shelters.value = s.value || [];
  else sectionErrors.value.shelters = s.reason?.message || 'No se pudieron cargar los refugios pendientes.';
  if (u.status === 'fulfilled') { users.value = u.value?.content || []; listTotals.value.users = u.value?.totalElements ?? users.value.length; }
  else sectionErrors.value.users = u.reason?.message || 'No se pudieron cargar los usuarios.';
  if (r.status === 'fulfilled') { reports.value = r.value?.content || []; listTotals.value.reports = r.value?.totalElements ?? reports.value.length; }
  else sectionErrors.value.reports = r.reason?.message || 'No se pudieron cargar los reportes.';
  if (a.status === 'fulfilled') { audit.value = a.value?.content || []; listTotals.value.audit = a.value?.totalElements ?? audit.value.length; }
  else sectionErrors.value.audit = a.reason?.message || 'No se pudo cargar la actividad.';
  if (m.status === 'fulfilled') metrics.value = m.value;
  else sectionErrors.value.metrics = m.reason?.message || 'No se pudieron cargar las métricas.';
}
function openReasonDialog(config) { reasonDialog.value = config; reasonText.value = ''; reasonDialogError.value = ''; }
function cancelReasonDialog() { reasonDialog.value = null; notice.value = 'Acción cancelada. No se hizo ningún cambio.'; }
async function confirmReasonDialog() {
  const dialog = reasonDialog.value; if (!dialog) return;
  if (dialog.required && !reasonText.value.trim()) { reasonDialogError.value = 'Indica el motivo.'; return; }
  const text = reasonText.value.trim() || null;
  reasonDialog.value = null; reasonText.value = '';
  if (dialog.kind === 'shelter') await runShelterDecision(dialog.item, dialog.action, text);
  else if (dialog.kind === 'user') await runUserDecision(dialog.item, dialog.action, text);
  else await runReportDecision(dialog.item, dialog.action, text);
}
async function shelterDecision(item, decision) {
  if (decision === 'REJECT') { openReasonDialog({ kind: 'shelter', item, action: decision, title: 'Rechazar verificación', label: 'Motivo del rechazo', required: true, placeholder: 'Explica por qué se rechaza la verificación.' }); return; }
  await runShelterDecision(item, decision, null);
}
async function runShelterDecision(item, decision, reasonValue) { try { await api(`/admin/shelters/${item.id}/verification`, { method: 'PATCH', body: { decision, reason: reasonValue } }); notice.value = 'Decisión guardada.'; await loadAdminData(); } catch (e) { error.value = e.message; } }
async function userDecision(item, action) {
  if (action === 'SUSPEND') { openReasonDialog({ kind: 'user', item, action, title: 'Suspender cuenta', label: 'Motivo de la suspensión', required: true, placeholder: 'Explica por qué se suspende la cuenta.' }); return; }
  await runUserDecision(item, action, null);
}
async function runUserDecision(item, action, reasonValue) { try { await api(`/admin/users/${item.id}/status`, { method: 'PATCH', body: { action, reason: reasonValue } }); notice.value = 'Cuenta actualizada.'; await loadAdminData(); } catch (e) { error.value = e.message; } }
async function reportDecision(item, action) {
  if (action === 'START_REVIEW') { openReasonDialog({ kind: 'report', item, action, title: 'Revisar reporte', label: 'Nota de revisión', required: false, placeholder: 'Nota de revisión (opcional).' }); return; }
  openReasonDialog({ kind: 'report', item, action, title: action === 'RESOLVE' ? 'Resolver reporte' : 'Descartar reporte', label: 'Detalles de la resolución', required: true, placeholder: 'Explica cómo se resolvió el reporte.' });
}
async function runReportDecision(item, action, detailsValue) { try { await api(`/admin/reports/${item.id}`, { method: 'PATCH', body: { action, details: detailsValue } }); notice.value = 'Reporte actualizado.'; await loadAdminData(); } catch (e) { error.value = e.message; } }
async function loadAffiliates() { affiliatesError.value = ''; try { affiliates.value = await api('/admin/affiliates'); } catch (e) { affiliatesError.value = e.message || 'No se pudieron cargar los aliados.'; } }
async function saveAffiliate() { if (affiliateSaving.value) return; affiliateSaving.value = true; error.value = ''; try { await api('/admin/affiliates', { method: 'POST', body: { ...affiliateDraft.value, name: affiliateDraft.value.name.trim(), category: affiliateDraft.value.category.trim().toUpperCase() } }); affiliateDraft.value = { name: '', category: 'VETERINARY', description: '', location: '', websiteUrl: '', discountCode: '', discountText: '', active: true }; notice.value = 'Aliado publicado.'; await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo guardar el aliado.'; } finally { affiliateSaving.value = false; } }
async function toggleAffiliate(item) { try { await api(`/admin/affiliates/${item.id}`, { method: 'PATCH', body: { name: item.name, category: item.category, description: item.description, location: item.location, websiteUrl: item.websiteUrl, discountCode: item.discountCode, discountText: item.discountText, active: !item.active } }); await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo actualizar el aliado.'; } }
async function deleteAffiliate(item) { if (!window.confirm(`¿Eliminar a ${item.name}?`)) return; try { await api(`/admin/affiliates/${item.id}`, { method: 'DELETE' }); await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo eliminar el aliado.'; } }
onMounted(async () => { await loadCurrentUser(); if (!isAdmin.value) { router.replace('/'); return; } await loadQueue(); try { await loadAdminData(); await loadAffiliates(); } catch (e) { error.value = e.message || 'No se pudo cargar el panel administrativo.'; } });
</script>

<template>
  <div class="admin-app">
    <SiteHeader />
    <a class="skip-link" href="#main-content">Saltar al contenido</a>
    <header class="admin-topbar">
      <button class="admin-menu-toggle" type="button" :aria-expanded="sidebarOpen" aria-controls="admin-sidebar" aria-label="Abrir menú" @click="sidebarOpen = !sidebarOpen"><Icon :name="sidebarOpen ? 'x' : 'menu'" /></button>
      <RouterLink to="/inicio.html" class="admin-brand" aria-label="PetHop, inicio"><img src="/img/image1.png" alt="PetHop"></RouterLink>
      <p class="admin-breadcrumb"><span aria-hidden="true">›</span> Panel <strong>Administrativo</strong></p>
      <div class="admin-user"><span>{{ currentUser?.fullName || 'Administrador' }}</span><span class="admin-avatar"><Icon name="user-round" /></span></div>
    </header>
    <div class="admin-workspace">
      <button v-if="sidebarOpen" type="button" class="admin-sidebar-backdrop" aria-label="Cerrar menú" @click="sidebarOpen = false"></button>
      <aside id="admin-sidebar" class="admin-sidebar" :class="{ 'is-open': sidebarOpen }">
        <nav aria-label="Secciones administrativas"><button v-for="item in adminSections" :key="item.id" type="button" class="admin-nav-item" :class="{ active: activeSection === item.id }" :aria-current="activeSection === item.id ? 'page' : undefined" @click="activeSection = item.id; sidebarOpen = false"><Icon :name="item.icon" /><span>{{ item.label }}</span><b v-if="item.id === 'pets' && queueTotal">{{ queueTotal }}</b></button></nav>
      </aside>
      <main id="main-content" tabindex="-1" class="admin-main" aria-labelledby="admin-title">
        <section v-if="metrics" class="admin-metrics" aria-label="Métricas de PetHop"><button type="button" @click="activeSection = 'pets'"><strong>{{ metrics.totalPets }}</strong><span>Mascotas registradas</span></button><button type="button" @click="activeSection = 'pets'"><strong>{{ metrics.availablePets }}</strong><span>Disponibles</span></button><button type="button" @click="activeSection = 'audit'"><strong>{{ metrics.openApplications }}</strong><span>Solicitudes abiertas</span></button><button type="button" @click="activeSection = 'audit'"><strong>{{ metrics.formalizedAdoptions }}</strong><span>Adopciones formalizadas</span></button><button type="button" @click="activeSection = 'shelters'"><strong>{{ metrics.activeShelters }}</strong><span>Refugios activos</span></button></section>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p><p v-if="notice" class="form-success" role="status">{{ notice }}</p>
        <template v-if="activeSection === 'pets'">
          <section class="admin-section-heading"><div><p class="eyebrow">MODERACIÓN</p><h1 id="admin-title">Cola pendiente</h1><p>{{ queueTotal }} esperando la revisión</p></div><button class="primary-button" type="button" :disabled="loading" @click="loadQueue(queuePage)">{{ loading ? 'Actualizando…' : 'Actualizar' }}</button></section>
          <div class="admin-queue-filters"><label>Especie<select v-model="queueSpecies"><option value="">Todas</option><option value="DOG">Perro</option><option value="CAT">Gato</option><option value="OTHER">Otro</option></select></label><label>Refugio<input v-model="queueShelter" type="search" placeholder="Buscar refugio"></label><label>Fecha de publicación<input v-model="queueDate" type="date"></label></div>
          <section class="admin-panel" aria-label="Cola de publicaciones">
            <p v-if="loading" class="admin-empty">Cargando publicaciones…</p><div v-else-if="queueError" class="admin-section-error" role="alert"><p>{{ queueError }}</p><button class="secondary-button" type="button" @click="loadQueue(queuePage)">Reintentar</button></div><p v-else-if="!filteredQueue.length" class="admin-empty">{{ queueSpecies || queueShelter || queueDate ? 'No hay publicaciones que coincidan con los filtros.' : 'No hay publicaciones pendientes.' }}</p>
            <div v-else class="admin-list"><button v-for="pet in filteredQueue" :key="pet.id" type="button" class="admin-list-item" @click="openPet(pet)"><span><strong>{{ pet.name }}</strong><small>{{ speciesLabel(pet.species) }} · {{ pet.shelterName }}</small></span><span class="status-pill">{{ moderationLabel(pet.moderationStatus) }}</span></button></div>
            <div v-if="queueTotalPages > 1" class="admin-pagination"><button class="secondary-button" type="button" :disabled="queuePage <= 0 || loading" @click="loadQueue(queuePage - 1)">← Anterior</button><span>Página {{ queuePage + 1 }} de {{ queueTotalPages }}</span><button class="secondary-button" type="button" :disabled="queuePage >= queueTotalPages - 1 || loading" @click="loadQueue(queuePage + 1)">Siguiente →</button></div>
          </section>
        </template>
        <template v-else-if="activeSection === 'shelters'">
          <section class="admin-section-heading"><div><p class="eyebrow">ORGANIZACIONES</p><h1 id="admin-title">Refugios</h1><p>Perfiles pendientes de verificación</p></div><button class="primary-button" type="button" @click="loadAdminData">Actualizar</button></section>
          <section class="admin-panel"><div v-if="sectionErrors.shelters" class="admin-section-error" role="alert"><p>{{ sectionErrors.shelters }}</p><button class="secondary-button" type="button" @click="loadAdminData">Reintentar</button></div><p v-else-if="!shelters.length" class="admin-empty">No hay perfiles pendientes.</p><div v-for="item in shelters" :key="item.id" class="admin-row"><span><strong>{{ item.organizationName }}</strong><small>{{ item.city || 'Ubicación no indicada' }} · {{ verificationLabel(item.verificationStatus) }}</small></span><span><button class="primary-button compact" type="button" @click="shelterDecision(item,'APPROVE')">Aprobar</button><button class="danger-button compact" type="button" @click="shelterDecision(item,'REJECT')">Rechazar</button></span></div></section>
        </template>
        <template v-else-if="activeSection === 'users'">
          <section class="admin-section-heading"><div><p class="eyebrow">CUENTAS</p><h1 id="admin-title">Usuarios</h1><p>Suspende cuentas con motivo y conserva sus registros.</p></div><label class="admin-search-label">Buscar usuario<input v-model="userSearch" class="admin-search" placeholder="Buscar usuario" @change="loadAdminData"></label></section>
          <section class="admin-panel"><div v-if="sectionErrors.users" class="admin-section-error" role="alert"><p>{{ sectionErrors.users }}</p><button class="secondary-button" type="button" @click="loadAdminData">Reintentar</button></div><p v-else-if="!users.length" class="admin-empty">No hay usuarios que coincidan.</p><p v-else-if="listTotals.users > users.length" class="admin-list-total">Mostrando {{ users.length }} de {{ listTotals.users }} usuarios.</p><div v-for="item in users" :key="item.id" class="admin-row"><span><strong>{{ item.fullName }}</strong><small>{{ item.email }} · {{ roleLabel(item.role) }} · {{ accountStatusLabel(item.accountStatus) }}</small></span><button v-if="item.accountStatus === 'ACTIVE'" class="danger-button compact" type="button" @click="userDecision(item,'SUSPEND')">Suspender</button><button v-else class="primary-button compact" type="button" @click="userDecision(item,'REACTIVATE')">Reactivar</button></div></section>
        </template>
        <template v-else-if="activeSection === 'reports'"><section class="admin-section-heading"><div><p class="eyebrow">SEGURIDAD</p><h1 id="admin-title">Reportes</h1><p>Revisa los reportes enviados por la comunidad.</p></div><button class="primary-button" type="button" @click="loadAdminData">Actualizar</button></section><section class="admin-panel"><div v-if="sectionErrors.reports" class="admin-section-error" role="alert"><p>{{ sectionErrors.reports }}</p><button class="secondary-button" type="button" @click="loadAdminData">Reintentar</button></div><p v-else-if="!reports.length" class="admin-empty">No hay reportes.</p><p v-else-if="listTotals.reports > reports.length" class="admin-list-total">Mostrando {{ reports.length }} de {{ listTotals.reports }} reportes.</p><div v-for="item in reports" :key="item.id" class="admin-row"><span><strong>{{ item.petName }} · {{ reportCategoryLabel(item.category) }}</strong><small>{{ item.description }} · {{ reportStatusLabel(item.status) }}</small></span><span><button v-if="item.status === 'SUBMITTED'" class="secondary-button compact" type="button" @click="reportDecision(item,'START_REVIEW')">Revisar</button><button v-if="item.status === 'UNDER_REVIEW'" class="primary-button compact" type="button" @click="reportDecision(item,'RESOLVE')">Resolver</button><button v-if="item.status === 'UNDER_REVIEW'" class="danger-button compact" type="button" @click="reportDecision(item,'DISMISS')">Descartar</button></span></div></section></template>
        <template v-else-if="activeSection === 'affiliates'"><section class="admin-section-heading"><div><h1 id="admin-title">Aliados y beneficios</h1><p>Administra los comercios que aparecen en el directorio público.</p></div><button class="primary-button" type="button" @click="loadAffiliates">Actualizar</button></section><section class="admin-panel affiliate-admin-create"><h2>Agregar aliado</h2><form class="affiliate-admin-form" @submit.prevent="saveAffiliate"><label>Nombre del comercio *<input v-model="affiliateDraft.name" required maxlength="180" placeholder="Nombre del comercio"></label><label>Categoría *<input v-model="affiliateDraft.category" required maxlength="40" placeholder="Categoría"></label><label>Ubicación<input v-model="affiliateDraft.location" maxlength="180" placeholder="Ubicación"></label><label>Sitio web<input v-model="affiliateDraft.websiteUrl" type="url" maxlength="500" placeholder="https://sitio.com"></label><label>Beneficio<input v-model="affiliateDraft.discountText" maxlength="240" placeholder="Beneficio"></label><label>Código<input v-model="affiliateDraft.discountCode" maxlength="80" placeholder="Código"></label><label class="affiliate-admin-field-wide">Descripción<textarea v-model="affiliateDraft.description" maxlength="4000" placeholder="Descripción"></textarea></label><button class="primary-button compact" type="submit" :disabled="affiliateSaving">{{ affiliateSaving ? 'Guardando…' : 'Publicar' }}</button></form></section><section class="admin-panel"><div v-if="affiliatesError" class="admin-section-error" role="alert"><p>{{ affiliatesError }}</p><button class="secondary-button" type="button" @click="loadAffiliates">Reintentar</button></div><p v-else-if="!affiliates.length" class="admin-empty">No hay aliados registrados.</p><div v-for="item in affiliates" :key="item.id" class="admin-row"><span><strong>{{ item.name }}</strong><small>{{ item.category }} · {{ item.active ? 'Publicado' : 'Oculto' }}</small></span><span><button class="secondary-button compact" type="button" @click="toggleAffiliate(item)">{{ item.active ? 'Ocultar' : 'Publicar' }}</button><button class="danger-button compact" type="button" @click="deleteAffiliate(item)">Eliminar</button></span></div></section></template>
        <template v-else><section class="admin-section-heading"><div><p class="eyebrow">REGISTRO</p><h1 id="admin-title">Actividad</h1><p>Historial de acciones administrativas</p></div><button class="primary-button" type="button" @click="loadAdminData">Actualizar</button></section><section class="admin-panel"><div v-if="sectionErrors.audit" class="admin-section-error" role="alert"><p>{{ sectionErrors.audit }}</p><button class="secondary-button" type="button" @click="loadAdminData">Reintentar</button></div><p v-else-if="!audit.length" class="admin-empty">No hay actividad registrada.</p><p v-else-if="listTotals.audit > audit.length" class="admin-list-total">Mostrando {{ audit.length }} de {{ listTotals.audit }} acciones.</p><div v-for="item in audit" :key="item.id" class="admin-row"><span><strong>{{ auditActionLabel(item.action) }} · {{ entityTypeLabel(item.entityType) }}</strong><small>{{ item.adminName }} · {{ item.reason || 'Sin motivo' }}</small></span><time>{{ new Date(item.createdAt).toLocaleString('es-MX') }}</time></div></section></template>
    <div v-if="selected" class="dynamic-modal open" role="presentation" @click.self="selected = null"><section ref="reviewDialog" class="dynamic-modal-content admin-review" role="dialog" aria-modal="true" aria-labelledby="review-title"><button class="dynamic-modal-close" type="button" aria-label="Cerrar" @click="selected = null">×</button><p class="eyebrow">REVISIÓN DE PUBLICACIÓN</p><div class="review-heading"><div><h2 id="review-title">{{ selected.name || 'Publicación sin nombre' }}</h2><p>Evalúa la evidencia disponible antes de tomar una decisión.</p></div><span class="status-pill">{{ moderationLabel(selected.moderationStatus) }}</span></div>
      <div class="admin-review-nav"><button class="secondary-button" type="button" :disabled="queueIndex <= 0" @click="stepReview(-1)">← Anterior</button><span v-if="queueIndex >= 0">{{ queueIndex + 1 }} de {{ filteredQueue.length }}</span><button class="secondary-button" type="button" :disabled="queueIndex < 0 || queueIndex >= filteredQueue.length - 1" @click="stepReview(1)">Siguiente →</button></div>
      <div class="review-layout"><section class="review-gallery" aria-labelledby="gallery-title"><div class="review-section-heading"><h3 id="gallery-title">Fotografías</h3><span>{{ selected.images?.length || 0 }} imagen{{ selected.images?.length === 1 ? '' : 'es' }}</span></div><div v-if="selected.images?.length" class="review-image-grid"><button v-for="image in selected.images" :key="image.id || image.url" type="button" class="review-image-button" :aria-label="`Ver fotografía ${image.displayOrder + 1}`" @click="previewImage = image"><img :src="imageUrl(image)" :alt="image.originalFilename || `Fotografía de ${selected.name || 'la mascota'}`"><small v-if="image.primary">Principal</small></button></div><div v-else class="review-empty">Esta publicación no tiene fotografías cargadas.</div></section>
        <div class="review-details"><section v-for="section in reviewSections" :key="section.title" class="review-section"><h3>{{ section.title }}</h3><dl class="review-facts"><div v-for="field in section.fields" :key="field[0]"><dt>{{ field[0] }}</dt><dd>{{ field[1] }}</dd></div></dl></section><section class="review-section"><h3>Publicador</h3><dl class="review-facts"><div><dt>Refugio u organización</dt><dd>{{ selected.shelterName || 'No indicado' }}</dd></div><div><dt>Verificación</dt><dd>{{ verificationLabel(selected.shelterVerificationStatus) }}</dd></div><div><dt>Correo de contacto</dt><dd>{{ selected.shelterContactEmail || 'No disponible' }}</dd></div><div><dt>Teléfono de contacto</dt><dd>{{ selected.shelterContactPhone || 'No disponible' }}</dd></div></dl><h4>Otras publicaciones del refugio</h4><ul v-if="selected.shelterOtherPets?.length" class="admin-history"><li v-for="other in selected.shelterOtherPets" :key="other.id"><strong>{{ other.name }}</strong><span>{{ moderationLabel(other.moderationStatus) }}</span></li></ul><p v-else>No tiene otras publicaciones.</p></section></div>
      </div><section class="review-section review-checklist"><h3>Lista de revisión</h3><p>Marca los puntos que hayas comprobado. Es una ayuda para la decisión y no cambia el resultado automáticamente.</p><label v-for="item in reviewChecks" :key="item[0]"><input v-model="checklist[item[0]]" type="checkbox"> <span>{{ item[1] }}</span></label></section><label class="form-field"><span>Motivo o indicaciones</span><textarea v-model="reason" maxlength="2000" rows="4" placeholder="Obligatorio para rechazo o correcciones"></textarea></label><div class="admin-actions"><button class="primary-button" type="button" :disabled="saving" @click="requestDecide('APPROVE')">Aprobar</button><button class="secondary-button" type="button" :disabled="saving" @click="decide('REQUEST_CORRECTIONS')">Solicitar correcciones</button><button class="danger-button" type="button" :disabled="saving" @click="decide('REJECT')">Rechazar</button></div><section class="review-section"><h3>Historial de moderación</h3><ul class="admin-history"><li v-for="item in history" :key="item.id"><strong>{{ moderationActionLabel(item.action) }}</strong><span>{{ item.previousStatus ? moderationLabel(item.previousStatus) : '—' }} → {{ item.newStatus ? moderationLabel(item.newStatus) : '—' }}</span><small>{{ item.adminName || 'Administrador' }} · {{ item.createdAt ? new Date(item.createdAt).toLocaleString('es-MX') : 'Fecha no disponible' }}</small><p v-if="item.reason">{{ item.reason }}</p></li><li v-if="!history.length">Sin decisiones previas.</li></ul></section><div v-if="selected && (selected.moderationStatus === 'APPROVED' || selected.moderationStatus === 'REJECTED')" class="admin-actions"><button class="secondary-button" type="button" :disabled="saving" @click="decide('REOPEN')">Volver a poner en revisión</button></div></section><div v-if="previewImage" class="review-lightbox" role="dialog" aria-modal="true" aria-label="Vista ampliada de fotografía" @click.self="previewImage = null"><button type="button" class="dynamic-modal-close" aria-label="Cerrar fotografía" @click="previewImage = null">×</button><img :src="imageUrl(previewImage)" :alt="previewImage.originalFilename || `Fotografía de ${selected.name || 'la mascota'}`"></div></div>
  <div v-if="reasonDialog" class="dynamic-modal open" role="presentation" @click.self="cancelReasonDialog"><section class="dynamic-modal-content admin-review admin-reason-dialog" role="dialog" aria-modal="true" aria-labelledby="reason-dialog-title"><button class="dynamic-modal-close" type="button" aria-label="Cerrar" @click="cancelReasonDialog">×</button><p class="eyebrow">ADMINISTRACIÓN</p><h2 id="reason-dialog-title">{{ reasonDialog.title }}</h2><label class="form-field"><span>{{ reasonDialog.label }}<span v-if="reasonDialog.required" class="shelter-required"> * Obligatorio</span></span><textarea v-model="reasonText" maxlength="2000" rows="4" :placeholder="reasonDialog.placeholder"></textarea></label><p v-if="reasonDialogError" class="form-error" role="alert">{{ reasonDialogError }}</p><div class="admin-actions"><button class="primary-button" type="button" @click="confirmReasonDialog">Confirmar</button><button class="secondary-button" type="button" @click="cancelReasonDialog">Cancelar</button></div></section></div>
  <ConfirmDialog :open="confirmApprove" title="¿Aprobar esta publicación?" :message="`Aprobar la publicación de ${selected?.name || 'esta mascota'} la hará visible en el catálogo público.`" confirm-label="Aprobar publicación" @confirm="confirmApprove = false; decide('APPROVE')" @cancel="confirmApprove = false" />
      </main>
    </div>
    <nav class="admin-bottombar" aria-label="Secciones administrativas"><button v-for="item in adminSections" :key="item.id" type="button" class="admin-bottombar-item" :class="{ active: activeSection === item.id }" :aria-current="activeSection === item.id ? 'page' : undefined" @click="activeSection = item.id"><Icon :name="item.icon" :size="22" /><span>{{ item.label }}</span><b v-if="item.id === 'pets' && queueTotal">{{ queueTotal }}</b></button></nav>
  </div>
</template>

<style scoped>
.admin-app { min-height:100vh; background:var(--background); color:var(--text); }
.admin-topbar { position:static; display:flex; align-items:center; gap:22px; min-height:78px; padding:0 clamp(18px,4vw,64px); border-bottom:1px solid var(--border); background:var(--white); }
.admin-menu-toggle { display:grid; width:40px; height:40px; place-items:center; border:1px solid var(--border); border-radius:10px; background:transparent; color:var(--primary-strong); cursor:pointer; }
.admin-brand { display:flex; flex:0 0 auto; align-items:center; }.admin-brand img { display:block; width:112px; max-height:48px; object-fit:contain; }
.admin-breadcrumb { display:flex; align-items:center; gap:12px; margin:0; color:var(--text); font-size:1.15rem; font-weight:750; }.admin-breadcrumb > span { color:var(--text-soft); font-size:1.6rem; }.admin-breadcrumb strong { color:#4A5556; }
.admin-user { display:flex; align-items:center; gap:14px; margin-left:auto; font-size:.9rem; font-weight:750; }.admin-avatar { display:grid; width:48px; height:48px; place-items:center; border:1px solid var(--border); border-radius:14px; background:var(--background); color:var(--primary-strong); }
.admin-workspace { display:grid; grid-template-columns:250px minmax(0,1fr); min-height:calc(100vh - 78px); }
.admin-sidebar { position:sticky; top:78px; align-self:start; display:flex; flex-direction:column; height:calc(100vh - 78px); padding:34px 14px 20px; border-right:1px solid var(--border); background:var(--white); }.admin-sidebar nav { display:grid; gap:6px; }
.admin-nav-item { display:flex; align-items:center; gap:12px; min-height:52px; padding:0 14px; border:0; border-radius:10px; background:transparent; color:var(--text-soft); text-align:left; font-weight:700; cursor:pointer; }.admin-nav-item svg { color:var(--primary-strong); }.admin-nav-item.active { background:var(--background); color:var(--text); }.admin-nav-item b { display:grid; min-width:22px; height:22px; place-items:center; margin-left:auto; border-radius:99px; background:var(--primary); color:#fff; font-size:.75rem; }
.admin-main { width:min(100%,1500px); margin:0 auto; padding:clamp(28px,4vw,58px) clamp(20px,3vw,48px); }.admin-section-heading { display:flex; align-items:center; justify-content:space-between; gap:20px; margin-bottom:26px; }.admin-section-heading .eyebrow { margin:0 0 10px; }.admin-section-heading h1 { margin:0; font-size:clamp(1.7rem,2.5vw,2.4rem); }.admin-section-heading p:last-child { margin:8px 0 0; color:var(--text-soft); }.admin-section-heading>.primary-button { min-width:150px; }
.admin-sidebar-backdrop { display:none; }
.admin-bottombar { display:none; }
.admin-panel,.admin-review { background:var(--white); border:1px solid var(--border); border-radius:18px; padding:24px; }
.admin-empty { display:grid; min-height:280px; place-items:center; margin:0; color:var(--text-soft); text-align:center; }
.admin-queue-filters { display:flex; flex-wrap:wrap; gap:12px; margin-bottom:16px; }
.admin-queue-filters label { display:grid; gap:6px; color:var(--text); font-size:.75rem; font-weight:750; }
.admin-queue-filters select,.admin-queue-filters input { min-height:42px; padding:9px 11px; border:1px solid var(--border); border-radius:10px; background:var(--white); color:var(--text); font:500 .8rem "Montserrat",sans-serif; }
.admin-pagination { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-top:18px; }
.admin-pagination span { color:var(--text-soft); font-size:.78rem; }
.admin-section-error { display:grid; min-height:280px; place-items:center; align-content:center; gap:12px; padding:24px; border:1px solid var(--border); border-radius:18px; background:var(--white); color:var(--danger); text-align:center; }
.admin-section-error p { margin:0; }
.admin-list-total { margin:0 0 8px; color:var(--text-soft); font-size:.75rem; }
.admin-review-nav { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:18px; }
.admin-review-nav span { color:var(--text-soft); font-size:.78rem; }
.admin-reason-dialog { width:min(520px,100%); }
.admin-reason-dialog h2 { margin:0 0 16px; color:var(--text); font:750 1.3rem "Poppins",sans-serif; }
.admin-reason-dialog .form-field { display:grid; gap:8px; }
.admin-reason-dialog .form-field textarea { width:100%; min-height:110px; padding:11px 12px; border:1px solid var(--border); border-radius:10px; background:var(--background); color:var(--text); font:500 .85rem "Montserrat",sans-serif; resize:vertical; }.admin-row { display:flex; align-items:center; justify-content:space-between; gap:16px; padding:14px 0; border-bottom:1px solid var(--border); }.admin-row > span:first-child { display:grid; gap:4px; }.admin-row small { color:var(--text-soft); }.compact { min-height:36px; padding:0 11px; margin-left:6px; font-size:.72rem; }.admin-search { min-height:44px; border:1px solid var(--border); border-radius:10px; padding:0 14px; background:var(--white); color:var(--text); }.admin-search-label { display:flex; align-items:center; gap:10px; font-size:.8rem; font-weight:700; }
.section-heading { display:flex; align-items:center; justify-content:space-between; gap:18px; margin-bottom:18px; }
.admin-list { display:grid; gap:9px; }.admin-list-item { display:flex; align-items:center; justify-content:space-between; width:100%; padding:15px; border:1px solid var(--border); border-radius:12px; background:var(--background); color:var(--text); text-align:left; cursor:pointer; }.admin-list-item:hover { border-color:var(--primary); }.admin-list-item span:first-child { display:grid; gap:4px; }.admin-list-item small { color:var(--text-soft); }.status-pill { color:var(--primary-strong); font-size:.72rem; font-weight:800; }.admin-review { width:min(1080px,100%); max-height:92vh; overflow:auto; }.review-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; margin-bottom:20px; }.review-heading p { margin:4px 0 0; color:var(--text-soft); }.review-layout { display:grid; grid-template-columns:minmax(260px,.85fr) minmax(360px,1.15fr); gap:22px; }.review-gallery,.review-section { border:1px solid var(--border); border-radius:14px; padding:18px; }.review-section + .review-section { margin-top:16px; }.review-section-heading { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:14px; }.review-section-heading h3,.review-section h3 { margin:0; }.review-section-heading span { color:var(--text-soft); font-size:.85rem; }.review-image-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }.review-image-button { position:relative; min-height:150px; padding:0; overflow:hidden; border:1px solid var(--border); border-radius:10px; background:var(--background); cursor:zoom-in; }.review-image-button img { display:block; width:100%; height:150px; object-fit:cover; }.review-image-button small { position:absolute; left:7px; bottom:7px; padding:4px 7px; border-radius:6px; background:rgb(0 0 0 / 68%); color:var(--white); }.review-empty,.review-data-note { color:var(--text-soft); line-height:1.5; }.review-facts { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:13px 18px; margin:15px 0 0; }.review-facts div { min-width:0; }.review-facts dt { color:var(--text-soft); font-size:.78rem; font-weight:750; }.review-facts dd { margin:3px 0 0; overflow-wrap:anywhere; white-space:pre-line; }.review-data-note { margin:16px 0 0; font-size:.88rem; }.review-checklist p { color:var(--text-soft); margin:8px 0 14px; }.review-checklist label { display:flex; gap:9px; align-items:flex-start; margin:10px 0; }.review-checklist input { width:18px; min-width:18px; height:18px; min-height:18px; flex:0 0 18px; margin:3px 0 0; padding:0; accent-color:var(--primary-strong); }.admin-actions { display:flex; flex-wrap:wrap; gap:9px; margin:18px 0 26px; }.secondary-button,.danger-button { min-height:42px; padding:0 15px; border-radius:10px; border:1px solid var(--border); background:var(--white); cursor:pointer; font-weight:750; }.danger-button { border-color:var(--danger); color:var(--danger); }.admin-history { padding-left:20px; color:var(--text-soft); }.admin-history li { margin:10px 0; }.admin-history li span,.admin-history li small { display:block; margin-top:3px; }.admin-history li p { margin:4px 0 0; color:var(--text); }.review-lightbox { position:fixed; inset:0; z-index:20; display:grid; place-items:center; padding:30px; background:rgb(0 0 0 / 82%); }.review-lightbox img { max-width:min(100%,1100px); max-height:90vh; object-fit:contain; }.review-lightbox .dynamic-modal-close { color:var(--white); border-color:var(--white); }
.affiliate-admin-create { margin-bottom:16px; }.affiliate-admin-create h2 { margin:0 0 14px; font-size:1.05rem; }.affiliate-admin-form { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }.affiliate-admin-form label { display:grid; gap:6px; font-size:.75rem; font-weight:700; }.affiliate-admin-field-wide { grid-column:1/-1; }.affiliate-admin-form input,.affiliate-admin-form textarea { min-height:42px; padding:9px 11px; border:1px solid var(--border); border-radius:9px; background:var(--background); color:var(--text); font:500 .78rem "Montserrat",sans-serif; }.affiliate-admin-form textarea { grid-column:1/-1; min-height:80px; resize:vertical; }.affiliate-admin-form button { justify-self:start; }
@media (max-width:760px) { .admin-topbar { gap:12px; min-height:68px; padding:0 14px; }.admin-brand img { width:88px; }.admin-breadcrumb { gap:6px; font-size:.95rem; }.admin-user>span:first-child { display:none; }.admin-avatar { width:40px; height:40px; }.admin-workspace { display:block; min-height:calc(100vh - 68px); }.admin-main { padding:28px 16px 96px; }.admin-menu-toggle { display:none; }.admin-sidebar { display:none; }.admin-sidebar-backdrop { display:none !important; }.admin-bottombar { display:flex; position:fixed; left:0; right:0; bottom:0; z-index:12; padding:6px 4px calc(6px + env(safe-area-inset-bottom)); border-top:1px solid var(--border); background:var(--white); box-shadow:0 -6px 18px rgb(0 0 0 / 08%); }.admin-bottombar-item { position:relative; flex:1; min-width:0; display:grid; justify-items:center; gap:3px; padding:6px 2px 4px; border:0; border-radius:10px; background:transparent; color:var(--text-soft); font-size:.75rem; font-weight:700; cursor:pointer; }.admin-bottombar-item svg { color:var(--primary-strong); }.admin-bottombar-item.active { background:var(--background); color:var(--text); }.admin-bottombar-item b { position:absolute; top:2px; left:calc(50% + 6px); display:grid; min-width:16px; height:16px; place-items:center; padding:0 4px; border-radius:99px; background:var(--primary); color:#fff; font-size:.75rem; }.admin-section-heading { flex-direction:column; align-items:stretch; gap:14px; }.admin-section-heading h1 { font-size:1.55rem; }.admin-section-heading>.primary-button { min-width:auto; width:100%; }.admin-section-heading>.admin-search { width:100%; }.admin-panel { padding:18px; }.admin-row { align-items:flex-start; flex-wrap:wrap; }.review-layout { grid-template-columns:1fr; }.review-facts { grid-template-columns:1fr; }.review-heading { flex-direction:column; }.review-image-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }.admin-review { padding:18px; } }
.admin-metrics { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:12px; margin-bottom:26px; }
.admin-metrics button { display:grid; gap:4px; padding:16px; border:1px solid var(--border); border-radius:14px; background:var(--white); box-shadow:var(--shadow-sm); cursor:pointer; text-align:left; font:inherit; }
.admin-metrics strong { color:var(--primary-strong); font:800 1.7rem/1 "Poppins",sans-serif; }
.admin-metrics span { color:var(--text-soft); font-size:.75rem; }
@media (max-width:760px) { .admin-metrics { grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; } .admin-metrics button { padding:12px; } .admin-metrics strong { font-size:1.35rem; } .admin-metrics span { font-size:.68rem; } .admin-metrics button:last-child { grid-column:1 / -1; } }
</style>
