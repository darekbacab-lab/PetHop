<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { getPetById, listFavorites, addFavorite, removeFavorite } from '../services/petApi.js';
import { api } from '../services/api.js';
import { listAffiliates } from '../services/affiliateApi.js';
const route = useRoute(); const router = useRouter();
const pet = ref({ id: '', name: 'Mascota', species: 'Mascota', description: '' });
const loadError = ref('');
const favorites = ref([]); const activeTrackIndex = ref(1); const transitionEnabled = ref(true); const isTransitioning = ref(false);
const affiliates = ref([]);
const pendingDirections = [];
const photos = computed(() => Array.isArray(pet.value.photos) ? pet.value.photos : []);
const generalInformation = computed(() => [
  ['Especie', pet.value.species], ['Raza', pet.value.breed], ['Sexo', pet.value.gender],
  ['Etapa de vida', pet.value.age], ['Tamaño', pet.value.size],
  ['Peso', pet.value.weight == null ? '' : `${pet.value.weight} kg`], ['Ubicación', pet.value.location]
]);
const healthInformation = computed(() => [
  ['Condición de salud', pet.value.healthCondition], ['Observaciones', pet.value.healthSummary],
  ['Esterilización', pet.value.sterilization], ['Vacunación', pet.value.vaccination], ['Desparasitación', pet.value.deworming]
].filter(([, value]) => value));
const personalityTraits = computed(() => (pet.value.personalityTraits || []).filter(value => !value.startsWith('Convive con ')));
const cohabitationInformation = computed(() => [
  ['Perros', 'Convive con perros'], ['Gatos', 'Convive con gatos'], ['Niños', 'Convive con niños']
].filter(([, trait]) => pet.value.personalityTraits?.includes(trait)).map(([label]) => label));
const adoptionRequirements = computed(() => [
  ['Tipo de hogar', pet.value.homeType], ['Condiciones necesarias', pet.value.conditions],
  ['Experiencia recomendada', pet.value.experience], ['Otros requisitos', pet.value.requirements]
].filter(([, value]) => value));
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
  try { affiliates.value = (await listAffiliates()).slice(0, 3); } catch { affiliates.value = []; }
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
  <div>
    <SiteHeader />
    <main>
      <p v-if="loadError" class="pet-profile-error" role="alert">{{ loadError }}</p>
      <section class="pet-profile" :data-pet-id="pet.id" :data-description="pet.description">
        <div class="pet-profile-container">
          <RouterLink to="/explorar.html" class="back-link"><Icon name="arrow-left" /> Volver a explorar</RouterLink>
          <div class="pet-profile-grid">
            <div class="pet-gallery-column">
              <div class="pet-profile-heading"><h1>{{ pet.name }}</h1><p v-if="pet.shelterName" class="pet-profile-publisher">Publicado por <strong>{{ pet.shelterName }}</strong></p></div>
              <div class="pet-gallery pet-profile-photo" :aria-label="`Fotografías de ${pet.name}`" tabindex="0" @keydown="handleGalleryKey">
                <div v-if="photos.length" class="gallery-viewport" role="region" aria-roledescription="carrusel" :aria-label="`Fotografías de ${pet.name}`" @touchstart.passive="startSwipe" @touchend.passive="endSwipe">
                  <div class="gallery-track" :class="{ 'gallery-track-no-transition': !transitionEnabled, 'gallery-track-moving': isTransitioning }" :style="{ transform: galleryTransform }" @transitionend="finishGalleryTransition">
                    <div v-for="(slide,index) in carouselSlides" :key="slide.key" class="gallery-card" :class="{ 'gallery-card-center':index===centerTrackIndex,'gallery-card-adjacent':Math.abs(index-centerTrackIndex)===1 }" role="group" aria-roledescription="diapositiva" :aria-label="`Fotografía ${slide.sourceIndex+1} de ${photos.length}`" :aria-hidden="index!==centerTrackIndex">
                      <ImageWithSkeleton frame-class="gallery-slide-frame" image-class="gallery-slide-image" :src="photoSrc(slide.photo)" :alt="`Fotografía ${slide.sourceIndex+1} de ${pet.name}`" />
                    </div>
                  </div>
                </div>
                <div v-else class="gallery-no-photo" role="img" :aria-label="`${pet.name} aún no tiene fotografías`"><Icon name="paw-print" /><span>El refugio aún no ha agregado fotografías.</span></div>
                <button v-if="photos.length > 1" class="gallery-arrow gallery-prev" type="button" aria-label="Ver imagen anterior" @click="nextPhoto(-1)"><Icon name="chevron-right" class="gallery-arrow-icon-left" :stroke-width="2.2" /></button>
                <button v-if="photos.length > 1" class="gallery-arrow gallery-next" type="button" aria-label="Ver imagen siguiente" @click="nextPhoto(1)"><Icon name="chevron-right" :stroke-width="2.2" /></button>
                <button class="gallery-favorite" :class="{ active:favorites.includes(pet.id) }" type="button" :aria-pressed="favorites.includes(pet.id)" :aria-label="favorites.includes(pet.id) ? `Quitar ${pet.name} de favoritos` : `Agregar ${pet.name} a favoritos`" @click="favorite"><Icon name="heart" :filled="favorites.includes(pet.id)" /></button>
              </div>
              <section class="pet-story pet-profile-story" :aria-label="`Descripción e historia de ${pet.name}`">
                <h2 class="pet-story-title">Descripción e historia</h2>
                <div class="pet-profile-copy"><strong>Descripción</strong><p>{{ pet.description || 'No se proporcionó una descripción.' }}</p></div>
                <div v-if="pet.story" class="pet-profile-copy"><strong>¿Cómo llegó a la adopción?</strong><p>{{ pet.story }}</p></div>
                <div v-if="pet.background" class="pet-profile-copy"><strong>Antecedentes relevantes</strong><p>{{ pet.background }}</p></div>
              </section>
            </div>
            <div class="pet-profile-info">
              <div class="pet-information">
                <h2>Datos de {{ pet.name }}</h2>
                <div class="information-grid pet-general-grid">
                  <div v-for="[label,value] in generalInformation" :key="label" class="information-item">
                    <strong>{{ label }}</strong><span>{{ value || 'No proporcionado' }}</span>
                  </div>
                </div>
              </div>
              <div class="pet-information">
                <h2>Salud y cuidados</h2>
                <div class="information-grid pet-health-grid"><div v-for="[label,value] in healthInformation" :key="label" class="information-item" :class="{ 'is-long': label==='Observaciones' && value.length>70 }"><strong>{{ label }}</strong><span>{{ value }}</span></div></div>
              </div>
              <div class="pet-information">
                <h2>Cómo es {{ pet.name }}</h2>
                <div class="pet-profile-copy"><strong>Características</strong><div v-if="personalityTraits.length" class="pet-trait-list"><span v-for="trait in personalityTraits" :key="trait">{{ trait }}</span></div><span v-else class="pet-profile-empty">Sin características registradas.</span></div>
                <div class="pet-profile-copy"><strong>Convive con:</strong><div v-if="cohabitationInformation.length" class="pet-trait-list pet-cohabitation-list"><span v-for="item in cohabitationInformation" :key="item">{{ item }}</span></div><span v-else class="pet-profile-empty">No especificado.</span></div>
              </div>
              <div v-if="pet.homeType || pet.conditions || pet.experience || pet.requirements" class="pet-information">
                <span class="profile-eyebrow">REQUISITOS DE ADOPCIÓN</span><h2>El hogar que necesita</h2>
                <div class="information-grid pet-requirements-grid"><div v-for="[label,value] in adoptionRequirements" :key="label" class="information-item" :class="{ 'is-long': ['Condiciones necesarias','Otros requisitos'].includes(label) && value.length>70 }"><strong>{{ label }}</strong><span>{{ value }}</span></div></div>
              </div>
              <div class="adoption-notice"><h2>Adopción responsable</h2><p>Conoce la información disponible de {{ pet.name }} antes de enviar tu solicitud. Puedes consultar con el refugio cualquier dato pendiente.</p></div>
              <section v-if="affiliates.length" class="pet-information pet-affiliate-section"><div class="pet-affiliate-heading"><div><span class="profile-eyebrow">BENEFICIOS PARA TU NUEVA HISTORIA</span><h2>Aliados que pueden acompañarte</h2></div><RouterLink class="pet-affiliate-all-link" :to="{ path: '/aliados.html', query: { pet: pet.id } }">Ver todos</RouterLink></div><div class="pet-affiliate-list"><article v-for="item in affiliates" :key="item.id"><Icon name="badge-check" /><div><strong>{{ item.name }}</strong><small>{{ item.discountText || item.category }}<template v-if="item.discountCode"> · Código {{ item.discountCode }}</template></small></div></article></div></section>
              <button type="button" class="message-shelter-button" @click="askShelter">Preguntar al refugio</button>
              <RouterLink class="pet-apply-button login-button" :to="{path:'/solicitud.html',query:{pet:pet.id}}">Solicitar adopción</RouterLink>
            </div>
          </div>
        </div>
      </section>
    </main>
    <SiteFooter />
  </div>
</template>
