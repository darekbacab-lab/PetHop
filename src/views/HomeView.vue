<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { listPets, listFavorites, addFavorite, removeFavorite } from '../services/petApi.js';
import { sessionStorageService } from '../services/storage.js';

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
const petLoadError = ref('');
async function refreshPets() {
  try {
    const result = await listPets({ sort: 'newest' });
    pets.value = result.content.slice(0, 6); petLoadError.value = '';
  } catch (error) { pets.value = []; petLoadError.value = error.message || 'No se pudieron cargar las mascotas.'; }
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
watch(visiblePetCount, () => {
  currentPetPage.value = 0;
  petTransitioning.value = false;
  pendingPetMoves.length = 0;
});
watch(canPagePets, canPage => {
  if (canPage) startPetAutoplay();
  else stopPetAutoplay();
});
const favorites = ref([]);
const search = ref('');
const showOrganizationForm = ref(false);
const registrationError = ref('');
const registration = ref({ organization: '', contact: '', email: '', phone: '', description: '' });
const petImage = pet => pet.photos?.[0] || '';
const processSteps = [
  { title: 'Explora', text: 'Consulta mascotas disponibles y utiliza filtros para encontrar opciones que se adapten a tus preferencias.', mobileText: 'Encuentra mascotas que buscan un hogar.', art: '/img/onboarding-explora.png' },
  { title: 'Conoce', text: 'Revisa información de la mascota, características, ubicación y datos sanitarios disponibles.', mobileText: 'Descubre su historia, salud y personalidad.', art: '/img/onboarding-conoce.png' },
  { title: 'Solicita', text: 'Completa un formulario estructurado para iniciar el proceso de adopción.', mobileText: 'Inicia una adopción responsable, paso a paso.', art: '/img/onboarding-solicita.png' },
  { title: 'Acompaña', text: 'Continúa registrando información y seguimiento después de la adopción.', mobileText: 'Sigue de cerca su adaptación en familia.', art: '/img/onboarding-acompana.png' },
  { title: 'Cambia vidas', text: 'Con una adopción responsable, ayudas a una mascota a encontrar un hogar y construyes una nueva historia juntos.', mobileText: 'Cada adopción abre una nueva historia.', art: '/img/onboarding-cambia-vidas.png' }
];
const processTrack = ref(null);
const processPosition = ref(1);
const processIsDragging = ref(false);
const processIsRecentering = ref(false);
const processCarouselSlides = computed(() => {
  const slides = processSteps.map((step, index) => ({ ...step, index, clone: false, key: `process-${index}` }));
  return [
    { ...slides[slides.length - 1], clone: true, key: 'process-clone-last' },
    ...slides,
    { ...slides[0], clone: true, key: 'process-clone-first' }
  ];
});
const processPositionLabel = computed(() => {
  const count = processSteps.length;
  const current = processPosition.value === 0 ? count : processPosition.value === count + 1 ? 1 : processPosition.value;
  return `${String(current).padStart(2, '0')} / ${String(count).padStart(2, '0')}`;
});
let processAutoTimer;
let processScrollTimer;
let processScrollFrame;
let processPauseUntil = 0;
let processNavigationTarget = 1;
let processDrag = null;
let processMotionPreference;
function centerProcessSlide(position, behavior = 'smooth') {
  const track = processTrack.value;
  const slide = track?.children[position];
  if (!track || !slide) return;
  const trackRect = track.getBoundingClientRect();
  const slideRect = slide.getBoundingClientRect();
  const left = track.scrollLeft + slideRect.left - trackRect.left - (track.clientWidth - slideRect.width) / 2;
  if (behavior === 'auto') {
    track.style.scrollBehavior = 'auto';
    track.style.scrollSnapType = 'none';
    requestAnimationFrame(() => {
      track.style.scrollBehavior = '';
      track.style.scrollSnapType = '';
    });
  }
  track.scrollTo({ left, behavior });
}
function updateProcessPosition() {
  const track = processTrack.value;
  if (!track) return;
  const center = track.getBoundingClientRect().left + track.clientWidth / 2;
  let closestPosition = processPosition.value;
  let closestDistance = Infinity;
  [...track.children].forEach((slide, index) => {
    const rect = slide.getBoundingClientRect();
    const distance = Math.abs(rect.left + rect.width / 2 - center);
    if (distance < closestDistance) { closestDistance = distance; closestPosition = index; }
  });
  processPosition.value = closestPosition;
}
function recenterProcessSlide(position) {
  processIsRecentering.value = true;
  processPosition.value = position;
  processNavigationTarget = position;
  centerProcessSlide(position, 'auto');
  requestAnimationFrame(() => { processIsRecentering.value = false; });
}
function settleProcessCarousel() {
  if (processIsDragging.value) return;
  if (processPosition.value === 0) recenterProcessSlide(processSteps.length);
  else if (processPosition.value === processSteps.length + 1) recenterProcessSlide(1);
  else processNavigationTarget = processPosition.value;
}
function onProcessScroll() {
  if (processScrollFrame) return;
  processScrollFrame = requestAnimationFrame(() => {
    processScrollFrame = undefined;
    updateProcessPosition();
    window.clearTimeout(processScrollTimer);
    processScrollTimer = window.setTimeout(settleProcessCarousel, 180);
  });
}
function noteProcessActivity() { processPauseUntil = Date.now() + 8000; }
function moveProcessCarousel(direction, userInitiated = true) {
  if (userInitiated) noteProcessActivity();
  let current = processNavigationTarget;
  if (current === 0) { recenterProcessSlide(processSteps.length); current = processSteps.length; }
  if (current === processSteps.length + 1) { recenterProcessSlide(1); current = 1; }
  processNavigationTarget = current + direction;
  centerProcessSlide(processNavigationTarget, processMotionPreference?.matches ? 'auto' : 'smooth');
}
function onProcessKeydown(event) {
  if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
    event.preventDefault();
    moveProcessCarousel(event.key === 'ArrowLeft' ? -1 : 1);
  }
}
function startProcessPointer(event) {
  noteProcessActivity();
  if (event.pointerType !== 'mouse' || event.button !== 0) return;
  const track = processTrack.value;
  processDrag = { pointerId: event.pointerId, startX: event.clientX, startScrollLeft: track.scrollLeft };
  processIsDragging.value = true;
  track.setPointerCapture(event.pointerId);
}
function moveProcessPointer(event) {
  if (!processDrag || processDrag.pointerId !== event.pointerId) return;
  const delta = event.clientX - processDrag.startX;
  if (Math.abs(delta) > 3 && event.cancelable) event.preventDefault();
  processTrack.value.scrollLeft = processDrag.startScrollLeft - delta;
}
function endProcessPointer(event) {
  if (!processDrag || processDrag.pointerId !== event.pointerId) return;
  if (processTrack.value?.hasPointerCapture(event.pointerId)) processTrack.value.releasePointerCapture(event.pointerId);
  processDrag = null;
  processIsDragging.value = false;
  window.clearTimeout(processScrollTimer);
  processScrollTimer = window.setTimeout(settleProcessCarousel, 180);
}
function processAutoplayTick() {
  if (document.hidden || processMotionPreference?.matches || Date.now() < processPauseUntil) return;
  moveProcessCarousel(1, false);
}
onMounted(async () => {
  petMotionPreference = window.matchMedia('(prefers-reduced-motion: reduce)');
  processMotionPreference = petMotionPreference;
  petMotionPreference.addEventListener('change', handleMotionPreference);
  await Promise.all([refreshPets(), listFavorites().then(items => favorites.value = items.map(item => String(item.id).toLowerCase())).catch(() => favorites.value = [])]);
  updateVisiblePetCount();
  startPetAutoplay();
  requestAnimationFrame(() => centerProcessSlide(1, 'auto'));
  processAutoTimer = window.setInterval(processAutoplayTick, 5500);
  window.addEventListener('resize', updateVisiblePetCount, { passive: true });
});
onUnmounted(() => {
  stopPetAutoplay();
  window.clearInterval(processAutoTimer);
  window.clearTimeout(processScrollTimer);
  if (processScrollFrame) cancelAnimationFrame(processScrollFrame);
  petMotionPreference?.removeEventListener('change', handleMotionPreference);
  window.removeEventListener('resize', updateVisiblePetCount);
});
function submitSearch() { router.push({ path: '/explorar.html', query: { search: search.value.trim() } }); }
async function favorite(id) {
  try {
    const key = String(id).toLowerCase();
    if (favorites.value.includes(key)) { await removeFavorite(id); favorites.value = favorites.value.filter(item => item !== key); }
    else { await addFavorite(id); favorites.value = [...favorites.value, key]; }
  } catch (error) { if (error.status === 401) router.push({ path: '/acceso.html', query: { redirect: '/' } }); }
}
function registerOrganization() {
  const saved = sessionStorageService.write('pethop_shelter_registration_draft', {
    organizationName: registration.value.organization.trim(), fullName: registration.value.contact.trim(),
    email: registration.value.email.trim(), phone: registration.value.phone.trim(),
    description: registration.value.description.trim()
  });
  if (!saved) { registrationError.value = 'No se pudo conservar el formulario para continuar. Revisa el almacenamiento de la sesión.'; return; }
  router.push({ path: '/acceso.html', query: { mode: 'register', role: 'provider' } });
}
</script>

