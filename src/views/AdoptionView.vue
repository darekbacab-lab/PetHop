<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { getPetById } from '../services/petApi.js';
import { api } from '../services/api.js';

const route = useRoute(); const router = useRouter();
const pet = ref({ id: '', name: '', species: '', location: '', vaccination: '', deworming: '', sterilization: '' });
const petLoadError = ref('');
const petLoading = ref(false);
const accepted = ref(false); const error = ref(''); const documentFiles = ref([]); const documentNotice = ref(''); const submitting = ref(false);
const user = ref(null);
const petImage = computed(() => pet.value?.photos?.[0] || '');
const form = reactive({ fullName: '', email: '', phone: '', age: '', housing: '', outdoorSpace: '', people: '', permission: '', otherPets: '', otherPetsType: '', otherPetsDetails: '', reason: '', experience: '' });
watch(() => form.otherPets, value => { if (value !== 'si') { form.otherPetsType = ''; form.otherPetsDetails = ''; } });
onMounted(async () => {
  try {
    user.value = await api('/auth/me');
    form.fullName = user.value.fullName || ''; form.email = user.value.email || ''; form.phone = user.value.phone || '';
  } catch { await router.replace({ path: '/acceso.html', query: { redirect: route.fullPath } }); return; }
  if (user.value.role === 'SHELTER') { await router.replace({ path: '/refugio.html', query: { section: 'solicitudes' } }); return; }
  if (user.value.role === 'ADMIN') { await router.replace({ path: '/admin.html' }); return; }
  if (!route.query.pet) { petLoadError.value = 'Selecciona una mascota desde el catálogo para iniciar una solicitud.'; return; }
  petLoading.value = true;
  try { pet.value = await getPetById(route.query.pet); }
  catch (error) { petLoadError.value = error.message || 'No pudimos cargar esta mascota.'; }
  finally { petLoading.value = false; }
});
async function submit(event) {
  if (!event.currentTarget.reportValidity()) return;
  if (submitting.value) return;
  error.value = '';
  submitting.value = true;
  try {
    const application = await api('/adoption-applications', { method: 'POST', body: {
      petId: pet.value.id, motivation: form.reason.trim(), housingType: form.housing,
      hasOtherPets: form.otherPets === 'si', hasChildren: null, experience: form.experience.trim() || null,
      availability: `Espacio exterior: ${form.outdoorSpace}; personas en el hogar: ${form.people}; todos de acuerdo: ${form.permission}`,
      additionalInformation: [`Edad: ${form.age}`, form.otherPetsType, form.otherPetsDetails].filter(Boolean).join(' · ') || null
    } });
    let documentWarning = '';
    for (const item of documentFiles.value) { try { const data = new FormData(); data.append('documentType', item.type); data.append('file', item.file); await api(`/adoption-applications/${application.id}/documents`, { method: 'POST', body: data }); } catch { documentWarning = ` La solicitud se envió, pero el documento ${item.file.name} no pudo cargarse.`; } }
    if (documentWarning) sessionStorage.setItem('pethop:application-warning', documentWarning.trim());
    await router.push({ path: '/seguimiento.html', query: { application: application.id } });
  } catch (failure) { error.value = failure.message || 'No se pudo enviar la solicitud. Revisa tu conexión e inténtalo de nuevo.'; }
  finally { submitting.value = false; }
}
function onDocumentFiles(event) {
  const files = [...event.target.files];
  const rejected = [];
  const valid = [];
  for (const file of files) {
    if (file.size > 10 * 1024 * 1024) { rejected.push(`${file.name} supera los 10 MB y no se agregó.`); continue; }
    valid.push(file);
  }
  const taken = valid.slice(0, 5);
  if (valid.length > 5) rejected.push(`Solo se tomaron los primeros 5 archivos (${valid.slice(5).map(file => file.name).join(', ')} no se agregaron).`);
  documentFiles.value = taken.map(file => ({ file, type: file.type === 'application/pdf' ? 'IDENTITY_OR_ADDRESS' : 'SUPPORTING_IMAGE' }));
  documentNotice.value = rejected.join(' ');
}
</script>

