<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import { getPet } from '../services/pets.js';
import { storageService } from '../services/storage.js';

const route = useRoute(); const router = useRouter();
const pet = computed(() => getPet(route.query.pet || 'chino') || getPet('chino'));
const accepted = ref(false); const error = ref('');
const petImage = computed(() => pet.value?.photos?.[0] || `/img/${pet.value?.image || 'img2.jpeg'}`);
const form = reactive({ fullName: '', email: '', phone: '', age: '', housing: '', outdoorSpace: '', people: '', permission: '', otherPets: '', otherPetsType: '', otherPetsDetails: '', reason: '', experience: '' });
function submit(event) {
  if (!event.currentTarget.reportValidity()) return;
  const applications = storageService.read('pethop_applications', []);
  const records = Array.isArray(applications) ? applications.filter(item => item && typeof item === 'object') : [];
  const application = { id: `APP-${Date.now().toString(36).toUpperCase()}`, petId: String(pet.value.id), status: 'Recibida', date: new Date().toISOString(), data: { ...form, responsibleAdoption: 'on' } };
  records.push(application);
  if (!storageService.write('pethop_applications', records)) { error.value = 'No se pudo guardar la solicitud en este dispositivo.'; return; }
  storageService.write('pethop_user', { name: form.fullName, email: form.email, phone: form.phone });
  router.push({ path: '/seguimiento.html', query: { application: application.id } });
}
</script>

