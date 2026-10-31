<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { getPetById } from '../services/petApi.js';
import { api } from '../services/api.js';
const route = useRoute();
const application = ref(null); const adoption = ref(null); const pet = ref(null); const history = ref([]);
const status = computed(() => application.value?.status || 'Sin solicitud activa');
const statusIndex = computed(() => ({ SUBMITTED: 0, UNDER_REVIEW: 1, APPROVED: 2, REJECTED: 2, WITHDRAWN: 2 }[status.value] ?? 0));
const statusLabel = computed(() => ({ SUBMITTED: 'Solicitud recibida', UNDER_REVIEW: 'En revisión', APPROVED: adoption.value ? 'Adopción formalizada' : 'Solicitud aprobada', REJECTED: 'Solicitud no aprobada', WITHDRAWN: 'Solicitud retirada' }[status.value] || status.value));
const followUps = ref([]);
const saveError = ref('');
const savedNotice = ref('');
const form = reactive({ status: '', date: new Date().toISOString().slice(0, 10), notes: '' });
const petImage = computed(() => pet.value?.photos?.[0] || '');
const labels = { GOOD: 'Todo marcha bien', ADAPTING: 'En proceso de adaptación', NEEDS_ATTENTION: 'Requiere atención' };
async function loadTracking() {
  try {
    const page = await api('/adoption-applications/mine?page=0&size=50');
    application.value = route.query.application
      ? await api('/adoption-applications/' + route.query.application)
      : page.content?.[0] || null;
    if (!application.value) return;
    // Once an application is approved, the pet leaves the public catalog.
    // Keep tracking visible using the application snapshot in that case.
    try {
      pet.value = await getPetById(application.value.petId);
    } catch {
      pet.value = { id: application.value.petId, name: application.value.petName, location: '', photos: [] };
    }
    history.value = await api('/adoption-applications/' + application.value.id + '/history');
    const adoptions = await api('/adoptions/mine?page=0&size=50');
    adoption.value = adoptions.content?.find(item => item.applicationId === application.value.id) || null;
    if (adoption.value) followUps.value = await api('/adoptions/' + adoption.value.id + '/follow-ups');
  } catch (error) { saveError.value = error.message || 'No se pudo cargar el seguimiento desde el servidor.'; }
}
async function save() {
  if (!adoption.value) { saveError.value = 'Los reportes de seguimiento se habilitan después de formalizar la adopción.'; return; }
  try {
    const item = await api('/adoptions/' + adoption.value.id + '/follow-ups', { method: 'POST', body: {
      reportDate: form.date, petCondition: form.status, generalNotes: form.notes.trim()
    } });
    followUps.value = [item, ...followUps.value]; form.status = ''; form.notes = ''; saveError.value = ''; savedNotice.value = 'Actualización guardada en PetHop.';
  } catch (error) { saveError.value = error.message || 'No se pudo guardar la actualización.'; savedNotice.value = ''; }
}
const orderedFollowUps = computed(() => followUps.value);
onMounted(loadTracking);
</script>
<template>
  <div class="follow-up-page"><SiteHeader /><main class="follow-up-main"><section class="follow-up-header"><div class="follow-up-header-container"><h1>Consulta el avance <span>de tu adopción.</span></h1><p>Consulta el estado oficial de tu solicitud y registra reportes después de formalizar la adopción.</p></div></section>
    <section class="follow-up-section"><div class="follow-up-container"><div class="follow-up-layout"><div class="follow-up-main">
      <div class="status-card"><div class="status-card-header"><div><span class="eyebrow">ESTADO ACTUAL</span><h2>{{ application ? 'Proceso de adopción' : 'Todavía no tienes solicitudes' }}</h2></div><span class="status-pending" data-application-status>{{ statusLabel }}</span></div>
        <div v-if="application" class="status-timeline"><div v-for="(item,index) in history" :key="item.id" class="status-step" :class="{ 'is-current': index === history.length - 1, 'is-complete': index < history.length - 1 }"><div class="status-step-marker"><Icon v-if="index < history.length - 1" name="check" /><span v-else>{{ index + 1 }}</span></div><div class="status-step-content"><strong>{{ item.newStatus }}</strong><p>{{ item.comment || new Date(item.changedAt).toLocaleString('es-MX') }}</p></div></div></div>
        <div v-else class="follow-up-empty"><p>Cuando envíes una solicitud, aquí podrás consultar el estado y los pasos siguientes.</p></div>
      </div>
      <article v-if="application && pet" class="follow-up-pet-card"><ImageWithSkeleton frame-class="follow-up-pet-image" :src="petImage" :alt="`Imagen de ${pet.name}`" role="img" /><div class="follow-up-pet-info"><span class="pet-species">SOLICITUD ACTIVA</span><h2>{{ pet.name }}</h2><p>Solicitud de adopción · {{ application.id }}</p><div class="follow-up-health"><span>Información registrada</span><span>Proceso activo</span></div></div></article>
      <div class="follow-up-record-card"><div class="follow-up-record-header"><div><span class="eyebrow">REGISTRO</span><h2>Reporte de seguimiento</h2><p v-if="adoption">Este reporte quedará asociado a la adopción y visible para las personas participantes.</p><p v-else>El reporte se habilita cuando el refugio formalice la adopción. El estado de la solicitud se muestra desde el servidor.</p></div></div><form v-if="adoption" @submit.prevent="save"><div class="follow-up-form-grid"><div class="form-field"><label for="followUpStatus">Bienestar</label><select id="followUpStatus" v-model="form.status" name="status" required><option value="">Selecciona una opción</option><option value="GOOD">Todo marcha bien</option><option value="ADAPTING">En proceso de adaptación</option><option value="NEEDS_ATTENTION">Requiere atención</option></select></div><div class="form-field"><label for="followUpDate">Fecha</label><input id="followUpDate" v-model="form.date" name="date" type="date" required></div><div class="form-field form-field-full"><label for="followUpNotes">Notas</label><textarea id="followUpNotes" v-model="form.notes" name="notes" required placeholder="Describe cómo se encuentra la mascota."></textarea></div></div><p v-if="saveError" class="auth-feedback is-error" role="alert">{{ saveError }}</p><p v-else-if="savedNotice" class="auth-feedback" role="status">{{ savedNotice }}</p><div class="follow-up-form-actions"><button type="submit" class="btn btn-primary">Guardar reporte</button></div></form><p v-else-if="saveError" class="auth-feedback is-error" role="alert">{{ saveError }}</p></div>
      <div v-if="adoption" class="follow-up-history-card"><div class="follow-up-record-header"><div><span class="eyebrow">HISTORIAL</span><h2>Reportes registrados</h2></div></div><div class="follow-up-list" id="followUpList"><div v-if="!orderedFollowUps.length" class="follow-up-empty"><h3>Aún no hay reportes.</h3><p>Los reportes enviados por ti o por el refugio aparecerán aquí.</p></div><article v-for="item in orderedFollowUps" :key="item.id" class="follow-up-record-card"><div class="follow-up-record-header"><strong>{{ labels[item.petCondition] || item.petCondition }}</strong><span>{{ new Date(item.reportDate).toLocaleDateString('es-MX') }}</span></div><p v-if="item.generalNotes" class="follow-up-note">{{ item.generalNotes }}</p></article></div></div>
    </div><aside class="follow-up-sidebar"><div class="follow-up-summary"><span class="eyebrow">RESUMEN</span><h2>{{ pet?.name || 'Sin solicitud' }}</h2><p>{{ application ? 'Solicitud activa' : 'Aún no hay solicitudes registradas' }}</p><div class="summary-divider"></div><div class="summary-row"><span>Estado</span><strong>{{ status }}</strong></div><div class="summary-row"><span>Solicitud</span><strong>{{ application?.id || '—' }}</strong></div><div class="summary-row"><span>Ubicación</span><strong>{{ pet?.location || '—' }}</strong></div></div><div class="follow-up-help"><span class="eyebrow">IMPORTANTE</span><h2>Mantén actualizada la información.</h2><p>El seguimiento permite conservar un registro organizado del proceso posterior a la adopción y facilita la comunicación con la organización.</p></div></aside></div></div></section></main><SiteFooter /></div>
</template>
