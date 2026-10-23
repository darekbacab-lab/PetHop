<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import { getFavorites, getPets, toggleFavorite } from '../services/pets.js';
import { storageService } from '../services/storage.js';

const router = useRouter();
const pets = ref([]);
const currentPetPage = ref(0);
const visiblePetCount = ref(3);
const petTransitionName = ref('home-pets-slide-next');
const petTransitioning = ref(false);
const petCarouselHovered = ref(false);
const petCarouselFocused = ref(false);
const pendingPetMoves = [];
let petAutoplayTimer;
let petMotionPreference;
const publishedDateFields = ['publishedAt', 'publicationDate', 'createdAt', 'created_at', 'dateCreated', 'fechaPublicacion', 'fechaCreacion'];
function publicationTime(pet) {
  for (const field of publishedDateFields) {
    const value = pet?.[field];
    if (value === undefined || value === null || value === '') continue;
    const time = value instanceof Date ? value.getTime() : typeof value === 'number' ? value : Date.parse(value);
    if (Number.isFinite(time)) return time;
  }
  return null;
}
function refreshPets() {
  const directory = getPets();
  const byId = new Map(directory.map(pet => [String(pet.id).trim().toLowerCase(), pet]));
  const published = globalThis.PetHopData?.publishedPets(storageService) || [];
  const publishedIds = published.map(pet => String(pet.id).trim().toLowerCase());
  const directoryIds = directory.map(pet => String(pet.id).trim().toLowerCase());
  const orderedIds = [...new Set([...publishedIds, ...directoryIds])];
  const ordered = orderedIds.map(id => byId.get(id)).filter(Boolean);
  pets.value = ordered
    .map((pet, index) => ({ pet, index, time: publicationTime(pet) }))
    .sort((a, b) => {
      if (a.time !== null && b.time !== null) return b.time - a.time || a.index - b.index;
      if (a.time !== null) return -1;
      if (b.time !== null) return 1;
      return a.index - b.index;
    })
    .slice(0, 6)
    .map(entry => entry.pet);
  currentPetPage.value = 0;
  petTransitioning.value = false;
  pendingPetMoves.length = 0;
}
function updateVisiblePetCount() {
  const width = window.innerWidth;
  visiblePetCount.value = width <= 700 ? 1 : width <= 1000 ? 2 : 3;
}
const petPages = computed(() => {
  const pages = [];
  for (let index = 0; index < pets.value.length; index += visiblePetCount.value) {
    pages.push(pets.value.slice(index, index + visiblePetCount.value));
  }
  return pages;
});
const canPagePets = computed(() => petPages.value.length > 1);
function movePetPage(direction, motion = 'manual') {
  const count = petPages.value.length;
  if (count < 2) return;
  if (petTransitioning.value) {
    pendingPetMoves.push({ direction, motion });
    return;
  }
  startPetPageMove(direction, motion);
}
function startPetPageMove(direction, motion = 'manual') {
  const count = petPages.value.length;
  if (count < 2) return;
  petTransitionName.value = motion === 'auto'
    ? 'home-pets-slide-auto'
    : direction > 0 ? 'home-pets-slide-next' : 'home-pets-slide-previous';
  petTransitioning.value = true;
  currentPetPage.value = (currentPetPage.value + direction + count) % count;
}
function finishPetPageMove() {
  if (!petTransitioning.value) return;
  petTransitioning.value = false;
  const nextMove = pendingPetMoves.shift();
  if (nextMove) startPetPageMove(nextMove.direction, nextMove.motion);
}
function stopPetAutoplay() {
  if (petAutoplayTimer) {
    window.clearInterval(petAutoplayTimer);
    petAutoplayTimer = undefined;
  }
}
function startPetAutoplay() {
  if (petAutoplayTimer || petMotionPreference?.matches || !canPagePets.value) return;
  petAutoplayTimer = window.setInterval(() => {
    if (document.hidden || petCarouselHovered.value || petCarouselFocused.value || petTransitioning.value) return;
    movePetPage(1, 'auto');
  }, 3000);
}
function handleMotionPreference(event) {
  if (event.matches) stopPetAutoplay();
  else startPetAutoplay();
}
function handleCarouselFocusOut(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) petCarouselFocused.value = false;
}
function refreshPublishedPets(event) {
  if (!event || event.key === 'pethop_provider_workspace') refreshPets();
}
watch(visiblePetCount, () => {
  currentPetPage.value = 0;
  petTransitioning.value = false;
  pendingPetMoves.length = 0;
});
watch(canPagePets, canPage => {
  if (canPage) startPetAutoplay();
  else stopPetAutoplay();
});
const favorites = ref(getFavorites());
const search = ref('');
const showOrganizationForm = ref(false);
const registrationError = ref('');
const registration = ref({ organization: '', contact: '', email: '', phone: '', description: '' });
const petImage = pet => pet.photos?.[0] || `/img/${pet.image || 'img2.jpeg'}`;
const processSteps = [
  { title: 'Explora', text: 'Consulta mascotas disponibles y utiliza filtros para encontrar opciones que se adapten a tus preferencias.' },
  { title: 'Conoce', text: 'Revisa información de la mascota, características, ubicación y datos sanitarios disponibles.' },
  { title: 'Solicita', text: 'Completa un formulario estructurado para iniciar el proceso de adopción.' },
  { title: 'Acompaña', text: 'Continúa registrando información y seguimiento después de la adopción.' },
  { title: 'Cambia vidas', text: 'Con una adopción responsable, ayudas a una mascota a encontrar un hogar y construyes una nueva historia juntos.' }
];
onMounted(() => {
  petMotionPreference = window.matchMedia('(prefers-reduced-motion: reduce)');
  petMotionPreference.addEventListener('change', handleMotionPreference);
  refreshPets();
  updateVisiblePetCount();
  startPetAutoplay();
  window.addEventListener('resize', updateVisiblePetCount, { passive: true });
  window.addEventListener('storage', refreshPublishedPets);
});
onUnmounted(() => {
  stopPetAutoplay();
  petMotionPreference?.removeEventListener('change', handleMotionPreference);
  window.removeEventListener('resize', updateVisiblePetCount);
  window.removeEventListener('storage', refreshPublishedPets);
});
function submitSearch() { router.push({ path: '/explorar.html', query: { search: search.value.trim() } }); }
function favorite(id) { toggleFavorite(id); favorites.value = getFavorites(); }
function registerOrganization() {
  registrationError.value = '';
  const state = storageService.read('pethop_provider_workspace', {}) || {};
  state.role = 'organization'; state.section = 'Inicio';
  state.profile = { ...(state.profile || {}), name: registration.value.organization.trim(), contact: registration.value.contact.trim(), email: registration.value.email.trim(), phone: registration.value.phone.trim(), description: registration.value.description.trim() };
  if (!storageService.write('pethop_provider_workspace', state)) { registrationError.value = 'No se pudo guardar la organización en este dispositivo.'; return; }
  showOrganizationForm.value = false;
  router.push('/proveedor.html');
}
</script>

