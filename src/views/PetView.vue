<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import '../../js/pethop-data.js';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { getFavorites, getPet, toggleFavorite } from '../services/pets.js';
import { storageService } from '../services/storage.js';
const route = useRoute(); const router = useRouter();
const pet = computed(() => getPet(route.query.pet || 'chino') || getPet('chino'));
watch(() => pet.value.name, name => { document.title = `${name} | PetHop`; }, { immediate: true });
const favorites = ref(getFavorites()); const activeTrackIndex = ref(1); const transitionEnabled = ref(true); const isTransitioning = ref(false);
const pendingDirections = [];
const photos = computed(() => pet.value.photos?.length ? pet.value.photos : (String(pet.value.species).toLowerCase() === 'gato' ? ['img5.jpg','img6.jpg'] : ['img2.jpeg','img3.jpeg','img4.jpeg','img7.jpeg']));
const carouselSlides = computed(() => photos.value.length > 1
  ? [{ photo: photos.value.at(-1), sourceIndex: photos.value.length - 1, clone: true, key: 'clone-last' }, ...photos.value.map((photo, sourceIndex) => ({ photo, sourceIndex, clone: false, key: `photo-${sourceIndex}` })), { photo: photos.value[0], sourceIndex: 0, clone: true, key: 'clone-first' }]
  : photos.value.map((photo, sourceIndex) => ({ photo, sourceIndex, clone: false, key: `photo-${sourceIndex}` })));