<template>
  <div><SiteHeader />
    <main>
      <section class="hero"><div class="hero-container"><div class="hero-copy"><span class="eyebrow">ADOPCIÓN RESPONSABLE</span><h1>Encuentra al próximo <span>miembro de tu familia.</span></h1><p>Conecta con mascotas que buscan un hogar y conoce información importante sobre su historia, salud y proceso de adopción antes de dar el siguiente paso.</p><div class="hero-actions"><RouterLink to="/explorar.html" class="primary-button">Explorar mascotas</RouterLink><RouterLink to="/#como-funciona" class="secondary-button">Conoce cómo funciona</RouterLink></div></div><div class="hero-visual"><ImageWithSkeleton frame-class="hero-image-placeholder" image-class="hero-pets-image" src="/img/mascotas-hero-v2.png" alt="Un perro y un gato descansan juntos sobre una manta" role="img" /><div class="hero-floating-card"><div class="floating-check" aria-hidden="true"><Icon name="check" /></div><div><strong>Adopción responsable</strong><span>Información organizada</span></div></div></div></div></section>
      <section class="quick-actions"><div class="actions-container"><RouterLink to="/explorar.html" class="action-card"><div class="action-content"><span>ADOPTANTES</span><h2>Adoptar</h2><p>Encuentra mascotas disponibles y conoce sus características antes de solicitar una adopción.</p><strong>Explorar mascotas</strong></div><span class="action-paw-trail" aria-hidden="true"><Icon name="paw-print" class="action-paw action-paw-one" /><Icon name="paw-print" class="action-paw action-paw-two" /><Icon name="paw-print" class="action-paw action-paw-four" /><Icon name="paw-print" class="action-paw action-paw-five" /><Icon name="paw-print" class="action-paw action-paw-six" /><Icon name="paw-print" class="action-paw action-paw-seven" /></span></RouterLink><a href="#registrar" class="action-card" @click.prevent="showOrganizationForm=true"><div class="action-content"><span>ORGANIZACIONES</span><h2>Registrar refugio</h2><p>Publica mascotas, organiza solicitudes y centraliza la información de tus procesos de adopción.</p><strong>Conocer más</strong></div><span class="action-paw-trail" aria-hidden="true"><Icon name="paw-print" class="action-paw action-paw-one" /><Icon name="paw-print" class="action-paw action-paw-two" /><Icon name="paw-print" class="action-paw action-paw-four" /><Icon name="paw-print" class="action-paw action-paw-five" /><Icon name="paw-print" class="action-paw action-paw-six" /><Icon name="paw-print" class="action-paw action-paw-seven" /></span></a><RouterLink to="/seguimiento.html" class="action-card"><div class="action-content"><span>SEGUIMIENTO</span><h2>Acompañar</h2><p>Mantén organizada la información posterior a la adopción y registra cómo continúa la historia.</p><strong>Ver seguimiento</strong></div><span class="action-paw-trail" aria-hidden="true"><Icon name="paw-print" class="action-paw action-paw-one" /><Icon name="paw-print" class="action-paw action-paw-two" /><Icon name="paw-print" class="action-paw action-paw-four" /><Icon name="paw-print" class="action-paw action-paw-five" /><Icon name="paw-print" class="action-paw action-paw-six" /><Icon name="paw-print" class="action-paw action-paw-seven" /></span></RouterLink></div></section>
      <section class="explore-section" id="explorar"><div class="explore-container"><div class="section-title"><div><span>CONOCE A LOS DISPONIBLES</span><h2>Tal vez tu nuevo compañero está aquí.</h2></div></div><div class="home-search"><form class="search-box" @submit.prevent="submitSearch"><div class="search-input"><span class="search-symbol" aria-hidden="true"><Icon name="search" /></span><label class="sr-only" for="homeSearch">Buscar mascota</label><input id="homeSearch" v-model="search" name="search" type="search" placeholder="Busca por nombre, especie o ubicación" autocomplete="off"></div><button type="submit" class="search-button">Buscar</button></form></div>
        <div class="home-pets-carousel" id="featuredPets" aria-label="Mascotas destacadas" @mouseenter="petCarouselHovered = true" @mouseleave="petCarouselHovered = false" @focusin="petCarouselFocused = true" @focusout="handleCarouselFocusOut">
          <p v-if="petLoadError" class="catalog-empty" role="alert">{{ petLoadError }}</p><p v-else-if="!pets.length" class="catalog-empty" role="status">Aún no hay mascotas publicadas. Vuelve pronto para conocerlas.</p>
          <button v-if="canPagePets" class="home-pets-arrow home-pets-arrow-previous" type="button" aria-label="Ver mascotas anteriores" @click="movePetPage(-1)"><Icon name="arrow-left" /></button>
          <div class="home-pets-viewport" aria-live="polite" aria-atomic="true">
            <Transition :name="petTransitionName" @after-enter="finishPetPageMove">
              <section v-if="petPages[currentPetPage]" :key="currentPetPage" class="home-pets-page" :style="{ '--home-pet-count': visiblePetCount }" role="group" :aria-label="`Grupo ${currentPetPage + 1} de ${petPages.length}`">
                <article v-for="pet in petPages[currentPetPage]" :key="pet.id" class="pet-card"><ImageWithSkeleton frame-class="pet-image" :src="petImage(pet)" :alt="`Imagen de ${pet.name}`"><RouterLink class="pet-image-link" :to="{ path: '/mascota.html', query: { pet: pet.id } }" :aria-label="`Ver información de ${pet.name}`"><span class="sr-only">Ver información de {{ pet.name }}</span></RouterLink><button class="favorite" :class="{ active: favorites.includes(String(pet.id).toLowerCase()) }" type="button" :aria-pressed="favorites.includes(String(pet.id).toLowerCase())" :aria-label="favorites.includes(String(pet.id).toLowerCase()) ? `Quitar ${pet.name} de favoritos` : `Agregar ${pet.name} a favoritos`" @click="favorite(pet.id)"><Icon name="heart" :filled="favorites.includes(String(pet.id).toLowerCase())" /></button></ImageWithSkeleton><RouterLink class="pet-info" :to="{ path: '/mascota.html', query: { pet: pet.id } }"><div class="pet-title"><div><span class="pet-species">{{ pet.species.toLocaleUpperCase('es-MX') }}</span><h3>{{ pet.name }}</h3></div><span class="available">Disponible</span></div><div class="pet-details"><span>{{ pet.age }}</span><span>{{ pet.gender }}</span><span>{{ pet.size }}</span></div><div class="pet-location">{{ pet.location }}</div></RouterLink></article>
              </section>
            </Transition>
          </div>
          <button v-if="canPagePets" class="home-pets-arrow home-pets-arrow-next" type="button" aria-label="Ver más mascotas" @click="movePetPage(1)"><Icon name="arrow-right" /></button>
        </div>
        <RouterLink to="/explorar.html" class="load-more ui-button">Explorar todas las mascotas</RouterLink></div></section>
      <section class="process-section" id="como-funciona">
        <div class="process-container">
          <div class="process-intro">
            <span>CÓMO FUNCIONA</span><h2>Adoptar puede ser más claro.</h2>
            <p>PetHop organiza el proceso para que puedas conocer la información de la mascota, enviar tu solicitud y continuar con el seguimiento.</p>
          </div>
          <div class="process-carousel" role="region" aria-roledescription="carrusel" aria-label="Etapas del proceso de adopción">
            <div
              ref="processTrack"
              class="process-steps process-carousel-track"
              :class="{ 'is-dragging': processIsDragging, 'is-recentering': processIsRecentering }"
              tabindex="0"
              aria-label="Tarjetas de las etapas. Usa las flechas izquierda y derecha para navegar."
              @scroll="onProcessScroll"
              @wheel="noteProcessActivity"
              @pointerdown="startProcessPointer"
              @pointermove="moveProcessPointer"
              @pointerup="endProcessPointer"
              @pointercancel="endProcessPointer"
              @keydown="onProcessKeydown"
            >
              <div
                v-for="(slide, slidePosition) in processCarouselSlides"
                :key="slide.key"
                class="process-slide"
                :class="{ 'is-active': processPosition === slidePosition }"
                :aria-hidden="slide.clone ? 'true' : undefined"
                :data-slide-index="slidePosition"
                role="group"
                aria-roledescription="diapositiva"
                :aria-label="`Etapa ${slide.index + 1} de ${processSteps.length}: ${slide.title}`"
              >
                <article class="process-step" :class="`process-step-${slide.index + 1}`">
                  <span class="process-step-number">0{{ slide.index + 1 }}</span>
                  <div class="process-step-copy">
                    <h3>{{ slide.title }}</h3>
                    <p><span class="process-step-copy-full">{{ slide.text }}</span><span class="process-step-copy-mobile">{{ slide.mobileText }}</span></p>
                  </div>
                  <span class="process-step-art" aria-hidden="true"><img :src="slide.art" alt="" loading="lazy" decoding="async" draggable="false"></span>
                </article>
              </div>
            </div>
            <div class="process-carousel-controls">
              <button type="button" aria-label="Ver etapa anterior" @click="moveProcessCarousel(-1)"><Icon name="arrow-left" /></button>
              <span aria-live="polite" aria-atomic="true">{{ processPositionLabel }}</span>
              <button type="button" aria-label="Ver etapa siguiente" @click="moveProcessCarousel(1)"><Icon name="arrow-right" /></button>
            </div>
          </div>
        </div>
      </section>
      <section class="organization-section" id="registrar"><div class="organization-container"><div><span>PARA REFUGIOS Y RESCATISTAS</span><h2>Una forma más organizada de gestionar adopciones.</h2><p>Centraliza la información de tus mascotas, solicitudes y seguimientos en un mismo espacio.</p></div><button type="button" class="organization-button" @click="showOrganizationForm = true">Registrar refugio</button></div></section>
    </main><SiteFooter />
    <div v-if="showOrganizationForm" class="dynamic-modal open organization-registration-overlay" role="presentation" @click.self="showOrganizationForm = false"><section class="dynamic-modal-content organization-registration-modal" role="dialog" aria-modal="true" aria-labelledby="organization-title" @keydown.esc="showOrganizationForm=false"><button type="button" class="dynamic-modal-close" aria-label="Cerrar" @click="showOrganizationForm = false"><Icon name="x" /></button><div class="organization-modal-heading"><span class="organization-modal-icon"><Icon name="building-2" :size="22" /></span><div><span class="organization-modal-eyebrow">ESPACIO PARA REFUGIOS</span><h2 id="organization-title">Registrar refugio</h2><p>Completa los datos básicos; después crearás una cuenta para acceder al panel.</p></div></div><form id="organizationForm" @submit.prevent="registerOrganization"><div class="organization-form-grid"><label for="organizationName">Nombre de la organización<input id="organizationName" v-model="registration.organization" name="organization" type="text" placeholder="Ej. Refugio Patitas" autocomplete="organization" required></label><label for="organizationContact">Persona de contacto<input id="organizationContact" v-model="registration.contact" name="contact" type="text" placeholder="Nombre completo" autocomplete="name" required></label><label for="organizationEmail">Correo electrónico<input id="organizationEmail" v-model="registration.email" name="email" type="email" placeholder="nombre@organizacion.org" autocomplete="email" required></label><label for="organizationPhone">Teléfono<input id="organizationPhone" v-model="registration.phone" name="phone" type="tel" placeholder="Número de contacto" autocomplete="tel" required></label><label class="organization-description-field" for="organizationDescription">Acerca de la organización<textarea id="organizationDescription" v-model="registration.description" name="description" placeholder="Cuéntanos brevemente a quiénes ayudan y cómo trabajan." required></textarea></label></div><p v-if="registrationError" class="auth-feedback is-error" role="alert">{{ registrationError }}</p><button class="organization-button organization-submit" type="submit"><Icon name="arrow-right" :size="18" /> Continuar al registro de cuenta</button></form></section></div>
  </div>