<template>
  <div><SiteHeader />
    <main>
      <section class="hero"><div class="hero-container"><div class="hero-copy"><span class="eyebrow">ADOPCIÓN RESPONSABLE</span><h1>Encuentra al próximo <span>miembro de tu familia.</span></h1><p>Conecta con mascotas que buscan un hogar y conoce información importante sobre su historia, salud y proceso de adopción antes de dar el siguiente paso.</p><div class="hero-actions"><RouterLink to="/explorar.html" class="primary-button">Explorar mascotas</RouterLink><RouterLink to="/#como-funciona" class="secondary-button">Conoce cómo funciona</RouterLink></div></div><div class="hero-visual"><div class="hero-image-placeholder" aria-label="Perro y gato disponibles para adopción"><img class="hero-pets-image" src="/img/mascotas-hero-v2.png" alt="Un perro y un gato descansan juntos sobre una manta"></div><div class="hero-floating-card"><div class="floating-check" aria-hidden="true"><Icon name="check" /></div><div><strong>Adopción responsable</strong><span>Información organizada</span></div></div></div></div></section>
      <section class="quick-actions"><div class="actions-container"><RouterLink to="/explorar.html" class="action-card"><div class="action-content"><span>ADOPTANTES</span><h2>Adoptar</h2><p>Encuentra mascotas disponibles y conoce sus características antes de solicitar una adopción.</p><strong>Explorar mascotas</strong></div><span class="action-paw-trail" aria-hidden="true"><Icon name="paw-print" class="action-paw action-paw-one" /><Icon name="paw-print" class="action-paw action-paw-two" /><Icon name="paw-print" class="action-paw action-paw-four" /><Icon name="paw-print" class="action-paw action-paw-five" /><Icon name="paw-print" class="action-paw action-paw-six" /><Icon name="paw-print" class="action-paw action-paw-seven" /></span></RouterLink><a href="#registrar" class="action-card" @click.prevent="showOrganizationForm=true"><div class="action-content"><span>ORGANIZACIONES</span><h2>Registrar refugio</h2><p>Publica mascotas, organiza solicitudes y centraliza la información de tus procesos de adopción.</p><strong>Conocer más</strong></div><span class="action-paw-trail" aria-hidden="true"><Icon name="paw-print" class="action-paw action-paw-one" /><Icon name="paw-print" class="action-paw action-paw-two" /><Icon name="paw-print" class="action-paw action-paw-four" /><Icon name="paw-print" class="action-paw action-paw-five" /><Icon name="paw-print" class="action-paw action-paw-six" /><Icon name="paw-print" class="action-paw action-paw-seven" /></span></a><RouterLink to="/seguimiento.html" class="action-card"><div class="action-content"><span>SEGUIMIENTO</span><h2>Acompañar</h2><p>Mantén organizada la información posterior a la adopción y registra cómo continúa la historia.</p><strong>Ver seguimiento</strong></div><span class="action-paw-trail" aria-hidden="true"><Icon name="paw-print" class="action-paw action-paw-one" /><Icon name="paw-print" class="action-paw action-paw-two" /><Icon name="paw-print" class="action-paw action-paw-four" /><Icon name="paw-print" class="action-paw action-paw-five" /><Icon name="paw-print" class="action-paw action-paw-six" /><Icon name="paw-print" class="action-paw action-paw-seven" /></span></RouterLink></div></section>
      <section class="explore-section" id="explorar"><div class="explore-container"><div class="section-title"><div><span>CONOCE A LOS DISPONIBLES</span><h2>Tal vez tu nuevo compañero está aquí.</h2></div></div><div class="home-search"><form class="search-box" @submit.prevent="submitSearch"><div class="search-input"><span class="search-symbol" aria-hidden="true"><Icon name="search" /></span><label class="sr-only" for="homeSearch">Buscar mascota</label><input id="homeSearch" v-model="search" name="search" type="search" placeholder="Busca por nombre, especie o ubicación" autocomplete="off"></div><button type="submit" class="search-button">Buscar</button></form></div>
        <div class="home-pets-carousel" id="featuredPets" aria-label="Mascotas destacadas" @mouseenter="petCarouselHovered = true" @mouseleave="petCarouselHovered = false" @focusin="petCarouselFocused = true" @focusout="handleCarouselFocusOut">
          <button v-if="canPagePets" class="home-pets-arrow home-pets-arrow-previous" type="button" aria-label="Ver mascotas anteriores" @click="movePetPage(-1)"><Icon name="arrow-left" /></button>
          <div class="home-pets-viewport" aria-live="polite" aria-atomic="true">
            <Transition :name="petTransitionName" @after-enter="finishPetPageMove">
              <section v-if="petPages[currentPetPage]" :key="currentPetPage" class="home-pets-page" :style="{ '--home-pet-count': visiblePetCount }" role="group" :aria-label="`Grupo ${currentPetPage + 1} de ${petPages.length}`">
                <article v-for="pet in petPages[currentPetPage]" :key="pet.id" class="pet-card"><div class="pet-image" :style="{ backgroundImage: `url('${petImage(pet)}')` }"><span class="sr-only">Imagen de {{ pet.name }}</span><RouterLink class="pet-image-link" :to="{ path: '/mascota.html', query: { pet: pet.id } }" :aria-label="`Ver información de ${pet.name}`"><span class="sr-only">Ver información de {{ pet.name }}</span></RouterLink><button class="favorite" :class="{ active: favorites.includes(String(pet.id).toLowerCase()) }" type="button" :aria-pressed="favorites.includes(String(pet.id).toLowerCase())" :aria-label="favorites.includes(String(pet.id).toLowerCase()) ? `Quitar ${pet.name} de favoritos` : `Agregar ${pet.name} a favoritos`" @click="favorite(pet.id)"><Icon name="heart" :filled="favorites.includes(String(pet.id).toLowerCase())" /></button></div><RouterLink class="pet-info" :to="{ path: '/mascota.html', query: { pet: pet.id } }"><div class="pet-title"><div><span class="pet-species">{{ pet.species.toLocaleUpperCase('es-MX') }}</span><h3>{{ pet.name }}</h3></div><span class="available">Disponible</span></div><div class="pet-details"><span>{{ pet.age }}</span><span>{{ pet.gender }}</span><span>{{ pet.size }}</span></div><div class="pet-location">{{ pet.location }}</div></RouterLink></article>
              </section>
            </Transition>
          </div>
          <button v-if="canPagePets" class="home-pets-arrow home-pets-arrow-next" type="button" aria-label="Ver más mascotas" @click="movePetPage(1)"><Icon name="arrow-right" /></button>
        </div>
        <RouterLink to="/explorar.html" class="load-more ui-button">Explorar todas las mascotas</RouterLink></div></section>
      <section class="process-section" id="como-funciona"><div class="process-container"><div class="process-intro"><span>CÓMO FUNCIONA</span><h2>Adoptar puede ser más claro.</h2><p>PetHop organiza el proceso para que puedas conocer la información de la mascota, enviar tu solicitud y continuar con el seguimiento.</p></div><div class="process-steps"><div v-for="(step,index) in processSteps" :key="step.title" class="process-step"><span>0{{ index+1 }}</span><div><h3>{{ step.title }}</h3><p>{{ step.text }}</p></div></div></div></div></section>
      <section class="organization-section" id="registrar"><div class="organization-container"><div><span>PARA REFUGIOS Y RESCATISTAS</span><h2>Una forma más organizada de gestionar adopciones.</h2><p>Centraliza la información de tus mascotas, solicitudes y seguimientos en un mismo espacio.</p></div><button type="button" class="organization-button" @click="showOrganizationForm = true">Registrar refugio</button></div></section>
    </main><SiteFooter />
    <div v-if="showOrganizationForm" class="dynamic-modal open" role="presentation" @click.self="showOrganizationForm = false"><section class="dynamic-modal-content" role="dialog" aria-modal="true" aria-labelledby="organization-title" @keydown.esc="showOrganizationForm=false"><button type="button" class="dynamic-modal-close" aria-label="Cerrar" @click="showOrganizationForm = false"><Icon name="x" /></button><h2 id="organization-title">Registrar refugio</h2><p>Registra la información básica de tu organización para integrarla al prototipo de PetHop.</p><form id="organizationForm" @submit.prevent="registerOrganization"><label class="sr-only" for="organizationName">Nombre de la organización</label><input id="organizationName" v-model="registration.organization" name="organization" type="text" placeholder="Nombre de la organización" required><label class="sr-only" for="organizationContact">Persona de contacto</label><input id="organizationContact" v-model="registration.contact" name="contact" type="text" placeholder="Persona de contacto" required><label class="sr-only" for="organizationEmail">Correo electrónico</label><input id="organizationEmail" v-model="registration.email" name="email" type="email" placeholder="Correo electrónico" required><label class="sr-only" for="organizationPhone">Teléfono</label><input id="organizationPhone" v-model="registration.phone" name="phone" type="tel" placeholder="Teléfono" required><label class="sr-only" for="organizationDescription">Descripción de la organización</label><textarea id="organizationDescription" v-model="registration.description" name="description" placeholder="Describe brevemente la organización" required></textarea><p v-if="registrationError" class="auth-feedback is-error" role="alert">{{ registrationError }}</p><button class="organization-button organization-submit" type="submit">Enviar registro</button></form></section></div>
  </div>