<template>
  <div class="application-page"><SiteHeader /><main>
    <section class="application-header"><div class="application-header-container"><h1>Da el siguiente paso.</h1><p>Completa la información necesaria para iniciar el proceso de adopción de {{ pet.name }}.</p></div></section>
    <section class="application-section"><div class="application-container"><div class="application-main">
      <form class="application-form" @submit.prevent="submit">
        <section class="form-section"><div class="form-section-heading"><div><span class="eyebrow">INFORMACIÓN PERSONAL</span><h2>Cuéntanos sobre ti.</h2><p>Estos datos permitirán identificar a la persona que realiza la solicitud.</p></div></div><div class="form-grid">
          <div class="form-field"><label for="fullName">Nombre completo</label><input id="fullName" v-model="form.fullName" name="fullName" required placeholder="Escribe tu nombre"></div>
          <div class="form-field"><label for="email">Correo electrónico</label><input id="email" v-model="form.email" name="email" type="email" required placeholder="correo@ejemplo.com"></div>
          <div class="form-field"><label for="phone">Teléfono</label><input id="phone" v-model="form.phone" name="phone" type="tel" required placeholder="Número de contacto"></div>
          <div class="form-field"><label for="age">Edad</label><input id="age" v-model="form.age" name="age" type="number" min="18" required placeholder="18"></div>
        </div></section>
        <section class="form-section"><div class="form-section-heading"><div><span class="eyebrow">HOGAR</span><h2>Cuéntanos sobre tu hogar.</h2><p>Queremos conocer las condiciones en las que viviría la mascota.</p></div></div><div class="form-grid">
          <div class="form-field"><label for="housing">Tipo de vivienda</label><select id="housing" v-model="form.housing" name="housing" required><option value="">Selecciona una opción</option><option value="casa">Casa</option><option value="departamento">Departamento</option><option value="otro">Otro</option></select></div>
          <div class="form-field"><label for="outdoorSpace">¿Cuenta con espacio exterior?</label><select id="outdoorSpace" v-model="form.outdoorSpace" name="outdoorSpace" required><option value="">Selecciona una opción</option><option value="si">Sí</option><option value="no">No</option></select></div>
          <div class="form-field"><label for="people">Personas en el hogar</label><input id="people" v-model="form.people" name="people" type="number" min="1" required placeholder="Número de personas"></div>
          <div class="form-field"><label for="permission">¿Todos están de acuerdo?</label><select id="permission" v-model="form.permission" name="permission" required><option value="">Selecciona una opción</option><option value="si">Sí</option><option value="no">No</option></select></div>
        </div></section>
        <section class="form-section"><div class="form-section-heading"><div><span class="eyebrow">OTROS ANIMALES</span><h2>¿Hay otros compañeros en casa?</h2><p>Esta información ayuda a evaluar la adaptación de la mascota.</p></div></div><div class="form-grid">
          <div class="form-field"><label for="otherPets">¿Tienes otras mascotas?</label><select id="otherPets" v-model="form.otherPets" name="otherPets" required><option value="">Selecciona una opción</option><option value="si">Sí</option><option value="no">No</option></select></div>
          <div class="form-field"><label for="otherPetsType">¿Qué animales tienes?</label><input id="otherPetsType" v-model="form.otherPetsType" name="otherPetsType" placeholder="Perros, gatos, etc."></div>
          <div class="form-field form-field-full"><label for="otherPetsDetails">Cuéntanos un poco más</label><textarea id="otherPetsDetails" v-model="form.otherPetsDetails" name="otherPetsDetails" placeholder="Describe brevemente a tus mascotas actuales."></textarea></div>
        </div></section>
        <section class="form-section"><div class="form-section-heading"><div><span class="eyebrow">MOTIVO</span><h2>¿Por qué quieres adoptar?</h2><p>Comparte tus motivos y expectativas sobre la adopción.</p></div></div><div class="form-grid"><div class="form-field form-field-full"><label for="reason">Motivo de adopción</label><textarea id="reason" v-model="form.reason" name="reason" required :placeholder="`Cuéntanos por qué quieres adoptar a ${pet.name}.`"></textarea></div></div></section>
        <section class="form-section"><div class="form-section-heading"><div><span class="eyebrow">EXPERIENCIA</span><h2>Tu experiencia</h2></div></div><div class="form-grid"><div class="form-field form-field-full"><label for="experience">Experiencia previa con mascotas</label><textarea id="experience" v-model="form.experience" name="experience" placeholder="Cuéntanos brevemente sobre tu experiencia."></textarea></div></div></section>
        <article class="application-pet-card"><div class="application-pet-image" :style="{ backgroundImage: `url('${petImage}')` }"><span class="sr-only">Imagen de {{ pet.name }}</span></div><div class="application-pet-content"><span class="pet-species">{{ pet.species.toLocaleUpperCase('es-MX') }}</span><h2>{{ pet.name }}</h2><p>{{ pet.location }}</p><div class="application-health"><div>Vacunación: <span>{{ pet.vaccination || 'Pendiente de confirmar' }}</span></div><div>Desparasitación: <span>{{ pet.deworming || 'Pendiente de confirmar' }}</span></div><div>Esterilización: <span>{{ pet.sterilization || 'Pendiente de confirmar' }}</span></div></div></div></article>
        <section class="form-section form-section-confirm"><div class="form-section-heading"><div><span class="eyebrow">RESPONSABILIDAD</span><h2>Antes de enviar</h2></div></div><label class="checkbox-field"><input v-model="accepted" type="checkbox" name="responsibleAdoption" required><span>Confirmo que la información proporcionada es verdadera y que comprendo que la adopción implica un compromiso de cuidado responsable.</span></label><p v-if="error" class="auth-feedback is-error" role="alert">{{ error }}</p><button type="submit" class="application-submit login-button" :disabled="!accepted">Enviar solicitud</button></section>
      </form>
    </div><aside class="application-sidebar"><div class="application-process"><div class="application-process-heading"><span class="eyebrow">PROCESO</span><h2>¿Qué sucede después?</h2></div><div v-for="step in [{title:'Revisamos tu solicitud',text:'La información será evaluada por la organización responsable.'},{title:'Contacto',text:'La organización podrá comunicarse contigo para continuar el proceso.'},{title:'Seguimiento',text:'Podrás consultar el estado de tu solicitud desde PetHop.'}]" :key="step.title" class="application-process-step"><span><Icon name="check" /></span><div><strong>{{ step.title }}</strong><p>{{ step.text }}</p></div></div></div></aside></div></section>
  </main><SiteFooter /></div>
</template>