<template>
  <div class="application-page"><SiteHeader /><main id="main-content" tabindex="-1">
    <section class="application-header"><div class="application-header-container"><h1>Da el siguiente <span>paso.</span></h1><p>Completa la información necesaria para iniciar el proceso de adopción de {{ pet.name || 'esta mascota' }}.</p></div></section>
    <section class="application-section"><div class="application-container"><div class="application-main">
      <div v-if="petLoadError" class="catalog-empty" role="alert"><h2>No pudimos cargar esta mascota</h2><p>{{ petLoadError }}</p><RouterLink class="primary-button" to="/explorar.html">Explorar mascotas</RouterLink></div>
      <form v-if="!petLoadError" class="application-form" @submit.prevent="submit">
        <section class="form-section"><div class="form-section-heading"><div><h2>Cuéntanos sobre ti.</h2><p>Estos datos permitirán identificar a la persona que realiza la solicitud.</p></div></div><div class="form-grid">
          <div class="form-field"><label for="fullName">Nombre completo</label><input id="fullName" v-model="form.fullName" name="fullName" readonly required placeholder="Escribe tu nombre"></div>
          <div class="form-field"><label for="email">Correo electrónico</label><input id="email" v-model="form.email" name="email" type="email" readonly required placeholder="correo@ejemplo.com"></div>
          <div class="form-field"><label for="phone">Teléfono</label><input id="phone" v-model="form.phone" name="phone" type="tel" readonly placeholder="Actualiza tu teléfono en tu perfil"><RouterLink class="form-field-link" to="/perfil.html" target="_blank" rel="noopener">Actualiza tu teléfono en tu perfil</RouterLink></div>
          <div class="form-field"><label for="age">Edad</label><input id="age" v-model="form.age" name="age" type="number" min="18" required placeholder="18"></div>
        </div></section>
        <section class="form-section"><div class="form-section-heading"><div><h2>Cuéntanos sobre tu hogar.</h2><p>Queremos conocer las condiciones en las que viviría la mascota.</p></div></div><div class="form-grid">
          <div class="form-field"><label for="housing">Tipo de vivienda</label><select id="housing" v-model="form.housing" name="housing" required><option value="">Selecciona una opción</option><option value="casa">Casa</option><option value="departamento">Departamento</option><option value="otro">Otro</option></select></div>
          <div class="form-field"><label for="outdoorSpace">¿Cuenta con espacio exterior?</label><select id="outdoorSpace" v-model="form.outdoorSpace" name="outdoorSpace" required><option value="">Selecciona una opción</option><option value="si">Sí</option><option value="no">No</option></select></div>
          <div class="form-field"><label for="people">Personas en el hogar</label><input id="people" v-model="form.people" name="people" type="number" min="1" required placeholder="Número de personas"></div>
          <div class="form-field"><label for="permission">¿Todos están de acuerdo?</label><select id="permission" v-model="form.permission" name="permission" required><option value="">Selecciona una opción</option><option value="si">Sí</option><option value="no">No</option></select></div>
        </div></section>
        <section class="form-section"><div class="form-section-heading"><div><h2>¿Hay otros compañeros en casa?</h2><p>Esta información ayuda a evaluar la adaptación de la mascota.</p></div></div><div class="form-grid">
          <div class="form-field"><label for="otherPets">¿Tienes otras mascotas?</label><select id="otherPets" v-model="form.otherPets" name="otherPets" required><option value="">Selecciona una opción</option><option value="si">Sí</option><option value="no">No</option></select></div>
          <div v-if="form.otherPets === 'si'" class="form-field"><label for="otherPetsType">¿Qué animales tienes?</label><input id="otherPetsType" v-model="form.otherPetsType" name="otherPetsType" placeholder="Perros, gatos, etc." :required="form.otherPets === 'si'"></div>
          <div v-if="form.otherPets === 'si'" class="form-field form-field-full"><label for="otherPetsDetails">Cuéntanos un poco más</label><textarea id="otherPetsDetails" v-model="form.otherPetsDetails" name="otherPetsDetails" placeholder="Describe brevemente a tus mascotas actuales."></textarea></div>
        </div></section>
        <section class="form-section"><div class="form-section-heading"><div><h2>¿Por qué quieres adoptar?</h2><p>Comparte tus motivos y expectativas sobre la adopción.</p></div></div><div class="form-grid"><div class="form-field form-field-full"><label for="reason">Motivo de adopción</label><textarea id="reason" v-model="form.reason" name="reason" required :placeholder="`Cuéntanos por qué quieres adoptar a ${pet.name}.`"></textarea></div></div></section>
        <section class="form-section"><div class="form-section-heading"><div><h2>Tu experiencia</h2></div></div><div class="form-grid"><div class="form-field form-field-full"><label for="experience">Experiencia previa con mascotas</label><textarea id="experience" v-model="form.experience" name="experience" placeholder="Cuéntanos brevemente sobre tu experiencia."></textarea></div></div></section>
        <section class="form-section"><div class="form-section-heading"><div><h2>Documentos de apoyo</h2><p>Si el refugio los solicita, puedes adjuntar comprobantes o documentos de identificación.</p></div></div><div class="form-grid"><div class="form-field form-field-full"><label for="application-documents">Archivos (opcional)</label><input id="application-documents" type="file" accept="application/pdf,image/jpeg,image/png,image/webp" multiple @change="onDocumentFiles"><small>Hasta 5 archivos de máximo 10 MB cada uno. Se almacenan de forma privada.</small><p v-if="documentNotice" class="form-field-notice" role="status">{{ documentNotice }}</p></div></div></section>
        <section class="form-section form-section-confirm"><div class="form-section-heading"><div><h2>Antes de enviar</h2></div></div><label class="checkbox-field"><input v-model="accepted" type="checkbox" name="responsibleAdoption" required><span>Confirmo que la información proporcionada es verdadera y que comprendo que la adopción implica un compromiso de cuidado responsable.</span></label><p v-if="error" class="auth-feedback is-error" role="alert">{{ error }}</p><p v-if="!accepted && !submitting" class="application-submit-hint">Marca la casilla de arriba para poder enviar la solicitud.</p><button type="submit" class="application-submit login-button" :disabled="!accepted || submitting">{{ submitting ? 'Enviando…' : 'Enviar solicitud' }}</button></section>
      </form>
    </div><aside class="application-sidebar"><article v-if="petLoading" class="application-pet-card" role="status" aria-label="Cargando mascota"><div class="catalog-loading-card" aria-hidden="true"></div></article><article v-else class="application-pet-card"><ImageWithSkeleton frame-class="application-pet-image" :src="petImage" :alt="`Imagen de ${pet.name}`" role="img" /><div class="application-pet-content"><span class="pet-species">{{ pet.species.toLocaleUpperCase('es-MX') }}</span><h2>{{ pet.name }}</h2><p>{{ pet.location }}</p><div class="application-health"><div>Vacunación: <span>{{ pet.vaccination || 'Pendiente de confirmar' }}</span></div><div>Desparasitación: <span>{{ pet.deworming || 'Pendiente de confirmar' }}</span></div><div>Esterilización: <span>{{ pet.sterilization || 'Pendiente de confirmar' }}</span></div></div></div></article><div class="application-process"><div class="application-process-heading"><span class="eyebrow">PROCESO</span><h2>¿Qué sucede después?</h2></div><div v-for="step in [{title:'Revisamos tu solicitud',text:'La información será evaluada por la organización responsable.'},{title:'Contacto',text:'La organización podrá comunicarse contigo para continuar el proceso.'},{title:'Seguimiento',text:'Podrás consultar el estado de tu solicitud desde PetHop.'}]" :key="step.title" class="application-process-step"><span><Icon name="check" /></span><div><strong>{{ step.title }}</strong><p>{{ step.text }}</p></div></div></div></aside></div></section>
  </main><SiteFooter /></div>
</template>