</template>

<style scoped>
.home-pets-carousel {
  position: relative;
  margin-inline: 42px;
}

.home-pets-viewport {
  display: grid;
  overflow: hidden;
  border-radius: 18px;
}

.home-pets-page {
  display: grid;
  width: 100%;
  grid-area: 1 / 1;
  grid-template-columns: repeat(var(--home-pet-count), minmax(0, 1fr));
  gap: 18px;
  min-width: 0;
}

.home-pets-slide-next-enter-active,
.home-pets-slide-next-leave-active,
.home-pets-slide-previous-enter-active,
.home-pets-slide-previous-leave-active {
  transition: transform 420ms cubic-bezier(.22, .68, 0, 1);
  will-change: transform;
}

.home-pets-slide-next-enter-from { transform: translateX(100%); }
.home-pets-slide-next-leave-to { transform: translateX(-100%); }
.home-pets-slide-previous-enter-from { transform: translateX(-100%); }
.home-pets-slide-previous-leave-to { transform: translateX(100%); }

.home-pets-slide-auto-enter-active,
.home-pets-slide-auto-leave-active {
  transition: transform 1120ms cubic-bezier(.22, .72, .2, 1);
  will-change: transform;
}

.home-pets-slide-auto-enter-from { transform: translateX(100%); }
.home-pets-slide-auto-leave-to { transform: translateX(-100%); }

