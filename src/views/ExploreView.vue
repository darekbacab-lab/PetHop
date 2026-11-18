<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import PetCard from '../components/PetCard.vue';
import { addFavorite, listFavorites, listPets, removeFavorite } from '../services/petApi.js';

const route = useRoute();
const router = useRouter();
const pets = ref([]);
const favorites = ref([]);
const loading = ref(false);
const loadError = ref('');
const favoriteNotice = ref('');
const retrying = ref(false);
const query = ref(String(route.query.search || ''));
const favoritesOnly = ref(route.query.favorites === '1');
const filters = reactive({ species: String(route.query.species || ''), age: String(route.query.age || ''), gender: String(route.query.gender || ''), size: String(route.query.size || ''), location: String(route.query.location || ''), sort: String(route.query.sort || '') });
const appliedFilters = reactive({ ...filters });
const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es');
const locations = computed(() => [...new Set(pets.value.map(pet => pet.location).filter(Boolean))].sort());
const visiblePets = computed(() => {
  const search = normalize(query.value);
  const result = pets.value.filter(pet => {
    const text = normalize(`${pet.name} ${pet.species} ${pet.location} ${pet.description || ''}`);
    return (!search || text.includes(search)) && (!appliedFilters.age || normalize(pet.ageGroup).includes(normalize(appliedFilters.age))) && (!favoritesOnly.value || favorites.value.includes(String(pet.id).toLowerCase()));
  });
  if (filters.sort === 'name') result.sort((a, b) => a.name.localeCompare(b.name, 'es'));
  if (filters.sort === 'age') { const rank = { cachorro: 0, joven: 1, adulto: 2, mayor: 3 }; result.sort((a, b) => (rank[normalize(a.age)] ?? 99) - (rank[normalize(b.age)] ?? 99)); }
  return result;
});
function filterQuery() {
  const next = {};
  for (const key of ['species', 'age', 'gender', 'size', 'location', 'sort']) if (filters[key]) next[key] = filters[key];
  if (query.value) next.search = query.value;
  if (favoritesOnly.value) next.favorites = '1';
  return next;
}
function syncFromQuery() {
  let changed = false;
  for (const key of Object.keys(filters)) {
    const value = String(route.query[key] || '');
    if (filters[key] !== value) { filters[key] = value; changed = true; }
  }
  const search = String(route.query.search || '');
  if (query.value !== search) { query.value = search; changed = true; }
  const fav = route.query.favorites === '1';
  if (favoritesOnly.value !== fav) { favoritesOnly.value = fav; changed = true; }
  if (changed) Object.assign(appliedFilters, filters);
  return changed;
}
watch(() => route.query, () => { if (syncFromQuery()) loadPets(); });
async function loadPets() {
  loading.value = true; loadError.value = '';
  try {
    const result = await listPets({ ...filters, search: query.value, sort: filters.sort === 'age' ? '' : filters.sort });
    pets.value = result.content;
    if (filters.sort === 'age') { const rank = { cachorro: 0, joven: 1, adulto: 2, mayor: 3 }; pets.value.sort((a,b) => (rank[normalize(a.ageGroup)] ?? 99) - (rank[normalize(b.ageGroup)] ?? 99)); }
  } catch (error) { pets.value = []; loadError.value = error.message || 'No fue posible cargar las mascotas. Inténtalo de nuevo.'; }
  finally { loading.value = false; }
}
async function loadFavorites() { try { favorites.value = (await listFavorites()).map(pet => String(pet.id).toLowerCase()); } catch { favorites.value = []; } }
async function refresh() { retrying.value = true; try { await Promise.all([loadPets(), loadFavorites()]); } finally { retrying.value = false; } }
async function favorite(id) {
  favoriteNotice.value = '';
  try {
    const key = String(id).toLowerCase();
    if (favorites.value.includes(key)) { await removeFavorite(id); favorites.value = favorites.value.filter(item => item !== key); }
    else { await addFavorite(id); favorites.value = [...favorites.value, key]; }
  } catch (error) { if (error.status === 401) favoriteNotice.value = 'Inicia sesión para guardar mascotas en favoritos.'; else loadError.value = error.message; }
}
function applyFilters() { Object.assign(appliedFilters, filters); router.replace({ query: filterQuery() }); loadPets(); }
function clearFilters() { Object.keys(filters).forEach(key => { filters[key] = ''; appliedFilters[key] = ''; }); query.value = ''; favoritesOnly.value = false; router.replace({ path: route.path }); loadPets(); }
onMounted(refresh);
</script>

