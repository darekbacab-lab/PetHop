<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { api, API_BASE } from '../services/api.js';
import { getPetById } from '../services/petApi.js';

const route = useRoute();
const router = useRouter();
const applications = ref([]); const adoptions = ref([]); const selectedId = ref('');
const petSnapshots = ref({}); const history = ref([]); const reports = ref([]); const visits = ref([]); const reportMediaSources = ref({});
const loading = ref(true); const selectionLoading = ref(false); const error = ref(''); const selectionError = ref(''); const saveError = ref(''); const savedNotice = ref('');
const selectedRequest = ref(null); const uploadFiles = ref([]); const loadSequence = ref(0);
let loadingWatchdog;
const form = reactive({ status: '', date: new Date().toISOString().slice(0, 10), notes: '' });
const visitForm = reactive({ date: new Date().toISOString().slice(0, 10), status: 'PLANNED', outcome: '', notes: '', nextAction: '' });

const conditionLabels = { GOOD: 'Todo marcha bien', ADAPTING: 'En proceso de adaptación', NEEDS_ATTENTION: 'Requiere atención' };
const statusLabels = { DRAFT: 'Borrador', SUBMITTED: 'Solicitud enviada', UNDER_REVIEW: 'En revisión', APPROVED: 'Aprobada', REJECTED: 'No aprobada', WITHDRAWN: 'Retirada' };
const statusTone = { SUBMITTED: 'pending', UNDER_REVIEW: 'pending', APPROVED: 'positive', REJECTED: 'negative', WITHDRAWN: 'neutral' };
const currentApplication = computed(() => applications.value.find(item => item.id === selectedId.value) || null);
const currentAdoption = computed(() => currentApplication.value?.status === 'APPROVED'
  ? adoptions.value.find(item => item.applicationId === currentApplication.value.id)
  : null);
const currentPet = computed(() => currentApplication.value ? petSnapshots.value[currentApplication.value.petId] || { id: currentApplication.value.petId, name: currentApplication.value.petName, photos: [] } : null);
const currentStatus = computed(() => currentApplication.value?.status || '');
const currentStatusLabel = computed(() => currentApplication.value?.adoptionStatus === 'ADOPTED' || currentPet.value?.adoptionStatus === 'ADOPTED' || currentAdoption.value
  ? 'Adoptada'
  : statusLabels[currentStatus.value] || 'Sin solicitud');
const currentTone = computed(() => currentApplication.value?.adoptionStatus === 'ADOPTED' || currentPet.value?.adoptionStatus === 'ADOPTED' || currentAdoption.value
  ? 'positive'
  : statusTone[currentStatus.value] || 'neutral');
const orderedReports = computed(() => [...reports.value].sort((a, b) => String(b.reportDate).localeCompare(String(a.reportDate))));
const summary = computed(() => ({
  sent: applications.value.length,
  review: applications.value.filter(item => item.status === 'UNDER_REVIEW').length,
  approved: applications.value.filter(item => item.status === 'APPROVED').length,
  formalized: applications.value.filter(item => isFormalized(item)).length,
  reports: applications.value.reduce((total, item) => total + (item.id === currentApplication.value?.id ? reports.value.length : 0), 0)
}));

function isFormalized(application) {
  return application.adoptionStatus === 'ADOPTED'
    || adoptions.value.some(item => item.applicationId === application.id)
    || petSnapshots.value[application.petId]?.adoptionStatus === 'ADOPTED';
}
function applicationLabel(application) {
  return isFormalized(application) ? 'Adoptada' : statusLabels[application.status] || application.status;
}
function applicationTone(application) {
  return isFormalized(application) ? 'positive' : statusTone[application.status] || 'neutral';
}