</template>

<style scoped>
.organization-registration-modal { width:min(620px,100%); max-height:min(780px,calc(100dvh - 40px)); padding:34px; border:1px solid #b9e2e5; border-radius:24px; }
.organization-registration-modal .dynamic-modal-close { z-index:1; width:38px; height:38px; }
.organization-modal-heading { display:flex; align-items:flex-start; gap:15px; padding-right:34px; }
.organization-modal-icon { display:grid; width:48px; height:48px; flex:0 0 48px; place-items:center; border-radius:15px; background:#e5f6f5; color:var(--primary-strong); }
.organization-modal-eyebrow { color:var(--primary-strong); font-size:.68rem; font-weight:850; letter-spacing:.12em; }
.organization-modal-heading h2 { margin:5px 0 5px; font:800 1.65rem/1.2 "Poppins",sans-serif; letter-spacing:-.035em; }
.organization-modal-heading p { margin:0; color:var(--text-soft); font-size:.82rem; line-height:1.55; }
.organization-review-hint { display:flex; align-items:center; gap:10px; margin:21px 0 18px; padding:12px 14px; border:1px solid #c9e8dc; border-radius:13px; background:#effaf5; color:#3e6258; font-size:.76rem; line-height:1.45; }
.organization-review-hint :deep(svg) { flex:0 0 auto; color:var(--primary-strong); }
.organization-form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
.organization-form-grid > label { display:flex; min-width:0; flex-direction:column; gap:7px; color:var(--text); font-size:.74rem; font-weight:750; }
.organization-registration-modal .organization-form-grid input,.organization-registration-modal .organization-form-grid textarea { width:100%; min-height:45px; padding:11px 13px; border:1px solid #c4e1e4; border-radius:11px; background:#f5fcfc; color:var(--text); font:500 .79rem "Montserrat",sans-serif; transition:border-color .18s ease,box-shadow .18s ease,background .18s ease; }
.organization-registration-modal .organization-form-grid textarea { min-height:105px; resize:vertical; }
.organization-registration-modal .organization-form-grid input:focus,.organization-registration-modal .organization-form-grid textarea:focus { border-color:var(--primary-strong); outline:0; background:#fff; box-shadow:0 0 0 3px rgba(22,127,135,.1); }
.organization-description-field { grid-column:1/-1; }
.organization-registration-modal #organizationForm > .organization-submit { display:flex; width:100%; min-height:50px; align-items:center; justify-content:center; gap:9px; margin-top:5px; }
@media(max-width:560px) {
  .organization-registration-modal { max-height:calc(100dvh - 24px); padding:27px 20px 20px; border-radius:19px; }
  .organization-modal-heading { gap:11px; padding-right:29px; }
  .organization-modal-icon { width:42px; height:42px; flex-basis:42px; border-radius:13px; }
  .organization-modal-heading h2 { font-size:1.35rem; }
  .organization-form-grid { grid-template-columns:1fr; gap:12px; }
  .organization-description-field { grid-column:auto; }
}
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

.hero-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 194px));
  gap: 12px;
}

.hero-actions > .primary-button,
.hero-actions > .secondary-button {
  width: 100%;
  min-height: 52px;
  padding: 12px 14px;
  font-size: .82rem;
  text-align: center;
}

.quick-actions .action-card {
  min-width: 0;
  text-decoration: none;
}

.quick-actions .action-content strong {
  gap: 8px;
  margin-top: auto;
  text-decoration: underline;
  text-decoration-color: transparent;
  text-underline-offset: 4px;
  transition: gap .2s ease, text-decoration-color .2s ease;
}

.quick-actions .action-content strong::after {
  content: "→";
  font-size: 1rem;
  line-height: 1;
  transition: transform .2s ease;
}

.quick-actions .action-card:hover .action-content strong,
.quick-actions .action-card:focus-visible .action-content strong {
  gap: 10px;
  text-decoration-color: currentColor;
}

.quick-actions .action-card:hover .action-content strong::after,
.quick-actions .action-card:focus-visible .action-content strong::after {
  transform: translateX(2px);
}

.quick-actions .action-card:focus-visible {
  z-index: 1;
  outline: 3px solid var(--primary-light);
  outline-offset: 3px;
}

.quick-actions .action-card:active {
  transform: translateY(-1px) scale(.995);
  box-shadow: 0 8px 18px rgba(18, 63, 75, .16);
}

.process-step-art,
.process-step-copy-mobile { display:none; }

.process-carousel { position:relative; width:min(1120px, 100%); margin-inline:auto; }
.process-carousel-track {
  display:flex;
  align-items:stretch;
  gap:18px;
  width:100%;
  overflow-x:auto;
  overflow-y:hidden;
  padding:22px max(calc((100% - min(680px, 74vw)) / 2), 14px) 30px;
  scrollbar-width:none;
  scroll-snap-type:x mandatory;
  scroll-behavior:smooth;
  overscroll-behavior-x:contain;
  touch-action:pan-y;
  cursor:grab;
}
.process-carousel-track::-webkit-scrollbar { display:none; }
.process-carousel-track.is-dragging { cursor:grabbing; scroll-snap-type:none; user-select:none; }
.process-carousel-track.is-recentering { scroll-behavior:auto; scroll-snap-type:none; }
.process-carousel-track:focus-visible { outline:3px solid #3eb2ba; outline-offset:3px; border-radius:18px; }
.process-slide { flex:0 0 min(430px, 34vw); scroll-snap-align:center; scroll-snap-stop:always; opacity:.58; transform:scale(.94); transition:opacity .38s ease,transform .38s ease; }
.process-slide.is-active { opacity:1; transform:scale(1); }
.process-steps .process-step {
  position:relative;
  display:grid;
  aspect-ratio:4 / 5;
  min-height:0;
  grid-template-columns:minmax(0,1fr);
  grid-template-rows:auto minmax(0,1fr) auto;
  grid-template-areas:"number" "art" "copy";
  align-content:center;
  align-items:center;
  gap:10px;
  overflow:hidden;
  padding:clamp(18px, 2.4vw, 30px);
  border:1px solid transparent;
  border-radius:28px;
}
.process-steps .process-step-1 { border-color:#c5e5e4; background:#e8f7f5; }
.process-steps .process-step-2 { border-color:#36999e; background:#3a9fa5; }
.process-steps .process-step-3 { border-color:#c8e8ea; background:#e7f5f7; }
.process-steps .process-step-4 { border-color:#367d82; background:#347d82; }
.process-steps .process-step-5 { border-color:#14545c; background:#14545c; }
.process-step-number { grid-area:number; align-self:end; margin:0; color:#167f87; font-size:.75rem; font-weight:850; letter-spacing:.1em; }
.process-step-2 .process-step-number,
.process-step-4 .process-step-number,
.process-step-5 .process-step-number { color:#d5f4f1; }
.process-step-copy { position:relative; z-index:2; grid-area:copy; align-self:start; min-width:0; }
.process-steps .process-step h3 { margin:0 0 8px; color:#14545c; font-size:clamp(1.65rem,2.2vw,2.1rem); line-height:1.02; letter-spacing:-.045em; }
.process-step-2 h3,.process-step-4 h3,.process-step-5 h3 { color:#fff!important; }
.process-steps .process-step p { max-width:390px; margin:0; color:#315f64; font-size:.94rem; line-height:1.55; }
.process-step-2 p,.process-step-4 p,.process-step-5 p { color:#e8f7f6!important; }
.process-step-art { position:relative; z-index:1; display:grid; width:min(100%, 260px); height:100%; min-height:0; max-height:200px; grid-area:art; place-items:center; align-self:center; justify-self:center; overflow:visible; background:transparent; }
.process-step-art img { display:block; width:100%; height:100%; object-fit:contain; user-select:none; }
.process-step-5::after { position:absolute; top:8%; right:9%; width:42%; aspect-ratio:1; border:1px solid rgba(208,244,240,.24); border-radius:50%; content:""; pointer-events:none; }
.process-carousel-controls { display:flex; align-items:center; justify-content:center; gap:16px; margin-top:4px; }
.process-carousel-controls button { display:grid; width:42px; height:42px; place-items:center; border:1px solid #b9dfe1; border-radius:50%; background:#f5fbfa; color:#14545c; cursor:pointer; transition:background .18s ease,border-color .18s ease,transform .18s ease; }
.process-carousel-controls button:hover { border-color:#3a9fa5; background:#e4f4f2; transform:translateY(-1px); }
.process-carousel-controls button:focus-visible { outline:3px solid #3eb2ba; outline-offset:3px; }
.process-carousel-controls button svg { width:18px; height:18px; }
.process-carousel-controls > span { min-width:70px; color:#14545c; font-size:.76rem; font-weight:800; letter-spacing:.08em; text-align:center; }

@media (max-width: 900px) {
  .quick-actions .action-card {
    display: flex;
    border-radius: 16px;
  }

  .quick-actions .action-content {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: flex-start;
  }

  .quick-actions .action-content p {
    min-height: 0;
  }

  .quick-actions .action-content strong {
    margin-top: auto;
  }
}

@media (min-width: 701px) and (max-width: 900px) {
  .quick-actions .actions-container {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    align-items: stretch;
    gap: 12px;
  }

  .quick-actions .action-card {
    min-height: 180px;
    padding: clamp(14px, 2vw, 18px);
  }

  .quick-actions .action-content > span {
    margin-bottom: 5px;
    font-size: .64rem;
  }

  .quick-actions .action-content h2 {
    margin-bottom: 8px;
    font-size: clamp(1.05rem, 2.2vw, 1.35rem);
    line-height: 1.12;
  }

  .quick-actions .action-content p {
    margin-bottom: 14px;
    font-size: clamp(.69rem, 1.1vw, .76rem);
    line-height: 1.5;
  }
}

@media (max-width: 700px) {
  .quick-actions .actions-container {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: stretch;
    gap: 10px;
  }

  .quick-actions .action-card:first-child {
    grid-column: 1 / -1;
    min-height: 112px;
    padding: 14px 16px;
    border: 1px solid #c5e8e9;
    background: #effafb;
    color: #14545c;
  }

  .quick-actions .action-card:first-child .action-content > * {
    color: #14545c;
  }

  .quick-actions .action-card .action-content p { display: none; }

  .quick-actions .action-card:first-child .action-content > span {
    margin-bottom: 4px;
    font-size: .62rem;
    letter-spacing: .07em;
  }

  .quick-actions .action-card:first-child .action-content h2 {
    margin-bottom: 7px;
    font-size: 1.38rem;
    line-height: 1.1;
  }

  .quick-actions .action-card:first-child .action-paw-trail .action-paw { display: none; }
  .quick-actions .action-card:first-child .action-paw-trail .action-paw-four {
    top: 12%;
    right: 14px;
    left: auto;
    display: block;
    width: 19px;
    height: 19px;
    color: #16818a;
    opacity: .82;
    transform: none;
  }

  .quick-actions .action-card:not(:first-child) {
    min-height: 116px;
    padding: 12px 13px;
    border-radius: 14px;
  }

  .quick-actions .action-card:nth-child(2) {
    background: #238b92;
  }

  .quick-actions .action-card:nth-child(3) {
    background: #24767d;
  }

  .quick-actions .action-card:not(:first-child) .action-content > span {
    margin-bottom: 4px;
    font-size: .58rem;
    letter-spacing: .055em;
  }

  .quick-actions .action-card:not(:first-child) .action-content h2 {
    margin-bottom: 5px;
    font-size: .98rem;
    line-height: 1.12;
    letter-spacing: -.025em;
  }

  .quick-actions .action-card:not(:first-child) .action-content strong {
    font-size: .64rem;
  }
}

@media (max-width: 430px) {
  .quick-actions .actions-container {
    width: calc(100% - 32px);
    gap: 9px;
  }

  .quick-actions .action-card:first-child {
    min-height: 108px;
    padding: 13px 14px;
  }

  .quick-actions .action-card:not(:first-child) {
    min-height: 112px;
    padding: 11px 10px;
  }

  .quick-actions .action-card:not(:first-child) .action-content h2 {
    font-size: clamp(.82rem, 3.7vw, .98rem);
  }

  .quick-actions .action-card:not(:first-child) .action-content strong {
    font-size: .61rem;
  }
}

@media (max-width: 900px) {
  .process-carousel { width:100%; }
  .process-carousel-track { gap:14px; padding-inline:7%; }
  .process-slide { flex-basis:min(430px, 82%); }
  .process-steps .process-step { padding:22px; }
  .process-step-art { max-height:250px; }
}

@media (max-width: 700px) {
  .process-section { padding:52px 0 56px; }
  .process-intro { margin-bottom:16px; }
  .process-intro > span { margin-bottom:7px; font-size:.64rem; }
  .process-intro h2 { max-width:360px; margin-bottom:8px; font-size:clamp(1.85rem,7.3vw,2.35rem); line-height:1.02; }
  .process-intro p { max-width:520px; font-size:.82rem; line-height:1.5; }
  .process-carousel-track { gap:10px; padding:12px 6% 20px; }
  .process-slide { flex-basis:90%; }
  .process-steps .process-step { grid-template-columns:minmax(0,1fr); grid-template-rows:auto minmax(0,1fr) auto; grid-template-areas:"number" "art" "copy"; gap:7px; padding:15px; border-radius:21px; }
  .process-step-number { font-size:.65rem; }
  .process-steps .process-step h3 { margin-bottom:5px; font-size:1.42rem; }
  .process-steps .process-step p { font-size:.72rem; line-height:1.4; }
  .process-step-copy-full { display:none; }
  .process-step-copy-mobile { display:inline; }
  .process-step-art { width:min(100%, 230px); height:min(100%, 210px); max-height:210px; }
  .process-step-5::after { top:8%; right:10%; width:38%; }
  .process-carousel-controls { gap:12px; margin-top:0; }
  .process-carousel-controls button { width:38px; height:38px; }
}

@media (max-width: 380px) {
  .process-section { padding:44px 0 48px; }
  .process-carousel-track { gap:8px; padding-inline:5.5%; }
  .process-slide { flex-basis:90%; }
  .process-steps .process-step { padding:12px; border-radius:19px; }
  .process-steps .process-step h3 { font-size:1.2rem; }
  .process-steps .process-step p { font-size:.69rem; }
  .process-step-art { width:min(100%, 205px); height:min(100%, 185px); max-height:185px; }
}

@media (min-width: 901px) {
  .process-carousel-track { padding-inline:14px; }
  .process-slide { flex-basis:calc((100% - 28px) / 3); opacity:1; transform:none; }
  .process-slide.is-active { opacity:1; transform:none; }
  .process-steps .process-step { aspect-ratio:3 / 4; gap:8px; padding:clamp(18px, 2vw, 24px); }
  .process-step-art { width:min(100%, 230px); max-height:220px; transform:translateY(-12px); }
  .process-steps .process-step p { font-size:.84rem; line-height:1.4; }
}

@media (prefers-reduced-motion: reduce) {
  .process-carousel-track { scroll-behavior:auto; }
  .process-slide { transition:none; }
}

@media (max-width: 700px) {
  .home-pets-carousel { margin-inline: 34px; }
  .home-pets-page { gap: 0; }
  .home-pets-arrow-previous { left: -34px; }
  .home-pets-arrow-next { right: -34px; }
  .hero-actions { grid-template-columns: minmax(0, 1fr); }

  .hero-actions > .primary-button,
  .hero-actions > .secondary-button {
    width: 100%;
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