<template>
  <div class="catalog-page"><SiteHeader />
    <main id="main-content" tabindex="-1">
      <section class="catalog-header"><div class="catalog-header-container"><div class="catalog-header-copy"><h1>Encuentra una mascota <span>que conecte contigo.</span></h1></div></div></section>
      <section class="catalog-section"><div class="catalog-container">
        <form class="catalog-search" @submit.prevent="applyFilters">
          <div class="catalog-search-input"><span class="search-symbol" aria-hidden="true"><Icon name="search" /></span><label class="sr-only" for="catalogSearch">Buscar mascotas</label><input id="catalogSearch" v-model="query" name="search" type="search" placeholder="Buscar por nombre, especie o ubicación" autocomplete="off"></div>
          <button type="submit" class="search-button">Buscar</button>
        </form>
        <div class="catalog-layout">
          <section class="filter-panel catalog-filterbar" aria-label="Filtros de búsqueda">
            <div class="catalog-filter-controls">
              <div class="catalog-filter-group"><label for="speciesFilter">Especie</label><select id="speciesFilter" v-model="filters.species" @change="applyFilters"><option value="">Todas</option><option>Perro</option><option>Gato</option><option>Otro</option></select></div>
              <div class="catalog-filter-group"><label for="ageFilter">Edad</label><select id="ageFilter" v-model="filters.age" @change="applyFilters"><option value="">Todas</option><option value="Cachorro">Cachorro (hasta 1 año)</option><option value="Joven">Joven (1 a 3 años)</option><option value="Adulto">Adulto (3 a 8 años)</option><option value="Mayor">Mayor (más de 8 años)</option></select></div>
              <div class="catalog-filter-group"><label for="genderFilter">Sexo</label><select id="genderFilter" v-model="filters.gender" @change="applyFilters"><option value="">Todos</option><option>Macho</option><option>Hembra</option></select></div>
              <div class="catalog-filter-group"><label for="sizeFilter">Tamaño</label><select id="sizeFilter" v-model="filters.size" @change="applyFilters"><option value="">Todos</option><option>Pequeño</option><option>Mediano</option><option>Grande</option></select></div>
              <div class="catalog-filter-group"><label for="locationFilter">Ubicación</label><select id="locationFilter" v-model="filters.location" @change="applyFilters"><option value="">Todas</option><option v-for="location in locations" :key="location">{{ location }}</option></select></div>
              <div class="catalog-filter-group"><label for="sortPets">Ordenar</label><select v-model="filters.sort" id="sortPets" @change="applyFilters"><option value="">Más recientes</option><option value="name">Nombre</option><option value="age">Edad</option></select></div>
              <div class="filter-actions"><button class="clear-filters" type="button" @click="clearFilters">Limpiar filtros</button></div>
            </div>
          </section>
        <div class="catalog-results"><div class="results-header"><div><h2>{{ favoritesOnly ? 'Tus favoritos' : 'Mascotas disponibles' }}</h2><p class="results-count" aria-live="polite"><template v-if="loading"><strong>…</strong> Consultando disponibilidad</template><template v-else-if="loadError"><strong>—</strong> No se pudo consultar la disponibilidad</template><template v-else><strong>{{ visiblePets.length }}</strong>{{ favoritesOnly ? (visiblePets.length === 1 ? ' favorito' : ' favoritos') : (visiblePets.length === 1 ? ' mascota disponible' : ' mascotas disponibles') }}</template></p></div><button v-if="favoritesOnly" class="clear-filters" type="button" @click="favoritesOnly = false; router.replace({ query: filterQuery() })">Ver todas</button></div>
            <p v-if="favoriteNotice" class="catalog-favorite-notice" role="status">{{ favoriteNotice }}</p>
            <div v-if="loadError" class="catalog-empty" role="alert"><h2>No se pudieron consultar las mascotas.</h2><p>{{ loadError }}</p><button class="primary-button" type="button" :disabled="retrying" @click="refresh">{{ retrying ? 'Reintentando…' : 'Reintentar' }}</button></div>
            <div v-else-if="loading" class="catalog-loading" role="status" aria-label="Cargando mascotas"><div v-for="item in 3" :key="item" class="catalog-loading-card" aria-hidden="true"></div></div>
            <div v-else class="catalog-grid pets-grid"><PetCard v-for="pet in visiblePets" :key="pet.id" :pet="pet" :favorite="favorites.includes(String(pet.id).toLowerCase())" @favorite="favorite" /></div>
            <div v-if="!loading && !loadError && !visiblePets.length" class="catalog-empty"><h2>No encontramos mascotas con esos criterios.</h2><p>Prueba ajustar tu búsqueda o limpiar los filtros.</p><button class="primary-button" type="button" @click="clearFilters">Limpiar filtros</button></div>
          </div>
        </div>
      </div></section>
    </main><SiteFooter />
  </div>
