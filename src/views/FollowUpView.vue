<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import { getPet } from '../services/pets.js';
import { storageService } from '../services/storage.js';
const route = useRoute();
const applications = storageService.read('pethop_applications', []);
const list = Array.isArray(applications) ? applications : [];
const application = computed(() => list.find(item => item.id === route.query.application) || (route.query.application ? null : list.at(-1)));
const pet = computed(() => application.value ? getPet(application.value.petId) : null);
const status = computed(() => application.value?.status || 'Sin solicitud activa');
const statusLabel = computed(() => ({ Recibida: 'Solicitud guardada', 'En revisión': 'En revisión', Contactada: 'Contacto iniciado', Aprobada: 'Adopción en proceso', Adoptada: 'Adopción completada', Rechazada: 'Solicitud no aprobada' }[status.value] || status.value));
const followUps = ref(readFollowUps());
const saveError = ref('');
const savedNotice = ref('');
const form = reactive({ status: '', date: new Date().toISOString().slice(0, 10), notes: '' });
const petImage = computed(() => pet.value?.photos?.[0] || `/img/${pet.value?.image || 'img2.jpeg'}`);
const labels = { bien: 'Todo marcha bien', adaptacion: 'En proceso de adaptación', atencion: 'Requiere atención' };
function readFollowUps() { const saved = storageService.read('pethop_followups', []); return Array.isArray(saved) ? saved.filter(item => item && typeof item === 'object') : []; }
function save() {
  const item = { id: `FOLLOW-${Date.now().toString(36).toUpperCase()}`, date: new Date().toISOString(), status: form.status || 'Actualización', wellbeing: 'Sin especificar', notes: form.notes };
  const next = [...followUps.value, item];
  if (storageService.write('pethop_followups', next)) { followUps.value = next; form.status = ''; form.notes = ''; saveError.value = ''; savedNotice.value = 'Nota guardada en este dispositivo.'; }
  else { saveError.value = 'No se pudo guardar la actualización en este dispositivo.'; savedNotice.value = ''; }
}
const orderedFollowUps = computed(() => [...followUps.value].reverse());
</script>
<template>
  <div class="follow-up-page"><SiteHeader /><main class="follow-up-main"><section class="follow-up-header"><div class="follow-up-header-container"><h1>Consulta el avance <span>de tu adopción.</span></h1><p>Revisa el estado de tus solicitudes y guarda notas personales sobre el bienestar de tu mascota.</p></div></section>
    <section class="follow-up-section"><div class="follow-up-container"><div class="follow-up-layout"><div class="follow-up-main">
      <div class="status-card"><div class="status-card-header"><div><span class="eyebrow">ESTADO ACTUAL</span><h2>{{ application ? 'Proceso de adopción' : 'Todavía no tienes solicitudes' }}</h2></div><span class="status-pending" data-application-status>{{ statusLabel }}</span></div>
        <div v-if="application" class="status-timeline"><div v-for="(step, index) in [{title:'Solicitud guardada',text:'Quedó guardada en este dispositivo. El refugio aún debe confirmar su recepción.'},{title:'Revisión de solicitud',text:'Pendiente de confirmación de la organización.'},{title:'Contacto',text:'Se establecerá comunicación para continuar el proceso.'},{title:'Adopción',text:'Registro de la adopción y comienzo del seguimiento.'}]" :key="step.title" class="status-step" :class="{ 'is-current': index === ({ Recibida:0,'En revisión':1,Contactada:2,Aprobada:3,Adoptada:3 }[status] ?? 0), 'is-complete': index < ({ Recibida:0,'En revisión':1,Contactada:2,Aprobada:3,Adoptada:4 }[status] ?? 0) }"><div class="status-step-marker"><Icon v-if="index === 0 || index < ({ Recibida:0,'En revisión':1,Contactada:2,Aprobada:3,Adoptada:4 }[status] ?? 0)" name="check" /><span v-else>{{ index + 1 }}</span></div><div class="status-step-content"><strong>{{ step.title }}</strong><p>{{ step.text }}</p></div></div></div>
        <div v-else class="follow-up-empty"><p>Cuando envíes una solicitud, aquí podrás consultar el estado y los pasos siguientes.</p></div>
      </div>
      <article v-if="application && pet" class="follow-up-pet-card"><div class="follow-up-pet-image" :style="{ backgroundImage: `url('${petImage}')` }"><span class="sr-only">Imagen de {{ pet.name }}</span></div><div class="follow-up-pet-info"><span class="pet-species">SOLICITUD ACTIVA</span><h2>{{ pet.name }}</h2><p>Solicitud de adopción · {{ application.id }}</p><div class="follow-up-health"><span>Información registrada</span><span>Proceso activo</span></div></div></article>
      <div class="follow-up-record-card"><div class="follow-up-record-header"><div><span class="eyebrow">REGISTRO</span><h2>{{ application ? 'Actualización de seguimiento' : 'Notas personales' }}</h2><p>Tus notas se guardan en este dispositivo y no cambian el estado oficial de la solicitud.</p></div></div><form @submit.prevent="save"><div class="follow-up-form-grid"><div class="form-field"><label for="followUpStatus">Estado</label><select id="followUpStatus" v-model="form.status" name="status" required><option value="">Selecciona una opción</option><option value="bien">Todo marcha bien</option><option value="adaptacion">En proceso de adaptación</option><option value="atencion">Requiere atención</option></select></div><div class="form-field"><label for="followUpDate">Fecha</label><input id="followUpDate" v-model="form.date" name="date" type="date" required></div><div class="form-field form-field-full"><label for="followUpNotes">Notas</label><textarea id="followUpNotes" v-model="form.notes" name="notes" required placeholder="Describe cómo se encuentra la mascota."></textarea></div></div><p v-if="saveError" class="auth-feedback is-error" role="alert">{{ saveError }}</p><p v-else-if="savedNotice" class="auth-feedback" role="status">{{ savedNotice }}</p><div class="follow-up-form-actions"><button type="submit" class="btn btn-primary">Guardar actualización</button></div></form></div>
      <div class="follow-up-history-card"><div class="follow-up-record-header"><div><span class="eyebrow">HISTORIAL</span><h2>Actualizaciones registradas</h2></div></div><div class="follow-up-list" id="followUpList"><div v-if="!orderedFollowUps.length" class="follow-up-empty"><h3>Aún no hay actualizaciones.</h3><p>Las actualizaciones que registres aparecerán aquí.</p></div><article v-for="item in orderedFollowUps" :key="item.id" class="follow-up-record-card"><div class="follow-up-record-header"><strong>{{ labels[item.status] || item.status || 'Actualización personal' }}</strong><span>{{ new Date(item.date).toLocaleDateString('es-MX') }}</span></div><div class="summary-row"><span>Bienestar</span><strong>{{ item.wellbeing || 'Sin especificar' }}</strong></div><p v-if="item.notes" class="follow-up-note">{{ item.notes }}</p></article></div></div>
    </div><aside class="follow-up-sidebar"><div class="follow-up-summary"><span class="eyebrow">RESUMEN</span><h2>{{ pet?.name || 'Sin solicitud' }}</h2><p>{{ application ? 'Solicitud activa' : 'Aún no hay solicitudes registradas' }}</p><div class="summary-divider"></div><div class="summary-row"><span>Estado</span><strong>{{ status }}</strong></div><div class="summary-row"><span>Solicitud</span><strong>{{ application?.id || '—' }}</strong></div><div class="summary-row"><span>Ubicación</span><strong>{{ pet?.location || '—' }}</strong></div></div><div class="follow-up-help"><span class="eyebrow">IMPORTANTE</span><h2>Mantén actualizada la información.</h2><p>El seguimiento permite conservar un registro organizado del proceso posterior a la adopción y facilita la comunicación con la organización.</p></div></aside></div></div></section></main><SiteFooter /></div>
</template>
