<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import '../../js/pethop-data.js';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { getPetById, listFavorites, addFavorite, removeFavorite } from '../services/petApi.js';
import { api } from '../services/api.js';
const route = useRoute(); const router = useRouter();
const pet = ref({ id: '', name: 'Mascota', species: 'Mascota', description: '' });
const loadError = ref('');
const favorites = ref([]); const activeTrackIndex = ref(1); const transitionEnabled = ref(true); const isTransitioning = ref(false);
const pendingDirections = [];
const photos = computed(() => pet.value.photos?.length ? pet.value.photos : ['/img/pet-gallery.png']);
const carouselSlides = computed(() => photos.value.length > 1
  ? [{ photo: photos.value.at(-1), sourceIndex: photos.value.length - 1, clone: true, key: 'clone-last' }, ...photos.value.map((photo, sourceIndex) => ({ photo, sourceIndex, clone: false, key: `photo-${sourceIndex}` })), { photo: photos.value[0], sourceIndex: 0, clone: true, key: 'clone-first' }]
  : photos.value.map((photo, sourceIndex) => ({ photo, sourceIndex, clone: false, key: `photo-${sourceIndex}` })));
const centerTrackIndex = computed(() => photos.value.length > 1 ? activeTrackIndex.value : 0);
const galleryTransform = computed(() => {
  const trackIndex = photos.value.length > 1 ? activeTrackIndex.value : 0;
  return `translateX(calc(13% - ${trackIndex * 74}% - ${trackIndex * 12}px))`;
});
watch(() => pet.value.id, () => { activeTrackIndex.value = 1; transitionEnabled.value = true; isTransitioning.value = false; pendingDirections.length = 0; });
const photoSrc = item => item.startsWith('data:') || item.startsWith('http') || item.startsWith('/') ? item : `/img/${item}`;
async function loadPet() {
  if (!route.query.pet) { loadError.value = 'Selecciona una mascota desde el catálogo.'; return; }
  try { pet.value = await getPetById(route.query.pet); document.title = `${pet.value.name} | PetHop`; loadError.value = ''; }
  catch (error) { loadError.value = error.message || 'No se pudo cargar esta mascota.'; }
}
onMounted(async () => {
  await loadPet();
  try { favorites.value = (await listFavorites()).map(item => String(item.id)); } catch { favorites.value = []; }
});
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
async function favorite() {
  try {
    if (favorites.value.includes(String(pet.value.id))) { await removeFavorite(pet.value.id); favorites.value = favorites.value.filter(id => id !== String(pet.value.id)); }
    else { await addFavorite(pet.value.id); favorites.value = [...favorites.value, String(pet.value.id)]; }
  } catch (error) { if (error.status === 401) router.push({ path: '/acceso.html', query: { redirect: route.fullPath } }); else loadError.value = error.message; }
}
async function askShelter() {
  try {
    const conversation = await api('/conversations', { method: 'POST', body: { petId: pet.value.id } });
    router.push({ path: '/mensajes.html', query: { conversation: conversation.id } });
  } catch (error) {
    if (error.status === 401) router.push({ path: '/acceso.html', query: { redirect: route.fullPath } });
    else loadError.value = error.message || 'No se pudo iniciar la conversación.';
  }
}
</script>
<template>
  <div><SiteHeader /><main><p v-if="loadError" class="pet-profile-error" role="alert">{{ loadError }}</p><section class="pet-profile" :data-pet-id="pet.id" :data-description="pet.description">
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