function formatDate(value, withTime = false) {
  if (!value) return 'Sin fecha';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return withTime
    ? date.toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' })
    : date.toLocaleDateString('es-MX', { dateStyle: 'medium' });
}
function imageUrl(url) { if (!url) return ''; return /^https?:\/\//i.test(url) ? url : `${API_BASE.replace(/\/api\/v1$/, '')}${url.startsWith('/') ? '' : '/'}${url}`; }
function statusClass(status) { return `is-${statusTone[status] || 'neutral'}`; }
function hasHistory(status) { return history.value.some(item => item.newStatus === status); }
function historyDate(status) { const item = history.value.find(entry => entry.newStatus === status); return item?.changedAt; }

const stages = computed(() => {
  const app = currentApplication.value; const adoption = currentAdoption.value || app?.adoptionStatus === 'ADOPTED' || currentPet.value?.adoptionStatus === 'ADOPTED'; const rejected = app?.status === 'REJECTED'; const withdrawn = app?.status === 'WITHDRAWN';
  return [
    { key: 'SUBMITTED', title: 'Solicitud enviada', copy: 'Tu información fue enviada al refugio.', done: Boolean(app?.submittedAt) || hasHistory('SUBMITTED'), date: app?.submittedAt || historyDate('SUBMITTED') },
    { key: 'RECEIVED', title: 'Recepción', copy: 'La organización recibió tu solicitud.', done: false, unsupported: true },
    { key: 'UNDER_REVIEW', title: 'En revisión', copy: 'La organización evalúa tu solicitud.', done: hasHistory('UNDER_REVIEW'), date: historyDate('UNDER_REVIEW') },
    { key: 'RESOLUTION', title: rejected ? 'No aprobada' : withdrawn ? 'Retirada' : adoption ? 'Adoptada' : 'Resolución', copy: rejected ? 'El refugio no aprobó esta solicitud.' : withdrawn ? 'Retiraste esta solicitud.' : adoption ? 'La adopción quedó registrada oficialmente.' : 'La organización tomó una decisión.', done: adoption || ['APPROVED', 'REJECTED', 'WITHDRAWN'].some(hasHistory), date: historyDate('APPROVED') || historyDate('REJECTED') || historyDate('WITHDRAWN'), tone: rejected ? 'negative' : withdrawn ? 'neutral' : 'positive' },
    { key: 'FORMALIZED', title: 'Adopción formalizada', copy: adoption ? 'La mascota forma parte de tu hogar.' : 'Se habilita cuando la adopción sea formalizada.', done: Boolean(adoption), date: currentAdoption.value?.adoptionDate },
    { key: 'ADAPTING', title: 'Adaptación', copy: adoption ? 'Comienza la adaptación en el nuevo hogar.' : 'Disponible después de formalizar la adopción.', done: false, future: !adoption },
    { key: 'FOLLOW_UP', title: 'Seguimiento', copy: reports.value.length ? 'Ya existen actualizaciones de bienestar.' : adoption ? 'Registra el primer reporte de bienestar.' : 'Disponible después de formalizar la adopción.', done: reports.value.length > 0, future: !adoption }
  ];
});
const activeStage = computed(() => { const done = stages.value.map((item, index) => item.done ? index : -1).filter(index => index >= 0); return done.length ? Math.min(done[done.length - 1] + 1, stages.value.length - 1) : 0; });

async function loadPetSnapshots(items) {
  const results = await Promise.allSettled(items.map(item => getPetById(item.petId)));
  const next = { ...petSnapshots.value };
  results.forEach((result, index) => { if (result.status === 'fulfilled') next[items[index].petId] = result.value; });
  petSnapshots.value = next;
}
async function loadSelected(id) {
  const sequence = ++loadSequence.value; selectionLoading.value = true; selectionError.value = ''; saveError.value = ''; savedNotice.value = '';
  const app = applications.value.find(item => item.id === id); if (!app) { selectionLoading.value = false; return; }
  // El estado de la solicitud se puede mostrar de inmediato; el historial y la
  // adopción son datos complementarios y no deben bloquear la interfaz.
  selectionLoading.value = false;
  try {
    const [historyResult, adoptionsResult] = await Promise.allSettled([
      api(`/adoption-applications/${id}/history`),
      api('/adoptions/mine?page=0&size=50')
    ]);
    if (sequence !== loadSequence.value) return;
    history.value = historyResult.status === 'fulfilled' ? historyResult.value || [] : [];
    adoptions.value = adoptionsResult.status === 'fulfilled' ? adoptionsResult.value?.content || [] : [];
    if (historyResult.status === 'rejected' && adoptionsResult.status === 'rejected') {
      throw historyResult.reason;
    }
    const adoption = adoptions.value.find(item => item.applicationId === id);
    reports.value = adoption ? await api(`/adoptions/${adoption.id}/follow-ups`) : [];
    visits.value = adoption ? await api(`/adoptions/${adoption.id}/visits`) : [];
    void loadReportMedia(reports.value);
  } catch (requestError) { if (sequence === loadSequence.value) selectionError.value = requestError.message || 'No se pudo cargar el detalle del seguimiento.'; }
  finally { if (sequence === loadSequence.value) selectionLoading.value = false; }
}
async function loadReportMedia(items) {
  const entries = items.flatMap(report => (report.media || []).map(media => ({ report, media })));
  const loaded = await Promise.allSettled(entries.map(async ({ media }) => {
    const response = await fetch(imageUrl(media.url), { credentials: 'include' });
    if (!response.ok) throw new Error(`Image ${response.status}`);
    return [media.id, URL.createObjectURL(await response.blob())];
  }));
  const next = { ...reportMediaSources.value };
  loaded.forEach(result => { if (result.status === 'fulfilled') next[result.value[0]] = result.value[1]; });
  reportMediaSources.value = next;
}
async function selectApplication(id, updateUrl = true) { selectedId.value = id; selectedRequest.value = applications.value.find(item => item.id === id) || null; if (updateUrl) router.replace({ path: '/seguimiento.html', query: { application: id } }); await loadSelected(id); }
async function loadTracking() {
  loading.value = true; error.value = '';
  clearTimeout(loadingWatchdog);
  loadingWatchdog = setTimeout(() => {
    if (loading.value) {
      loading.value = false;
      error.value = 'No se pudieron cargar tus solicitudes. Inténtalo nuevamente.';
    }
  }, 8000);
  try {
    const page = await api('/adoption-applications/mine?page=0&size=50'); applications.value = page?.content || [];
    if (!applications.value.length) return;
    const requested = String(route.query.application || ''); const selected = applications.value.find(item => item.id === requested) || applications.value[0];
    // La lista de solicitudes es la respuesta crítica. El detalle se carga aparte
    // para que historial/adopciones no bloqueen toda la pantalla.
    loading.value = false;
    void loadPetSnapshots([selected]);
    void selectApplication(selected.id, Boolean(requested));
    // Los datos de la mascota se enriquecen en segundo plano para no bloquear la vista.
    void loadPetSnapshots(applications.value);
  } catch (requestError) { error.value = requestError.message || 'No se pudo cargar tu seguimiento.'; }
  finally { clearTimeout(loadingWatchdog); loading.value = false; }
}
function onFiles(event) { uploadFiles.value = [...event.target.files].slice(0, 6); }
async function saveReport() {
  saveError.value = ''; savedNotice.value = '';
  if (!currentAdoption.value) { saveError.value = 'Los reportes se habilitan después de formalizar la adopción.'; return; }
  if (!form.status || !form.date || !form.notes.trim()) { saveError.value = 'Completa el estado, la fecha y las notas del reporte.'; return; }
  try {
    const report = await api(`/adoptions/${currentAdoption.value.id}/follow-ups`, { method: 'POST', body: { reportDate: form.date, petCondition: form.status, generalNotes: form.notes.trim() } });
    let mediaWarning = '';
    for (const file of uploadFiles.value) { try { const data = new FormData(); data.append('file', file); await api(`/follow-ups/${report.id}/media`, { method: 'POST', body: data }); } catch { mediaWarning = ' El reporte se guardó, pero alguna fotografía no pudo adjuntarse.'; } }
    reports.value = await api(`/adoptions/${currentAdoption.value.id}/follow-ups`); await loadReportMedia(reports.value); form.status = ''; form.notes = ''; uploadFiles.value = []; savedNotice.value = `Actualización guardada en PetHop.${mediaWarning}`;
  } catch (requestError) { saveError.value = requestError.message || 'No se pudo guardar la actualización.'; }
}
async function saveVisit() {
  saveError.value = ''; savedNotice.value = '';
  if (!currentAdoption.value) { saveError.value = 'Las visitas se habilitan después de formalizar la adopción.'; return; }
  if (!visitForm.date || !visitForm.status) { saveError.value = 'Completa la fecha y el estado de la visita.'; return; }
  try {
    await api(`/adoptions/${currentAdoption.value.id}/visits`, { method: 'POST', body: { visitDate: visitForm.date, status: visitForm.status, outcome: visitForm.outcome.trim(), notes: visitForm.notes.trim(), nextAction: visitForm.nextAction.trim() } });
    visits.value = await api(`/adoptions/${currentAdoption.value.id}/visits`);
    visitForm.outcome = ''; visitForm.notes = ''; visitForm.nextAction = ''; savedNotice.value = 'Visita registrada en PetHop.';
  } catch (requestError) { saveError.value = requestError.message || 'No se pudo registrar la visita.'; }
}
onMounted(loadTracking);
onBeforeUnmount(() => Object.values(reportMediaSources.value).forEach(url => URL.revokeObjectURL(url)));
</script>

<template>
  <div class="follow-up-page"><SiteHeader />
    <main class="follow-up-main">
      <section class="follow-up-header"><div class="follow-up-header-container"><span class="eyebrow">SEGUIMIENTO DE ADOPCIONES</span><h1>El camino hacia un <span>nuevo hogar.</span></h1><p>Consulta el avance de tus solicitudes y descubre cómo evoluciona la historia de tus adopciones.</p></div></section>
      <section class="follow-up-container follow-up-dashboard">
        <div v-if="loading" class="follow-up-state"><Icon name="refresh-cw" /><p>Cargando tu seguimiento…</p></div>
        <div v-else-if="error" class="follow-up-state is-error"><Icon name="circle-help" /><p>{{ error }}</p><button class="button button-primary" type="button" @click="loadTracking">Reintentar</button></div>
        <div v-else-if="!applications.length" class="follow-up-state"><Icon name="paw-print" /><h2>Aún no tienes solicitudes</h2><p>Explora las mascotas disponibles y encuentra una historia que pueda comenzar contigo.</p><RouterLink class="button button-primary" to="/explorar.html">Explorar mascotas</RouterLink></div>
        <template v-else>
          <section class="follow-up-summary-grid" aria-label="Resumen de adopciones"><article><span class="summary-icon"><Icon name="clipboard-list" /></span><div><strong>{{ summary.sent }}</strong><span>Solicitudes enviadas</span></div></article><article><span class="summary-icon is-amber"><Icon name="refresh-cw" /></span><div><strong>{{ summary.review }}</strong><span>En revisión</span></div></article><article><span class="summary-icon is-green"><Icon name="check-circle-2" /></span><div><strong>{{ summary.approved }}</strong><span>Aprobadas</span></div></article><article><span class="summary-icon is-blue"><Icon name="heart" /></span><div><strong>{{ summary.formalized }}</strong><span>Adopciones formalizadas</span></div></article><article><span class="summary-icon is-purple"><Icon name="route" /></span><div><strong>{{ summary.reports }}</strong><span>Reportes en selección</span></div></article></section>
          <div class="follow-up-content-grid">
            <section class="follow-up-primary">
              <section class="follow-up-card application-selector"><div class="follow-up-card-heading"><div><span class="eyebrow">TUS PROCESOS</span><h2>Selecciona una solicitud</h2></div><span>{{ applications.length }} registradas</span></div><div class="application-list"><button v-for="item in applications" :key="item.id" type="button" class="application-item" :class="{ 'is-selected': item.id === selectedId }" @click="selectApplication(item.id)"><span class="application-thumb"><img v-if="petSnapshots[item.petId]?.photos?.[0]" :src="petSnapshots[item.petId].photos[0]" :alt="`Imagen de ${item.petName}`"><Icon v-else name="paw-print" /></span><span class="application-item-copy"><strong>{{ item.petName }}</strong><small>{{ formatDate(item.submittedAt) }} · {{ item.id.slice(0, 8) }}</small></span><span class="status-pill" :class="`is-${applicationTone(item)}`">{{ applicationLabel(item) }}</span></button></div></section>
              <section v-if="currentApplication" class="follow-up-card progress-card"><div class="follow-up-card-heading"><div><span class="eyebrow">PROCESO ACTUAL</span><h2>{{ currentStatusLabel }}</h2></div><span v-if="selectionLoading" class="loading-label">Actualizando…</span><span v-else class="status-pill" :class="`is-${currentTone}`">{{ currentStatusLabel }}</span></div><p class="progress-note">La solicitud fue recibida por la organización y su avance se actualizará aquí.</p><div class="progress-track"><article v-for="(stage,index) in stages" :key="stage.key" class="progress-stage" :class="{ 'is-done': stage.done, 'is-active': index === activeStage && !stage.done, 'is-future': stage.future || stage.unsupported, [`is-${stage.tone || 'positive'}`]: stage.tone }"><span class="progress-marker"><Icon v-if="stage.done" name="check" /><span v-else>{{ index + 1 }}</span></span><div><strong>{{ stage.title }}</strong><small>{{ stage.copy }}</small><time v-if="stage.date">{{ formatDate(stage.date) }}</time></div></article></div></section>
              <section v-if="currentApplication" class="follow-up-card activity-card"><div class="follow-up-card-heading"><div><span class="eyebrow">HISTORIAL REAL</span><h2>Actividad de tu solicitud</h2></div></div><div v-if="!history.length" class="inline-empty">Aún no hay eventos de historial disponibles.</div><ol v-else class="activity-list"><li v-for="item in history" :key="item.id"><span class="activity-marker"><Icon name="check" /></span><div><strong>{{ statusLabels[item.newStatus] || item.newStatus }}</strong><p>{{ item.comment || 'Se registró un cambio de estado.' }}</p><time>{{ formatDate(item.changedAt, true) }} · {{ item.changedBy }}</time></div></li></ol></section>
            </section>
            <aside class="follow-up-secondary">
              <section v-if="currentApplication" class="follow-up-card pet-follow-card"><span class="eyebrow">MASCOTA Y SOLICITUD</span><div class="pet-follow-hero"><ImageWithSkeleton frame-class="pet-follow-image" :src="currentPet?.photos?.[0] || ''" :alt="`Imagen de ${currentPet?.name || currentApplication.petName}`" role="img" /><div><h2>{{ currentPet?.name || currentApplication.petName }}</h2><p>{{ currentPet?.species || 'Mascota' }}<span v-if="currentPet?.breed"> · {{ currentPet.breed }}</span></p><span class="status-pill" :class="statusClass(currentStatus)">{{ currentStatusLabel }}</span></div></div><dl class="pet-follow-facts"><div><dt>Solicitud</dt><dd>{{ currentApplication.id }}</dd></div><div><dt>Enviada</dt><dd>{{ formatDate(currentApplication.submittedAt) }}</dd></div><div><dt>Refugio</dt><dd>{{ currentApplication.shelterName || 'No disponible' }}</dd></div><div><dt>Ubicación</dt><dd>{{ currentPet?.location || 'No disponible' }}</dd></div></dl><RouterLink v-if="currentPet" class="follow-health-link" :to="{ path: '/salud.html', query: { pet: currentPet.id } }"><Icon name="clipboard-list" /> Ver expediente sanitario</RouterLink></section>
              <section v-if="currentAdoption" class="follow-up-card wellbeing-card"><div class="follow-up-card-heading"><div><span class="eyebrow">NUEVA VIDA</span><h2>Bienestar y adaptación</h2></div><Icon name="heart" /></div><div class="wellbeing-overview"><strong>{{ reports.length }}</strong><span>reportes registrados</span><small v-if="orderedReports[0]">Último: {{ formatDate(orderedReports[0].reportDate) }}</small><small v-else>Aún no hay reportes</small></div><div class="report-list"><article v-for="report in orderedReports.slice(0, 3)" :key="report.id"><span class="condition-dot" :class="`is-${report.petCondition.toLowerCase()}`"></span><div><strong>{{ conditionLabels[report.petCondition] || report.petCondition }}</strong><time>{{ formatDate(report.reportDate) }}</time><p v-if="report.generalNotes">{{ report.generalNotes }}</p><div v-if="report.media?.length" class="report-media"><img v-for="media in report.media" :key="media.id" :src="reportMediaSources[media.id] || imageUrl(media.url)" :alt="media.originalFilename || 'Fotografía del reporte'"></div></div></article></div></section>
            </aside>
          </div>
          <section v-if="currentAdoption" class="follow-up-card report-form-card"><div class="follow-up-card-heading"><div><span class="eyebrow">ACTUALIZACIÓN</span><h2>La nueva vida de tu mascota</h2><p>Comparte cómo se encuentra después de la adopción formalizada.</p></div><span class="formalized-badge"><Icon name="check-circle-2" /> Adopción formalizada</span></div><form class="report-form" @submit.prevent="saveReport"><label>Estado de bienestar<select v-model="form.status" required><option value="" disabled>Selecciona una opción</option><option value="GOOD">Todo marcha bien</option><option value="ADAPTING">En proceso de adaptación</option><option value="NEEDS_ATTENTION">Requiere atención</option></select></label><label>Fecha<input v-model="form.date" type="date" required></label><label class="report-form-wide">Notas<textarea v-model="form.notes" maxlength="5000" required placeholder="Describe cómo se encuentra tu mascota."></textarea><small>{{ form.notes.length }}/5000</small></label><label class="report-form-wide">Fotografías (opcional)<input type="file" accept="image/*" multiple @change="onFiles"><small>Hasta 6 archivos por reporte; se guardan en el servidor junto con la actualización.</small></label><div class="report-form-wide report-actions"><p v-if="saveError" class="inline-error" role="alert">{{ saveError }}</p><p v-if="savedNotice" class="inline-success" role="status">{{ savedNotice }}</p><button class="button button-primary" type="submit">Guardar actualización</button></div></form></section>
          <section v-if="currentAdoption" class="follow-up-card visits-card"><div class="follow-up-card-heading"><div><h2>Visitas de seguimiento</h2><p>Registra visitas, resultados y próximos acuerdos de la familia.</p></div><span class="visits-count">{{ visits.length }} registradas</span></div><div v-if="visits.length" class="visit-list"><article v-for="visit in visits" :key="visit.id"><div><strong>{{ formatDate(visit.visitDate) }}</strong><span class="visit-status" :class="`is-${visit.status.toLowerCase()}`">{{ visit.status === 'COMPLETED' ? 'Completada' : visit.status === 'CANCELLED' ? 'Cancelada' : 'Programada' }}</span></div><p v-if="visit.outcome"><b>Resultado:</b> {{ visit.outcome }}</p><p v-if="visit.notes">{{ visit.notes }}</p><small v-if="visit.nextAction"><b>Próximo paso:</b> {{ visit.nextAction }}</small></article></div><form class="report-form visit-form" @submit.prevent="saveVisit"><label>Fecha<input v-model="visitForm.date" type="date" required></label><label>Estado<select v-model="visitForm.status" required><option value="PLANNED">Programada</option><option value="COMPLETED">Completada</option><option value="CANCELLED">Cancelada</option></select></label><label>Resultado<input v-model="visitForm.outcome" maxlength="120" placeholder="Ej. Adaptación favorable"></label><label class="report-form-wide">Notas<textarea v-model="visitForm.notes" maxlength="5000" placeholder="Observaciones de la visita."></textarea></label><label class="report-form-wide">Próximo paso<textarea v-model="visitForm.nextAction" maxlength="3000" placeholder="Acuerdos o acciones pendientes."></textarea></label><div class="report-form-wide report-actions"><button class="button button-primary" type="submit">Registrar visita</button></div></form></section>
        </template>
      </section>
    </main><SiteFooter />
  </div>
</template>

<style scoped>
.visits-card { margin-top: 18px; }
.visits-count { color: var(--text-soft); font-size: .85rem; }
.visit-list { display: grid; gap: 10px; margin-bottom: 20px; }
.visit-list article { padding: 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--background); }
.visit-list article > div { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.visit-list p { margin: 8px 0 0; color: var(--text-soft); white-space: pre-line; }
.visit-list small { display: block; margin-top: 8px; color: var(--text-soft); }
.visit-status { padding: 4px 8px; border-radius: 99px; font-size: .72rem; font-weight: 800; }
.visit-status.is-planned { background: #fff4d9; color: #8a5b12; }
.visit-status.is-completed { background: #e6f5ec; color: #287451; }
.visit-status.is-cancelled { background: #fbe9e9; color: #a13232; }
</style>