const centerTrackIndex = computed(() => photos.value.length > 1 ? activeTrackIndex.value : 0);
const galleryTransform = computed(() => {
  const trackIndex = photos.value.length > 1 ? activeTrackIndex.value : 0;
  return `translateX(calc(13% - ${trackIndex * 74}% - ${trackIndex * 12}px))`;
});
watch(() => pet.value.id, () => { activeTrackIndex.value = 1; transitionEnabled.value = true; isTransitioning.value = false; pendingDirections.length = 0; });
const user = computed(() => storageService.read('pethop_user', {}) || {});
const threadId = computed(() => `${String(user.value.email || 'visitante').trim().toLowerCase()}:${pet.value.id}`);
const photoSrc = item => item.startsWith('data:') ? item : `/img/${item}`;
const touchStartX = ref(0);
function nextPhoto(direction) {
  if (photos.value.length < 2) return;
  if (isTransitioning.value) { pendingDirections.push(direction); return; }
  activeTrackIndex.value += direction;
  isTransitioning.value = true;
}
function continueQueuedNavigation() {
  isTransitioning.value = false;
  if (pendingDirections.length) nextPhoto(pendingDirections.shift());
}
async function finishGalleryTransition(event) {
  if (event.target !== event.currentTarget || event.propertyName !== 'transform') return;
  if (activeTrackIndex.value !== 0 && activeTrackIndex.value !== photos.value.length + 1) {
    continueQueuedNavigation();
    return;
  }
  transitionEnabled.value = false;
  activeTrackIndex.value = activeTrackIndex.value === 0 ? photos.value.length : 1;
  await nextTick();
  requestAnimationFrame(() => { transitionEnabled.value = true; continueQueuedNavigation(); });
}
function startSwipe(event) { touchStartX.value = event.changedTouches[0]?.clientX || 0; }
function endSwipe(event) { const distance = touchStartX.value - (event.changedTouches[0]?.clientX || 0); if (Math.abs(distance) >= 45) nextPhoto(distance > 0 ? 1 : -1); }
function handleGalleryKey(event) { if (event.key === 'ArrowRight') nextPhoto(1); if (event.key === 'ArrowLeft') nextPhoto(-1); }
function favorite() { toggleFavorite(pet.value.id); favorites.value = getFavorites(); }
function askShelter() {
  const owner = String(user.value.email || 'visitante').trim().toLowerCase();
  const existing = globalThis.PetHopData.messages(storageService).find(thread => thread.id === threadId.value);
  if (!existing) {
    const thread = { id: threadId.value, petId: pet.value.id, petName: pet.value.name, owner, adopter: user.value.name || user.value.fullName || user.value.nombre || owner, messages: [] };
    if (!globalThis.PetHopData.upsertMessageThread(storageService, thread)) return;
  }
  router.push({ path: '/mensajes.html', query: { pet: pet.value.id } });
}
</script>
<template>
  <div><SiteHeader /><main><section class="pet-profile" :data-pet-id="pet.id" :data-description="pet.description">
    <div class="pet-profile-container"><RouterLink to="/explorar.html" class="back-link"><Icon name="arrow-left" /> Volver a explorar</RouterLink><div class="pet-profile-grid">
        <div class="pet-gallery-column"><div class="pet-profile-heading"><h1>{{ pet.name }}</h1></div>
        <div class="pet-gallery pet-profile-photo" :aria-label="`Galería de imágenes de referencia de ${pet.name}`" tabindex="0" @keydown="handleGalleryKey"><div class="gallery-viewport" role="region" aria-roledescription="carrusel" :aria-label="`Imágenes de referencia de ${pet.name}`" @touchstart.passive="startSwipe" @touchend.passive="endSwipe"><div class="gallery-track" :class="{ 'gallery-track-no-transition': !transitionEnabled, 'gallery-track-moving': isTransitioning }" :style="{ transform: galleryTransform }" @transitionend="finishGalleryTransition"><div v-for="(slide, index) in carouselSlides" :key="slide.key" class="gallery-card" :class="{ 'gallery-card-center': index === centerTrackIndex, 'gallery-card-adjacent': Math.abs(index - centerTrackIndex) === 1 }" role="group" aria-roledescription="diapositiva" :aria-label="`Imagen ${slide.sourceIndex + 1} de ${photos.length}`" :aria-hidden="index !== centerTrackIndex"><ImageWithSkeleton frame-class="gallery-slide-frame" image-class="gallery-slide-image" :src="photoSrc(slide.photo)" :alt="pet.photos?.length ? `Fotografía ${slide.sourceIndex + 1} de ${pet.name}.` : `Imagen de referencia ${slide.sourceIndex + 1} de ${pet.name}. Confirma con el refugio si corresponde a esta mascota.`" /></div></div></div><button v-if="photos.length > 1" class="gallery-arrow gallery-prev" type="button" aria-label="Ver imagen anterior" @click="nextPhoto(-1)"><Icon name="chevron-right" class="gallery-arrow-icon-left" :stroke-width="2.2" /></button><button v-if="photos.length > 1" class="gallery-arrow gallery-next" type="button" aria-label="Ver imagen siguiente" @click="nextPhoto(1)"><Icon name="chevron-right" :stroke-width="2.2" /></button><button class="gallery-favorite" :class="{ active: favorites.includes(pet.id) }" type="button" :aria-pressed="favorites.includes(pet.id)" :aria-label="favorites.includes(pet.id) ? `Quitar ${pet.name} de favoritos` : `Agregar ${pet.name} a favoritos`" @click="favorite"><Icon name="heart" :filled="favorites.includes(pet.id)" /></button></div>
        <p class="gallery-caption">Imágenes de referencia. Confirma con el refugio cuáles corresponden a {{ pet.name }}.</p><section class="pet-story" :aria-label="`Descripción de ${pet.name}`"><h2 class="pet-story-title">Detalles extra de {{ pet.name }}</h2><p>{{ pet.description || `El refugio aún no ha compartido una descripción sobre ${pet.name}. Puedes preguntarle cómo convive, qué actividades disfruta y qué tipo de hogar se adapta mejor a ${pet.name}.` }}</p></section>
      </div>
      <div class="pet-profile-info"><div class="pet-information"><span class="profile-eyebrow">INFORMACIÓN</span><h2>Datos de {{ pet.name }}</h2><div class="information-grid"><div v-for="[label, value] in [['Ubicación', pet.location ? `${pet.location}, Quintana Roo` : 'Ubicación por confirmar'], ['Especie', pet.species], ['Edad', pet.age], ['Sexo', pet.gender], ['Tamaño', pet.size], ['Vacunación', pet.vaccination || 'Pendiente de confirmar'], ['Desparasitación', pet.deworming || 'Pendiente de confirmar'], ['Esterilización', pet.sterilization || 'Pendiente de confirmar']]" :key="label" class="information-item"><div class="information-icon" aria-hidden="true"><Icon v-if="label === 'Ubicación'" name="map-pin" /><Icon v-else-if="label === 'Especie'" :name="pet.species.toLowerCase() === 'gato' ? 'cat' : pet.species.toLowerCase() === 'perro' ? 'dog' : 'paw-print'" /><span v-else>{{ ({ Edad: 'A', Sexo: pet.gender?.[0] || 'S', Tamaño: 'T', Vacunación: 'V', Desparasitación: 'D', Esterilización: 'E' })[label] }}</span></div><div><strong>{{ label }}</strong><span>{{ value || 'Por confirmar' }}</span></div></div></div></div>
        <div class="adoption-notice"><h2>Adopción responsable</h2><p>Conoce las características y la información de salud de {{ pet.name }} antes de enviar tu solicitud. El refugio puede resolver cualquier dato pendiente.</p></div><button type="button" class="message-shelter-button" @click="askShelter">Preguntar al refugio</button><RouterLink class="pet-apply-button login-button" :to="{ path: '/solicitud.html', query: { pet: pet.id } }">Solicitar adopción</RouterLink></div>
    </div></div>
  </section></main><SiteFooter />
  </div>
</template>