.home-pets-arrow {
  position: absolute;
  z-index: 2;
  top: 50%;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: var(--white);
  color: var(--primary-dark);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transform: translateY(-50%);
}

.home-pets-arrow:hover { background: var(--primary-soft); }
.home-pets-arrow:focus-visible { outline: 3px solid var(--primary); outline-offset: 3px; }
.home-pets-arrow :deep(svg) { width: 19px; height: 19px; }
.home-pets-arrow-previous { left: -42px; }
.home-pets-arrow-next { right: -42px; }

.hero-actions > .primary-button {
  min-height: 44px;
  padding: 10px 16px;
  font-size: .82rem;
}

@media (max-width: 700px) {
  .home-pets-carousel { margin-inline: 34px; }
  .home-pets-page { gap: 0; }
  .home-pets-arrow-previous { left: -34px; }
  .home-pets-arrow-next { right: -34px; }

  .hero-actions > .primary-button {
    min-height: 52px;
    padding: 12px 16px;
    font-size: .9rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-pets-slide-next-enter-active,
  .home-pets-slide-next-leave-active,
  .home-pets-slide-previous-enter-active,
  .home-pets-slide-previous-leave-active,
  .home-pets-slide-auto-enter-active,
  .home-pets-slide-auto-leave-active { transition-duration: 0.01ms; }
}
</style>