</template>

<style scoped>
.catalog-header { margin-bottom:24px; padding-top:26px; }
.catalog-header h1 { max-width:970px; color:var(--hero-title-dark); font-size:clamp(2rem,4vw,3.3rem); font-weight:800; line-height:.99; letter-spacing:-.045em; }
.catalog-header h1 span { color:var(--hero-title-light); }
.catalog-layout { display:block; }
.catalog-filterbar { position:static; margin-bottom:18px; padding:12px 16px; border-color:var(--primary); background-color:var(--background); }
.catalog-filter-controls { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)) minmax(130px,1.2fr) minmax(168px,1.55fr) repeat(2,minmax(140px,1.2fr)); gap:10px; align-items:end; }
.catalog-filter-controls .catalog-filter-group { min-width:0; margin:0; display:block; width:auto; }
.catalog-filter-group label { margin-bottom:4px; font-size:.7rem; }
.catalog-filter-controls .catalog-filter-group select { min-width:0; min-height:40px; width:100%; padding:7px 26px 7px 10px; border-color:var(--primary); background-color:var(--white); font-size:.8rem; }
.catalog-filter-controls .filter-actions { display:contents; }
.catalog-filter-controls .filter-actions > button { width:100%; min-width:0; min-height:40px; padding:8px 10px; white-space:nowrap; }
.results-header > div:first-child h2 { color:var(--hero-title-dark); font-size:1.7rem; font-weight:800; }
.results-count { display:flex; align-items:baseline; gap:7px; }
.catalog-grid { grid-template-columns:repeat(3,minmax(0,1fr)); }
@media (max-width:1000px) {
  .catalog-filter-controls { grid-template-columns:repeat(3,minmax(0,1fr)); }
  .catalog-filter-controls .catalog-apply { grid-column:auto; }
  .catalog-filter-controls .filter-actions { display:grid; grid-column:1 / -1; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }
  .catalog-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
}
@media (max-width:700px) {
  .catalog-header { margin-bottom:8px; padding-top:16px; }
  .catalog-header h1 { margin:0; font-size:clamp(2.05rem,7.8vw,2.8rem); line-height:1; }
  .catalog-section { padding-top:12px; }
  .catalog-search { align-items:stretch; flex-direction:row; gap:7px; margin-bottom:10px; }
  .catalog-search-input input { min-height:44px; padding:10px 10px 10px 40px; font-size:.78rem; }
  .catalog-search .search-button { flex:0 0 25%; min-width:78px; min-height:44px; padding:0 10px; }
  .catalog-filterbar { margin-bottom:10px; padding:10px; }
  .catalog-filter-controls { grid-template-columns:repeat(2,minmax(0,1fr)); gap:6px 8px; }
  .catalog-filter-group label { margin-bottom:3px; font-size:.65rem; }
  .catalog-filter-controls .catalog-filter-group select { min-height:40px; padding:6px 24px 6px 8px; font-size:.74rem; }
  .catalog-filter-controls .filter-actions { grid-column:1 / -1; display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin-top:4px; }
  .catalog-filter-controls .filter-actions > button { width:100%; min-height:42px; padding:7px 5px; white-space:normal; }
  .catalog-results { margin-top:0; }
  .results-header { flex-direction:row; align-items:flex-end; flex-wrap:wrap; gap:6px 12px; margin-bottom:12px; }
  .results-header > div:first-child h2 { font-size:1.45rem; line-height:1.08; }
  .results-count { margin-top:2px; }
  .catalog-grid { grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }
}
@media (max-width:480px) {
  .results-header { align-items:flex-start; flex-direction:column; }
  .sort-control { width:100%; justify-content:space-between; }
  .catalog-grid { grid-template-columns:1fr; }
}
@media (max-width:360px) {
  .catalog-search { align-items:stretch; flex-direction:column; }
  .catalog-search .search-button { flex:0 0 auto; width:100%; }
}
</style>
